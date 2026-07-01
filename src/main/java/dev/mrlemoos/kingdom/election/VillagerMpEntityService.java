package dev.mrlemoos.kingdom.election;

import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.model.election.MpSeatKind;
import dev.mrlemoos.kingdom.model.election.MpSeatLocation;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
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
    private final NamespacedKey treasuryLordTagKey;

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
        this.treasuryLordTagKey = new NamespacedKey(plugin, "treasury_lord");
    }

    public void syncKingdom(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> {
            releaseOrphanedMpVillagers(kingdomId);
            reconcileStrandedMpVillagers(kingdom);
            syncSeats(kingdom);
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
        kingdomService.getKingdom(kingdomId).flatMap(k -> k.getElectionState().seat(seatIndex)).ifPresent(seat -> {
            if (seat.kind() != MpSeatKind.VILLAGER) {
                return;
            }
            seat.entityId().ifPresent(entityId -> releaseEntity(seat, entityId));
            seat.setEntityId(null);
            seat.setOriginLocation(null);
        });
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
        applyStandardNametag(villager, VillagerMpProfessionMatcher.professionName(profession));
    }

    public void reconcileTerritoryVillagerNametag(Villager villager) {
        if (!isEligibleForOrdinaryTerritoryNametag(villager)) {
            return;
        }
        String professionName = VillagerMpProfessionMatcher.professionName(villager);
        if (!VillagerTerritoryNametagReconciliation.shouldReconcileNametag(
                villager.getCustomName(), professionName, true)) {
            return;
        }
        applyStandardNametag(villager, professionName);
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
        boolean treasuryLord = isTreasuryLord(villager);
        boolean seatedMp = isSeatedMpVillager(villager.getUniqueId());
        boolean kingdomTaggedMp = isMpVillager(villager);
        if (!TerritoryVillagerDespawnPolicy.shouldManage(treasuryLord, seatedMp, kingdomTaggedMp)) {
            return;
        }

        boolean inTerritory = isInAnyKingdomTerritory(villager);
        if (TerritoryVillagerDespawnPolicy.shouldApplyProtection(inTerritory, true)) {
            applyTerritoryDespawnProtection(villager);
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
        String regionId = kingdom.getWorldGuardRegion();
        if (regionId == null || regionId.isBlank()) {
            return;
        }
        String worldName = kingdomService.resolveWorldName(kingdom);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        Set<UUID> seatedMpIds = seatedVillagerEntityIds(kingdom);
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (isTreasuryLord(villager) || seatedMpIds.contains(villager.getUniqueId())) {
                continue;
            }
            if (isMpVillager(villager)) {
                continue;
            }
            if (!isInKingdomTerritory(villager, kingdom)) {
                continue;
            }
            applyStandardNametag(villager);
        }
    }

    private void syncSeats(Kingdom kingdom) {
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
                        configureMpBehaviour(villager, seat, kingdom.getId());
                    }
                });
                continue;
            }
            if (!VillagerMpEntityLookup.shouldReplaceSeatedEntity(presence)) {
                continue;
            }
            seatVillagerEntity(kingdom, seat, location.get(), reserved);
        }
    }

    private void seatVillagerEntity(
            Kingdom kingdom, MpSeat seat, MpSeatLocation seatLocation, Set<UUID> reservedEntityIds) {
        Optional<Villager> candidate = villagerScanner.findCandidate(
                kingdom,
                seat.profession().orElse("none"),
                reservedEntityIds,
                villager -> isTreasuryLord(villager) || isMpVillager(villager));
        if (candidate.isPresent()) {
            claimExistingVillager(kingdom, seat, seatLocation, candidate.get());
            return;
        }
        spawnFallbackVillager(kingdom.getId(), seat, seatLocation);
    }

    private void claimExistingVillager(Kingdom kingdom, MpSeat seat, MpSeatLocation seatLocation, Villager villager) {
        Location origin = villager.getLocation().clone();
        MpSeatLocation originSeat = toSeatLocation(origin);
        seat.setOriginLocation(originSeat);
        storeOriginOnEntity(villager, originSeat);
        Location destination = toBukkitLocation(seatLocation);
        villager.teleport(destination);
        configureMpBehaviour(villager, seat, kingdom.getId());
        seat.setEntityId(villager.getUniqueId());
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

    private void releaseEntity(MpSeat seat, UUID entityId) {
        findEntity(entityId).ifPresent(entity -> {
            if (!(entity instanceof Villager villager)) {
                entity.remove();
                return;
            }
            Optional<MpSeatLocation> origin = seat.originLocation();
            if (origin.isEmpty()) {
                origin = readOriginFromEntity(villager);
            }
            origin.ifPresentOrElse(
                    stored -> {
                        Location destination = toBukkitLocation(stored);
                        villager.teleport(destination);
                        restoreDefaultBehaviour(villager);
                    },
                    () -> villager.remove());
        });
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
            readOriginFromEntity(villager).ifPresent(origin -> villager.teleport(toBukkitLocation(origin)));
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
            readOriginFromEntity(villager).ifPresent(origin -> villager.teleport(toBukkitLocation(origin)));
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
        String regionId = kingdom.getWorldGuardRegion();
        if (regionId == null || regionId.isBlank()) {
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
        String label = VillagerTerritoryNametagReconciliation.labelForProfession(professionName);
        villager.setCustomNameVisible(true);
        villager.setCustomName(label);
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

    private boolean isSeatedMpVillager(UUID entityId) {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            if (seatedVillagerEntityIds(kingdom).contains(entityId)) {
                return true;
            }
        }
        return false;
    }

    private Set<UUID> seatedVillagerEntityIds(Kingdom kingdom) {
        Set<UUID> reserved = new HashSet<>();
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
