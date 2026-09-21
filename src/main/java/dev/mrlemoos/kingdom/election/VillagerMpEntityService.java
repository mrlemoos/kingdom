package dev.mrlemoos.kingdom.election;

import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.economy.villager.VillagerEconomyConfig;
import dev.mrlemoos.kingdom.economy.villager.VillagerStrike;
import dev.mrlemoos.kingdom.granary.GranaryConfig;
import dev.mrlemoos.kingdom.granary.HungerLedgerStore;
import dev.mrlemoos.kingdom.granary.HungerRamp;
import dev.mrlemoos.kingdom.hearth.ColdLedgerStore;
import dev.mrlemoos.kingdom.hearth.ColdRamp;
import dev.mrlemoos.kingdom.hearth.HearthConfig;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.model.election.MpSeatKind;
import dev.mrlemoos.kingdom.model.election.MpSeatLocation;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.model.parliament.ParliamentState;
import dev.mrlemoos.kingdom.parliament.SittingCalendar;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class VillagerMpEntityService {

    private static final int STARTUP_SYNC_MAX_ATTEMPTS = 6;
    private static final long STARTUP_SYNC_RETRY_TICKS = 200L;

    private final JavaPlugin plugin;
    private final KingdomService kingdomService;
    private final ProductiveVillagerScanner villagerScanner;
    private final KingdomTerritoryResolver territoryResolver;
    private final NamespacedKey mpKingdomTagKey;
    private final NamespacedKey mpOriginKey;
    private final NamespacedKey mpSpawnedKey;
    private final NamespacedKey treasuryLordTagKey;
    private final NamespacedKey townCrierTagKey;
    private final NamespacedKey clericTagKey;
    private final NamespacedKey villagerMagistrateTagKey;
    private EconomyService economyService;
    private VillagerEconomyConfig villagerEconomyConfig = VillagerEconomyConfig.defaults();
    private ColdLedgerStore coldLedger;
    private HearthConfig hearthConfig = HearthConfig.defaults();
    private HungerLedgerStore hungerLedger;
    private GranaryConfig granaryConfig = GranaryConfig.defaults();
    private LongSupplier realmDayClock = () -> 0L;

    public VillagerMpEntityService(
            JavaPlugin plugin,
            KingdomService kingdomService,
            ProductiveVillagerScanner villagerScanner,
            KingdomTerritoryResolver territoryResolver) {
        this.plugin = plugin;
        this.kingdomService = kingdomService;
        this.villagerScanner = villagerScanner;
        this.territoryResolver = territoryResolver;
        this.mpKingdomTagKey = new NamespacedKey(plugin, "kingdom_mp");
        this.mpOriginKey = new NamespacedKey(plugin, "kingdom_mp_origin");
        this.mpSpawnedKey = new NamespacedKey(plugin, "kingdom_mp_spawned");
        this.treasuryLordTagKey = new NamespacedKey(plugin, "treasury_lord");
        this.townCrierTagKey = new NamespacedKey(plugin, "town_crier");
        this.clericTagKey = new NamespacedKey(plugin, "church_cleric");
        // Written by PoliceCourtService; read here so no sweep relabels the magistrate.
        this.villagerMagistrateTagKey = new NamespacedKey(plugin, "police_judge");
    }

    /** Gives the sweep the cold ledger, so a frozen villager wears the strike nametag as an unpaid one does. */
    public void setColdStrikeSource(ColdLedgerStore coldLedger, HearthConfig hearthConfig) {
        this.coldLedger = coldLedger;
        this.hearthConfig = hearthConfig != null ? hearthConfig : HearthConfig.defaults();
    }

    public void setVillagerStrikeSource(EconomyService economyService, VillagerEconomyConfig villagerEconomyConfig) {
        this.economyService = economyService;
        this.villagerEconomyConfig =
                villagerEconomyConfig != null ? villagerEconomyConfig : VillagerEconomyConfig.defaults();
    }

    /** Puts the sweep on the realm calendar; without it every sync reads as the first sitting day. */
    public void setRealmDayClock(LongSupplier realmDayClock) {
        this.realmDayClock = realmDayClock != null ? realmDayClock : () -> 0L;
    }

    /** Syncs against the realm calendar and the kingdom's own session, not an assumed sitting day. */
    public void syncKingdom(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return;
        }
        syncKingdom(
                kingdomId, realmDayClock.getAsLong(), !kingdom.get().getParliamentState().isSessionOpen());
    }

    /**
     * Syncs villager MPs for the sitting calendar: ordinary villager MPs work professions on recess
     * (and while prorogued); Premier villager and Speaker stay at Parliament.
     */
    public void syncKingdom(String kingdomId, long realmDay, boolean prorogued) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> {
            releaseOrphanedMpVillagers(kingdomId);
            reconcileStrandedMpVillagers(kingdom);
            syncSpeaker(kingdom);
            if (SittingCalendar.villagerMpsAtProfession(realmDay, prorogued)) {
                releaseOrdinaryVillagerMpsForRecess(kingdom);
            } else {
                syncSeatsWithSubstitution(kingdom);
            }
            refreshTerritoryVillagerNametags(kingdom);
            reconcileKingdomWorldTerritoryVillagerDespawn(kingdom);
        });
    }

    public void syncAllKingdoms() {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            syncKingdom(kingdom.getId());
        }
    }

    public void scheduleStartupSync() {
        attemptStartupSync(0);
    }

    public boolean isVillagerSeatVacant(Kingdom kingdom, MpSeat seat) {
        if (seat.kind() != MpSeatKind.VILLAGER) {
            return false;
        }
        VillagerMpEntityLookup.EntityPresence presence = locateSeatedEntity(kingdom, seat);
        return VillagerMpEntityLookup.isSeatVacantForByElection(presence, seat.entityId().isPresent());
    }

    public void releaseKingdomVillagerMps(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> {
            for (MpSeat seat : kingdom.getElectionState().seatsView().values()) {
                if (seat.kind() == MpSeatKind.VILLAGER) {
                    releaseSeat(kingdomId, seat.index());
                }
            }
            releaseOrphanedMpVillagers(kingdomId);
            refreshTerritoryVillagerNametags(kingdom);
        });
    }

    /** @deprecated use {@link #releaseKingdomVillagerMps(String)} */
    @Deprecated
    public void despawnKingdomVillagerMps(String kingdomId) {
        releaseKingdomVillagerMps(kingdomId);
    }

    public void releaseSeat(String kingdomId, int seatIndex) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> kingdom.getElectionState()
                .seat(seatIndex)
                .ifPresent(seat -> {
                    if (seat.kind() != MpSeatKind.VILLAGER) {
                        return;
                    }
                    Optional<MpSeatLocation> bench = kingdom.getElectionState().seatLocation(seatIndex);
                    seat.entityId().ifPresent(entityId -> releaseEntity(seat, entityId, bench));
                    seat.setEntityId(null);
                    seat.setOriginLocation(null);
                    seat.setRecessed(false);
                }));
    }

    /** @deprecated use {@link #releaseSeat(String, int)} */
    @Deprecated
    public void despawnSeat(String kingdomId, int seatIndex) {
        releaseSeat(kingdomId, seatIndex);
    }

    public void refreshNametagAfterProfessionChange(Villager villager) {
        refreshNametagAfterProfessionChange(villager, villager.getProfession());
    }

    public void refreshNametagAfterProfessionChange(Villager villager, Villager.Profession profession) {
        if (!isEligibleForOrdinaryTerritoryNametag(villager)) {
            return;
        }
        applyOrdinaryTerritoryNametag(
                villager,
                VillagerMpProfessionMatcher.professionName(profession),
                isOnStrike(villager),
                isStarving(villager));
    }

    public void reconcileTerritoryVillagerNametag(Villager villager) {
        if (!isEligibleForOrdinaryTerritoryNametag(villager)) {
            return;
        }
        String professionName = VillagerMpProfessionMatcher.professionName(villager);
        boolean onStrike = isOnStrike(villager);
        boolean starving = isStarving(villager);
        if (!VillagerTerritoryNametagReconciliation.shouldReconcileNametag(
                villager.getCustomName(), professionName, true, onStrike, starving)) {
            return;
        }
        applyOrdinaryTerritoryNametag(villager, professionName, onStrike, starving);
    }

    public boolean isTreasuryLordVillager(Villager villager) {
        return isTreasuryLord(villager);
    }

    public boolean isKingdomTaggedMpVillager(Villager villager) {
        return isMpVillager(villager);
    }

    public boolean isSeatedMpVillager(Villager villager) {
        return isSeatedMpVillager(villager.getUniqueId());
    }

    public void reconcileTerritoryVillagerDespawn(Villager villager) {
        if (isClericVillager(villager)) {
            // The cleric keeps its own persistence; the territory sweep has no business with it.
            return;
        }
        boolean treasuryLord = isTreasuryLord(villager);
        boolean seatedMp = isSeatedMpVillager(villager.getUniqueId());
        boolean kingdomTaggedMp = isMpVillager(villager);
        boolean townCrier = isTownCrier(villager);
        if (!TerritoryVillagerDespawnPolicy.shouldManage(treasuryLord, seatedMp, kingdomTaggedMp, townCrier)) {
            return;
        }

        boolean inTerritory = isInAnyKingdomTerritory(villager);
        if (TerritoryVillagerDespawnPolicy.shouldApplyProtection(inTerritory, true)) {
            applyTerritoryDespawnProtection(villager);
            assignHomeBedIfMissing(villager);
            return;
        }
        if (TerritoryVillagerDespawnPolicy.shouldRevertToVanilla(inTerritory, true)) {
            revertTerritoryDespawnProtection(villager);
        }
    }

    public void reconcileTerritoryVillagersInChunk(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Villager villager) {
                reconcileTerritoryVillagerDespawn(villager);
                reconcileTerritoryVillagerNametag(villager);
            }
        }
    }

    public void reconcileAllTerritoryVillagerDespawn() {
        reconcileTerritoryVillagersInWorlds(distinctKingdomWorldNames());
    }

    public void reconcileAllTerritoryVillagerNametags() {
        reconcileTerritoryVillagerNametagsInWorlds(distinctKingdomWorldNames());
    }

    public void refreshTerritoryVillagerNametags(Kingdom kingdom) {
        if (!kingdom.hasWorldGuardRegions()) {
            return;
        }
        String worldName = kingdomService.resolveWorldName(kingdom);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        Set<UUID> seatedMpIds = seatedVillagerEntityIds(kingdom);
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            // One exclusion rule for every sweep: plugin NPCs own their nametag.
            if (VillagerNametagRefreshEligibility.isPluginNpc(
                    isTreasuryLord(villager),
                    isMpVillager(villager),
                    seatedMpIds.contains(villager.getUniqueId()),
                    isTownCrier(villager),
                    isVillagerMagistrate(villager),
                    isClericVillager(villager))) {
                continue;
            }
            if (!isInKingdomTerritory(villager, kingdom)) {
                continue;
            }
            applyStandardNametag(villager);
        }
    }

    /** Seats or dismisses this kingdom's villager Speaker without sweeping every territory villager. */
    public void syncSpeaker(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(this::syncSpeaker);
    }

    /** True while this villager presides over the Commons as its Speaker. */
    public boolean isVillagerSpeaker(Villager villager) {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            if (kingdom.getParliamentState()
                    .speakerVillagerEntityId()
                    .filter(villager.getUniqueId()::equals)
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Seats a villager Speaker whenever no player holds the Speakership and Parliament is in session,
     * and dismisses it otherwise. Spawned fresh: the Chair claims no territory villager.
     */
    private void syncSpeaker(Kingdom kingdom) {
        ParliamentState state = kingdom.getParliamentState();
        boolean wanted = state.isSessionOpen()
                && !kingdomService.hasPlayerWithRank(kingdom.getId(), NobleRank.SPEAKER);

        if (!wanted) {
            state.speakerVillagerEntityId().flatMap(this::findEntity).ifPresent(Entity::remove);
            state.clearSpeakerVillager();
            return;
        }

        Optional<Location> chair = speakerChairSite(kingdom).flatMap(VillagerMpEntityService::toBukkitLocation);
        if (chair.isEmpty()) {
            return;
        }
        Location location = chair.get();
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        if (state.speakerVillagerEntityId().isPresent()) {
            location.getChunk();
            Optional<Entity> seated = state.speakerVillagerEntityId().flatMap(this::findEntity);
            if (seated.isPresent()) {
                if (seated.get() instanceof Villager villager) {
                    configureSpeakerBehaviour(villager, kingdom.getId());
                }
                return;
            }
        }

        Villager speaker = world.spawn(location, Villager.class, spawned -> {
            spawned.setProfession(Villager.Profession.NONE);
            configureSpeakerBehaviour(spawned, kingdom.getId());
        });
        state.setSpeakerVillagerEntityId(speaker.getUniqueId());
    }

    private void configureSpeakerBehaviour(Villager villager, String kingdomId) {
        ensureSeatedStance(villager);
        villager.setAI(false);
        villager.setInvulnerable(VillagerMpCombatPolicy.shouldLockFromCombat(true));
        villager.setPersistent(VillagerMpDespawnPolicy.persistentWhileSeated());
        villager.setRemoveWhenFarAway(VillagerMpDespawnPolicy.removeWhenFarAwayWhileSeated());
        villager.setSilent(true);
        villager.setCustomNameVisible(true);
        villager.setCustomName(NoblePrefixDisplay.speakerVillagerNametag());
        villager.getPersistentDataContainer().set(mpKingdomTagKey, PersistentDataType.STRING, kingdomId);
    }

    private static Optional<ChamberSite> speakerChairSite(Kingdom kingdom) {
        Optional<ChamberSite> chair = kingdom.getParliamentSites().speakerChair();
        return chair.isPresent() ? chair : kingdom.getParliamentSites().commons();
    }

    private static Optional<Location> toBukkitLocation(ChamberSite site) {
        World world = Bukkit.getWorld(site.worldName());
        return world == null ? Optional.empty() : Optional.of(new Location(world, site.x(), site.y(), site.z()));
    }

    private void syncSeatsWithSubstitution(Kingdom kingdom) {
        Set<UUID> reserved = seatedVillagerEntityIds(kingdom);
        for (MpSeat seat : kingdom.getElectionState().seatsView().values()) {
            if (seat.kind() != MpSeatKind.VILLAGER || seat.profession().isEmpty()) {
                continue;
            }
            Optional<MpSeatLocation> location = kingdom.getElectionState().seatLocation(seat.index());
            if (location.isEmpty()) {
                continue;
            }
            VillagerMpEntityLookup.EntityPresence presence = locateSeatedEntity(kingdom, seat);
            if (presence == VillagerMpEntityLookup.EntityPresence.PRESENT) {
                seat.entityId().flatMap(this::findEntity).ifPresent(entity -> {
                    if (entity instanceof Villager villager) {
                        Optional<MpSeatLocation> commons = location;
                        if (seat.isRecessed() && commons.isPresent()) {
                            villager.teleport(toBukkitLocation(commons.get()));
                        }
                        seat.setRecessed(false);
                        configureMpBehaviour(villager, seat, kingdom.getId());
                    }
                });
                continue;
            }
            if (presence == VillagerMpEntityLookup.EntityPresence.UNKNOWN) {
                // Merely absent / unloaded — leave the seat empty for this sitting day.
                continue;
            }
            if (presence == VillagerMpEntityLookup.EntityPresence.ABSENT_NO_ID) {
                seatVillagerEntity(kingdom, seat, location.get(), reserved);
                seat.entityId().ifPresent(reserved::add);
                continue;
            }
            if (presence != VillagerMpEntityLookup.EntityPresence.ABSENT_CONFIRMED) {
                continue;
            }
            String preferred = seat.profession().orElse("none");
            Optional<Villager> substitute = findSubstitutionCandidate(kingdom, preferred, reserved);
            if (substitute.isPresent()) {
                claimExistingVillager(kingdom, seat, location.get(), substitute.get());
                reserved.add(substitute.get().getUniqueId());
                String label = ProfessionConstituencyResolver.displayLabel(preferred);
                Bukkit.broadcastMessage(dev.mrlemoos.kingdom.helpers.ColourEncoder.c(
                        "&6The member for " + label + " has been replaced."));
                continue;
            }
            // No eligible substitute — empty seat for the sitting day.
            seat.setEntityId(null);
        }
    }

    private Optional<Villager> findSubstitutionCandidate(
            Kingdom kingdom, String preferredProfession, Set<UUID> reservedEntityIds) {
        List<MpSubstitutionSelector.Candidate> pool = new ArrayList<>();
        List<Villager> byId = new ArrayList<>();
        String worldName = kingdomService.resolveWorldName(kingdom);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return Optional.empty();
        }
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (reservedEntityIds.contains(villager.getUniqueId())) {
                continue;
            }
            if (isTreasuryLord(villager) || isMpVillager(villager) || isTownCrier(villager)) {
                continue;
            }
            String profession = VillagerMpProfessionMatcher.professionName(villager);
            pool.add(new MpSubstitutionSelector.Candidate(villager.getUniqueId().toString(), profession));
            byId.add(villager);
        }
        Optional<MpSubstitutionSelector.Candidate> chosen =
                MpSubstitutionSelector.select(preferredProfession, pool);
        if (chosen.isEmpty()) {
            return Optional.empty();
        }
        String id = chosen.get().id();
        for (Villager villager : byId) {
            if (villager.getUniqueId().toString().equals(id)) {
                return Optional.of(villager);
            }
        }
        return Optional.empty();
    }

    /**
     * Sends ordinary villager MPs back to their professions for recess: teleport to origin, restore
     * AI and vanilla despawn, keep the [MP] nametag and kingdom MP tag for territory protection.
     * Seat keeps entityId for re-claim by UUID; {@link MpSeat#isRecessed()} marks them not sitting.
     */
    private void releaseOrdinaryVillagerMpsForRecess(Kingdom kingdom) {
        for (MpSeat seat : kingdom.getElectionState().seatsView().values()) {
            if (seat.kind() != MpSeatKind.VILLAGER || seat.profession().isEmpty()) {
                continue;
            }
            if (kingdom.getElectionState().isPremierVillagerSeat(seat.index())) {
                Optional<MpSeatLocation> location = kingdom.getElectionState().seatLocation(seat.index());
                if (location.isPresent()) {
                    VillagerMpEntityLookup.EntityPresence presence = locateSeatedEntity(kingdom, seat);
                    if (presence == VillagerMpEntityLookup.EntityPresence.PRESENT) {
                        seat.entityId().flatMap(this::findEntity).ifPresent(entity -> {
                            if (entity instanceof Villager villager) {
                                seat.setRecessed(false);
                                configureMpBehaviour(villager, seat, kingdom.getId());
                            }
                        });
                    } else if (VillagerMpEntityLookup.shouldReplaceSeatedEntity(presence)) {
                        Set<UUID> reserved = seatedVillagerEntityIds(kingdom);
                        seatVillagerEntity(kingdom, seat, location.get(), reserved);
                    }
                }
                continue;
            }
            // Where the member stands decides, not the flag: a State Opening summons carries a
            // recessed member back to the bench, and the flag alone would leave it sitting there.
            if (seat.isRecessed() && !isOnBench(kingdom, seat)) {
                seat.entityId().flatMap(this::findEntity).ifPresent(entity -> {
                    if (entity instanceof Villager villager) {
                        refreshSeatNametag(villager, seat, kingdom.getId());
                    }
                });
                continue;
            }
            seat.entityId().ifPresent(entityId -> releaseEntityForRecess(kingdom, seat, entityId));
        }
    }

    /** True while the seat's member is loaded and standing at its bench in the chamber. */
    private boolean isOnBench(Kingdom kingdom, MpSeat seat) {
        Optional<MpSeatLocation> bench = kingdom.getElectionState().seatLocation(seat.index());
        Optional<UUID> entityId = seat.entityId();
        if (bench.isEmpty() || entityId.isEmpty()) {
            return false;
        }
        Optional<Entity> entity = findEntity(entityId.get());
        return entity.isPresent()
                && VillagerMpOriginPolicy.isAtChamber(toSeatLocation(entity.get().getLocation()), bench.get());
    }

    private void releaseEntityForRecess(Kingdom kingdom, MpSeat seat, UUID entityId) {
        String kingdomId = kingdom.getId();
        findEntity(entityId).ifPresent(entity -> {
            if (!(entity instanceof Villager villager)) {
                return;
            }
            if (sendHomeOrDismiss(villager, seat.originLocation(), kingdom.getElectionState()
                    .seatLocation(seat.index()))) {
                seat.setEntityId(null);
                seat.setOriginLocation(null);
                seat.setRecessed(false);
                return;
            }
            villager.setAI(true);
            villager.setInvulnerable(false);
            villager.setPersistent(VillagerMpDespawnPolicy.persistentAfterRelease());
            villager.setRemoveWhenFarAway(VillagerMpDespawnPolicy.removeWhenFarAwayAfterRelease());
            villager.setSilent(false);
            refreshSeatNametag(villager, seat, kingdomId);
            villager.getPersistentDataContainer().set(mpKingdomTagKey, PersistentDataType.STRING, kingdomId);
            seat.setRecessed(true);
            if (isInAnyKingdomTerritory(villager)) {
                applyTerritoryDespawnProtection(villager);
            }
        });
    }

    private void seatVillagerEntity(
            Kingdom kingdom, MpSeat seat, MpSeatLocation seatLocation, Set<UUID> reservedEntityIds) {
        Optional<Villager> candidate = villagerScanner.findCandidate(
                kingdom,
                seat.profession().orElse("none"),
                reservedEntityIds,
                villager -> isTreasuryLord(villager) || isMpVillager(villager) || isClericVillager(villager));
        if (candidate.isPresent()) {
            claimExistingVillager(kingdom, seat, seatLocation, candidate.get());
            return;
        }
        spawnFallbackVillager(kingdom.getId(), seat, seatLocation);
    }

    private void claimExistingVillager(Kingdom kingdom, MpSeat seat, MpSeatLocation seatLocation, Villager villager) {
        Optional<MpSeatLocation> origin = Optional.of(toSeatLocation(villager.getLocation()));
        // A villager already loitering in the chamber must not take the bench for its home, or the
        // House would send it back to Parliament every recess. Its bed serves instead.
        if (!VillagerMpOriginPolicy.shouldRecordOrigin(origin.get(), Optional.of(seatLocation))) {
            origin = homeBedOf(villager);
        }
        seat.setOriginLocation(origin.orElse(null));
        if (origin.isPresent()) {
            storeOriginOnEntity(villager, origin.get());
        } else {
            clearOriginOnEntity(villager);
        }
        Location destination = toBukkitLocation(seatLocation);
        villager.teleport(destination);
        configureMpBehaviour(villager, seat, kingdom.getId());
        seat.setEntityId(villager.getUniqueId());
        seat.setRecessed(false);
    }

    private void spawnFallbackVillager(String kingdomId, MpSeat seat, MpSeatLocation seatLocation) {
        World world = plugin.getServer().getWorld(seatLocation.worldName());
        if (world == null) {
            return;
        }
        Location location = toBukkitLocation(seatLocation);
        Villager villager = world.spawn(location, Villager.class, spawned -> {
            applyProfession(spawned, seat.profession().orElse("none"));
            configureMpBehaviour(spawned, seat, kingdomId);
            // Marked as the House's own: it has no home in the realm, so release dismisses it
            // rather than leaving it to loiter in the chamber for ever.
            spawned.getPersistentDataContainer().set(mpSpawnedKey, PersistentDataType.BYTE, (byte) 1);
        });
        seat.setEntityId(villager.getUniqueId());
    }

    private void configureMpBehaviour(Villager villager, MpSeat seat, String kingdomId) {
        ensureSeatedStance(villager);
        villager.setAI(false);
        villager.setInvulnerable(VillagerMpCombatPolicy.shouldLockFromCombat(true));
        villager.setPersistent(VillagerMpDespawnPolicy.persistentWhileSeated());
        villager.setRemoveWhenFarAway(VillagerMpDespawnPolicy.removeWhenFarAwayWhileSeated());
        villager.setSilent(true);
        refreshSeatNametag(villager, seat, kingdomId);
        if (kingdomId != null) {
            villager.getPersistentDataContainer().set(mpKingdomTagKey, PersistentDataType.STRING, kingdomId);
        }
    }

    private void refreshSeatNametag(Villager villager, MpSeat seat, String kingdomId) {
        String label = ProfessionConstituencyResolver.displayLabel(seat.profession().orElse("none"));
        boolean premier = kingdomService.getKingdom(kingdomId)
                .map(k -> k.getElectionState().isPremierVillagerSeat(seat.index()))
                .orElse(false);
        villager.setCustomNameVisible(true);
        villager.setCustomName(premier
                ? NoblePrefixDisplay.premierVillagerNametag(label)
                : NoblePrefixDisplay.mpVillagerNametag(label));
    }

    private void releaseEntity(MpSeat seat, UUID entityId, Optional<MpSeatLocation> bench) {
        findEntity(entityId).ifPresent(entity -> {
            if (!(entity instanceof Villager villager)) {
                entity.remove();
                return;
            }
            if (sendHomeOrDismiss(villager, seat.originLocation(), bench)) {
                return;
            }
            restoreDefaultBehaviour(villager);
        });
    }

    /**
     * Sends a released villager back to its home, never to the bench it is being sent from. One the
     * House itself spawned, with no home in the realm, is dismissed rather than left to loiter in
     * the chamber. Returns true when the villager was dismissed.
     */
    private boolean sendHomeOrDismiss(
            Villager villager, Optional<MpSeatLocation> seatOrigin, Optional<MpSeatLocation> bench) {
        Optional<MpSeatLocation> home = VillagerMpOriginPolicy.releaseDestination(
                seatOrigin, readOriginFromEntity(villager), homeBedOf(villager), bench);
        if (VillagerMpOriginPolicy.shouldDismiss(isPluginSpawned(villager), home.isPresent())) {
            villager.remove();
            return true;
        }
        if (home.isPresent()) {
            villager.teleport(toBukkitLocation(home.get()));
        }
        return false;
    }

    private Optional<MpSeatLocation> homeBedOf(Villager villager) {
        Location bed = villager.getMemory(MemoryKey.HOME);
        return bed == null || bed.getWorld() == null ? Optional.empty() : Optional.of(toSeatLocation(bed));
    }

    private boolean isPluginSpawned(Villager villager) {
        Byte tag = villager.getPersistentDataContainer().get(mpSpawnedKey, PersistentDataType.BYTE);
        return tag != null && tag == 1;
    }

    private void releaseOrphanedMpVillagers(String kingdomId) {
        Set<UUID> seatedIds = kingdomService.getKingdom(kingdomId)
                .map(this::seatedVillagerEntityIds)
                .orElseGet(Set::of);
        for (Villager villager : allVillagers()) {
            String taggedKingdom = villager.getPersistentDataContainer().get(mpKingdomTagKey,
                    PersistentDataType.STRING);
            if (!kingdomId.equals(taggedKingdom)) {
                continue;
            }
            if (seatedIds.contains(villager.getUniqueId())) {
                continue;
            }
            if (sendHomeOrDismiss(villager, Optional.empty(), Optional.empty())) {
                continue;
            }
            restoreDefaultBehaviour(villager);
        }
    }

    private void reconcileStrandedMpVillagers(Kingdom kingdom) {
        Set<UUID> seatedIds = seatedVillagerEntityIds(kingdom);
        String worldName = kingdomService.resolveWorldName(kingdom);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (isTreasuryLord(villager)) {
                continue;
            }
            boolean seated = seatedIds.contains(villager.getUniqueId());
            boolean treasuryLord = isTreasuryLord(villager);
            boolean kingdomTaggedMp = isMpVillager(villager);
            if (!VillagerMpCombatPolicy.needsCombatRestore(
                    kingdomTaggedMp, seated, villager.isInvulnerable(), villager.getCustomName())
                    && !VillagerMpDespawnPolicy.needsDespawnRestore(
                            kingdomTaggedMp,
                            seated,
                            treasuryLord,
                            villager.isPersistent(),
                            villager.getRemoveWhenFarAway(),
                            villager.getCustomName())) {
                continue;
            }
            if (sendHomeOrDismiss(villager, Optional.empty(), Optional.empty())) {
                continue;
            }
            restoreDefaultBehaviour(villager);
        }
    }

    private static Iterable<Villager> allVillagers() {
        return Bukkit.getWorlds().stream()
                .flatMap(world -> world.getEntitiesByClass(Villager.class).stream())
                .toList();
    }

    private void reconcileKingdomWorldTerritoryVillagerDespawn(Kingdom kingdom) {
        reconcileTerritoryVillagersInWorlds(Set.of(kingdomService.resolveWorldName(kingdom)));
    }

    private void reconcileTerritoryVillagersInWorlds(Set<String> worldNames) {
        for (String worldName : worldNames) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                reconcileTerritoryVillagerDespawn(villager);
            }
        }
    }

    private void reconcileTerritoryVillagerNametagsInWorlds(Set<String> worldNames) {
        for (String worldName : worldNames) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                reconcileTerritoryVillagerNametag(villager);
            }
        }
    }

    private Set<String> distinctKingdomWorldNames() {
        Set<String> worldNames = new HashSet<>();
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            worldNames.add(kingdomService.resolveWorldName(kingdom));
        }
        return worldNames;
    }

    private void assignHomeBedIfMissing(Villager villager) {
        if (!VillagerHomeBedPolicy.shouldAssign(true, true, villager.getMemory(MemoryKey.HOME) != null)) {
            return;
        }
        Location origin = villager.getLocation();
        World world = origin.getWorld();
        if (world == null) {
            return;
        }
        VillagerHomeBedPolicy.nearestBed(origin, loadedBedsNear(world, origin))
                .ifPresent(bed -> villager.setMemory(MemoryKey.HOME, bed));
    }

    // ponytail: scans loaded chunks only and ignores whether another villager already claimed the bed; vanilla POI
    // stays the authority and will drop the memory if the bed is taken or unreachable.
    private List<Location> loadedBedsNear(World world, Location origin) {
        List<Location> beds = new ArrayList<>();
        int chunkRadius = VillagerHomeBedPolicy.searchChunkRadius();
        int centreChunkX = origin.getBlockX() >> 4;
        int centreChunkZ = origin.getBlockZ() >> 4;
        for (int chunkX = centreChunkX - chunkRadius; chunkX <= centreChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centreChunkZ - chunkRadius; chunkZ <= centreChunkZ + chunkRadius; chunkZ++) {
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    continue;
                }
                for (BlockState state : world.getChunkAt(chunkX, chunkZ).getTileEntities()) {
                    if (state.getBlockData() instanceof Bed bed && bed.getPart() == Bed.Part.HEAD) {
                        beds.add(state.getLocation());
                    }
                }
            }
        }
        return beds;
    }

    private void applyTerritoryDespawnProtection(Villager villager) {
        villager.setPersistent(TerritoryVillagerDespawnPolicy.persistentInTerritory());
        villager.setRemoveWhenFarAway(TerritoryVillagerDespawnPolicy.removeWhenFarAwayInTerritory());
    }

    private void revertTerritoryDespawnProtection(Villager villager) {
        villager.setPersistent(TerritoryVillagerDespawnPolicy.persistentOutsideTerritory());
        villager.setRemoveWhenFarAway(TerritoryVillagerDespawnPolicy.removeWhenFarAwayOutsideTerritory());
    }

    private boolean isInAnyKingdomTerritory(Villager villager) {
        Location location = villager.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        return territoryResolver
                .owningKingdomId(
                        world.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ())
                .isPresent();
    }

    private boolean isInKingdomTerritory(Villager villager, Kingdom kingdom) {
        if (!kingdom.hasWorldGuardRegions()) {
            return false;
        }
        Location location = villager.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        String worldName = kingdomService.resolveWorldName(kingdom);
        if (!worldName.equals(world.getName())) {
            return false;
        }
        return territoryResolver
                .owningKingdomId(
                        world.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ())
                .filter(kingdom.getId()::equals)
                .isPresent();
    }

    private boolean isEligibleForOrdinaryTerritoryNametag(Villager villager) {
        return VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                isTreasuryLord(villager),
                isMpVillager(villager),
                isSeatedMpVillager(villager.getUniqueId()),
                isTownCrier(villager),
                isVillagerMagistrate(villager),
                isClericVillager(villager),
                isInAnyKingdomTerritory(villager));
    }

    private void ensureSeatedStance(Villager villager) {
        if (!VillagerMpStancePolicy.needsStandingReset(villager.isSleeping(), villager.getPose())) {
            return;
        }
        if (villager.isSleeping()) {
            villager.wakeup();
        }
        if (villager.getPose() == Pose.SLEEPING) {
            villager.setPose(Pose.STANDING);
        }
    }

    private void restoreDefaultBehaviour(Villager villager) {
        villager.setAI(true);
        villager.setInvulnerable(false);
        villager.setPersistent(VillagerMpDespawnPolicy.persistentAfterRelease());
        villager.setRemoveWhenFarAway(VillagerMpDespawnPolicy.removeWhenFarAwayAfterRelease());
        villager.setSilent(false);
        villager.getPersistentDataContainer().remove(mpKingdomTagKey);
        clearOriginOnEntity(villager);
        applyStandardNametag(villager);
    }

    private static void applyStandardNametag(Villager villager) {
        applyStandardNametag(villager, VillagerMpProfessionMatcher.professionName(villager));
    }

    private static void applyStandardNametag(Villager villager, String professionName) {
        applyOrdinaryTerritoryNametag(villager, professionName, false, false);
    }

    private static void applyOrdinaryTerritoryNametag(
            Villager villager, String professionName, boolean onStrike, boolean starving) {
        String label = VillagerTerritoryNametagReconciliation.labelFor(professionName, onStrike, starving);
        villager.setCustomNameVisible(true);
        villager.setCustomName(label);
    }

    private boolean isOnStrike(Villager villager) {
        if (economyService == null || villager.getLocation().getWorld() == null) {
            return false;
        }
        Optional<String> kingdomId = territoryResolver.owningKingdomId(
                villager.getLocation().getWorld().getName(),
                villager.getLocation().getBlockX(),
                villager.getLocation().getBlockY(),
                villager.getLocation().getBlockZ());
        if (kingdomId.isEmpty()) {
            return false;
        }
        if (VillagerStrike.isOnStrike(
                economyService.getVillagerWalletFrozenSince(kingdomId.get(), villager.getUniqueId()),
                villager.getLocation().getWorld().getFullTime() / 24000L,
                villagerEconomyConfig.frozenWalletStrikeMcDays(),
                villagerEconomyConfig.frozenWalletEscheatMcDays())) {
            return true;
        }
        ColdLedgerStore ledger = this.coldLedger;
        if (ledger != null
                && ColdRamp.strikes(ledger.coldDays(kingdomId.get(), villager.getUniqueId()), hearthConfig)) {
            return true;
        }
        HungerLedgerStore hunger = this.hungerLedger;
        return hunger != null
                && HungerRamp.strikes(hunger.hungryDays(kingdomId.get(), villager.getUniqueId()), granaryConfig);
    }

    /** Gives the service the hunger ledger, so a villager left unfed strikes and is called starving. */
    public void setHungerStrikeSource(HungerLedgerStore hungerLedger, GranaryConfig granaryConfig) {
        this.hungerLedger = hungerLedger;
        this.granaryConfig = granaryConfig != null ? granaryConfig : GranaryConfig.defaults();
    }

    /** Whether the villager has gone hungry long enough to down tools, and so be called starving. */
    private boolean isStarving(Villager villager) {
        HungerLedgerStore hunger = this.hungerLedger;
        if (hunger == null || villager.getLocation().getWorld() == null) {
            return false;
        }
        Optional<String> kingdomId = territoryResolver.owningKingdomId(
                villager.getLocation().getWorld().getName(),
                villager.getLocation().getBlockX(),
                villager.getLocation().getBlockY(),
                villager.getLocation().getBlockZ());
        return kingdomId.isPresent()
                && HungerRamp.strikes(hunger.hungryDays(kingdomId.get(), villager.getUniqueId()), granaryConfig);
    }

    private static void refreshMpNametag(Villager villager, String profession) {
        String label = ProfessionConstituencyResolver.displayLabel(profession);
        villager.setCustomNameVisible(true);
        villager.setCustomName(NoblePrefixDisplay.mpVillagerNametag(label));
    }

    private void storeOriginOnEntity(Villager villager, MpSeatLocation origin) {
        villager.getPersistentDataContainer().set(mpOriginKey, PersistentDataType.STRING, encodeOrigin(origin));
    }

    private void clearOriginOnEntity(Villager villager) {
        villager.getPersistentDataContainer().remove(mpOriginKey);
    }

    private Optional<MpSeatLocation> readOriginFromEntity(Villager villager) {
        String encoded = villager.getPersistentDataContainer().get(mpOriginKey, PersistentDataType.STRING);
        if (encoded == null || encoded.isBlank()) {
            return Optional.empty();
        }
        return decodeOrigin(encoded);
    }

    private static String encodeOrigin(MpSeatLocation origin) {
        return origin.worldName()
                + "|"
                + origin.x()
                + "|"
                + origin.y()
                + "|"
                + origin.z()
                + "|"
                + origin.yaw()
                + "|"
                + origin.pitch();
    }

    private static Optional<MpSeatLocation> decodeOrigin(String encoded) {
        String[] parts = encoded.split("\\|", -1);
        if (parts.length != 6) {
            return Optional.empty();
        }
        try {
            return Optional.of(new MpSeatLocation(
                    parts[0],
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Float.parseFloat(parts[4]),
                    Float.parseFloat(parts[5])));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private static void applyProfession(Villager villager, String profession) {
        try {
            org.bukkit.entity.Villager.Profession bukkitProfession = org.bukkit.entity.Villager.Profession
                    .valueOf(profession.toUpperCase(Locale.ROOT));
            villager.setProfession(bukkitProfession);
        } catch (IllegalArgumentException ignored) {
            villager.setProfession(org.bukkit.entity.Villager.Profession.NONE);
        }
    }

    private boolean isMpVillager(Villager villager) {
        return villager.getPersistentDataContainer().has(mpKingdomTagKey, PersistentDataType.STRING);
    }

    private boolean isTreasuryLord(Villager villager) {
        Byte tag = villager.getPersistentDataContainer().get(treasuryLordTagKey, PersistentDataType.BYTE);
        return tag != null && tag == 1;
    }

    private boolean isVillagerMagistrate(Villager villager) {
        Byte tag = villager.getPersistentDataContainer().get(villagerMagistrateTagKey, PersistentDataType.BYTE);
        return tag != null && tag == 1;
    }

    /** True when this villager holds a plugin role and so owns its own nametag. */
    public boolean isPluginNpcVillager(Villager villager) {
        return VillagerNametagRefreshEligibility.isPluginNpc(
                isTreasuryLord(villager),
                isMpVillager(villager),
                isSeatedMpVillager(villager.getUniqueId()),
                isTownCrier(villager),
                isVillagerMagistrate(villager),
                isClericVillager(villager));
    }

    /** The cleric stands at the church and is no part of the villager economy or the Commons. */
    public boolean isClericVillager(Villager villager) {
        Byte tag = villager.getPersistentDataContainer().get(clericTagKey, PersistentDataType.BYTE);
        return tag != null && tag == 1;
    }

    private boolean isTownCrier(Villager villager) {
        Byte tag = villager.getPersistentDataContainer().get(townCrierTagKey, PersistentDataType.BYTE);
        return tag != null && tag == 1;
    }

    public boolean isTownCrierVillager(Villager villager) {
        return isTownCrier(villager);
    }

    private boolean isSeatedMpVillager(UUID entityId) {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            if (kingdom.getParliamentState().speakerVillagerEntityId().filter(entityId::equals).isPresent()) {
                return true;
            }
            for (MpSeat seat : kingdom.getElectionState().seatsView().values()) {
                if (seat.kind() != MpSeatKind.VILLAGER || seat.isRecessed()) {
                    continue;
                }
                if (seat.entityId().filter(entityId::equals).isPresent()) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<UUID> seatedVillagerEntityIds(Kingdom kingdom) {
        Set<UUID> reserved = new HashSet<>();
        kingdom.getParliamentState().speakerVillagerEntityId().ifPresent(reserved::add);
        for (MpSeat seat : kingdom.getElectionState().seatsView().values()) {
            if (seat.kind() == MpSeatKind.VILLAGER) {
                seat.entityId().ifPresent(reserved::add);
            }
        }
        return reserved;
    }

    private static MpSeatLocation toSeatLocation(Location location) {
        return new MpSeatLocation(
                location.getWorld() != null ? location.getWorld().getName() : "",
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
    }

    private static Location toBukkitLocation(MpSeatLocation location) {
        World world = org.bukkit.Bukkit.getWorld(location.worldName());
        if (world == null) {
            throw new IllegalStateException("World not loaded: " + location.worldName());
        }
        return new Location(
                world, location.x(), location.y(), location.z(), location.yaw(), location.pitch());
    }

    private void attemptStartupSync(int attempt) {
        syncAllKingdoms();
        if (attempt >= STARTUP_SYNC_MAX_ATTEMPTS || !hasUnknownSeatedEntities()) {
            return;
        }
        plugin.getServer()
                .getScheduler()
                .runTaskLater(plugin, () -> attemptStartupSync(attempt + 1), STARTUP_SYNC_RETRY_TICKS);
    }

    private boolean hasUnknownSeatedEntities() {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            for (MpSeat seat : kingdom.getElectionState().seatsView().values()) {
                if (seat.kind() == MpSeatKind.VILLAGER
                        && locateSeatedEntity(kingdom, seat) == VillagerMpEntityLookup.EntityPresence.UNKNOWN) {
                    return true;
                }
            }
        }
        return false;
    }

    private VillagerMpEntityLookup.EntityPresence locateSeatedEntity(Kingdom kingdom, MpSeat seat) {
        if (seat.kind() != MpSeatKind.VILLAGER) {
            return VillagerMpEntityLookup.EntityPresence.ABSENT_NO_ID;
        }
        Optional<UUID> entityId = seat.entityId();
        if (entityId.isEmpty()) {
            return VillagerMpEntityLookup.EntityPresence.ABSENT_NO_ID;
        }
        if (findEntity(entityId.get()).isPresent()) {
            return VillagerMpEntityLookup.EntityPresence.PRESENT;
        }

        boolean chunkLoaded = false;
        Optional<MpSeatLocation> seatLocation = kingdom.getElectionState().seatLocation(seat.index());
        if (seatLocation.isPresent()) {
            chunkLoaded |= tryLoadChunk(seatLocation.get());
        }
        Optional<MpSeatLocation> origin = seat.originLocation();
        if (origin.isPresent()) {
            chunkLoaded |= tryLoadChunk(origin.get());
        }
        if (!chunkLoaded) {
            return VillagerMpEntityLookup.EntityPresence.UNKNOWN;
        }
        return findEntity(entityId.get()).isPresent()
                ? VillagerMpEntityLookup.EntityPresence.PRESENT
                : VillagerMpEntityLookup.EntityPresence.ABSENT_CONFIRMED;
    }

    private boolean tryLoadChunk(MpSeatLocation location) {
        World world = Bukkit.getWorld(location.worldName());
        if (world == null) {
            return false;
        }
        int chunkX = (int) Math.floor(location.x()) >> 4;
        int chunkZ = (int) Math.floor(location.z()) >> 4;
        world.getChunkAt(chunkX, chunkZ);
        return true;
    }

    private Optional<Entity> findEntity(UUID entityId) {
        Entity entity = plugin.getServer().getEntity(entityId);
        return Optional.ofNullable(entity);
    }
}
