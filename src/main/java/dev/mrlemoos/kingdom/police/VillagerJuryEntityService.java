package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.election.VillagerMpEntityService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.model.election.MpSeatKind;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Claims territory villagers as trial jurors and releases them to stored origin when the trial ends.
 */
public final class VillagerJuryEntityService {

    private final KingdomService kingdomService;
    private final PoliceService policeService;
    private final PoliceCourtService courtService;
    private final VillagerMpEntityService villagerMpEntityService;
    private final NamespacedKey jurorTagKey;
    private final NamespacedKey kingdomTagKey;
    private final NamespacedKey originWorldKey;
    private final NamespacedKey originXKey;
    private final NamespacedKey originYKey;
    private final NamespacedKey originZKey;
    private final Map<String, Set<UUID>> claimedByKingdom = new HashMap<>();

    public VillagerJuryEntityService(
            JavaPlugin plugin,
            KingdomService kingdomService,
            PoliceService policeService,
            PoliceCourtService courtService,
            VillagerMpEntityService villagerMpEntityService) {
        JavaPlugin pluginRef = Objects.requireNonNull(plugin, "plugin");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.policeService = Objects.requireNonNull(policeService, "policeService");
        this.courtService = Objects.requireNonNull(courtService, "courtService");
        this.villagerMpEntityService = Objects.requireNonNull(villagerMpEntityService, "villagerMpEntityService");
        this.jurorTagKey = new NamespacedKey(pluginRef, "trial_juror");
        this.kingdomTagKey = new NamespacedKey(pluginRef, "trial_juror_kingdom");
        this.originWorldKey = new NamespacedKey(pluginRef, "trial_juror_origin_world");
        this.originXKey = new NamespacedKey(pluginRef, "trial_juror_origin_x");
        this.originYKey = new NamespacedKey(pluginRef, "trial_juror_origin_y");
        this.originZKey = new NamespacedKey(pluginRef, "trial_juror_origin_z");
    }

    public Set<UUID> claimJurors(String kingdomId, UUID accusedId, int count) {
        List<UUID> claimed = new ArrayList<>();
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        Optional<CourtLocation> court = policeService.court(kingdomId);
        if (kingdom.isEmpty() || court.isEmpty()) {
            return Set.of();
        }
        World world = Bukkit.getWorld(court.get().worldName());
        if (world == null) {
            return Set.of();
        }
        Location destination = new Location(world, court.get().x() + 0.5, court.get().y(), court.get().z() + 0.5);
        Set<UUID> seatedOfficeIds = seatedOfficeEntityIds(kingdom.get());
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (claimed.size() >= count) {
                break;
            }
            if (!isEligible(villager, kingdomId, accusedId, seatedOfficeIds)) {
                continue;
            }
            claimOne(villager, kingdomId, destination);
            claimed.add(villager.getUniqueId());
        }
        claimedByKingdom.put(kingdomId, new HashSet<>(claimed));
        return Set.copyOf(claimed);
    }

    public void releaseJurors(String kingdomId) {
        Set<UUID> claimed = claimedByKingdom.remove(kingdomId);
        if (claimed == null) {
            return;
        }
        for (UUID id : claimed) {
            findVillager(id).ifPresent(this::releaseOne);
        }
    }

    private boolean isEligible(
            Villager villager, String kingdomId, UUID accusedId, Set<UUID> seatedOfficeIds) {
        if (!villager.isValid() || villager.isDead()) {
            return false;
        }
        boolean accused = accusedId != null && accusedId.equals(villager.getUniqueId());
        boolean seatedMpOrPremier = seatedOfficeIds.contains(villager.getUniqueId())
                || villagerMpEntityService.isKingdomTaggedMpVillager(villager);
        boolean speaker = kingdomService
                .getKingdom(kingdomId)
                .flatMap(k -> k.getParliamentState().speakerVillagerEntityId())
                .filter(id -> id.equals(villager.getUniqueId()))
                .isPresent();
        boolean treasuryLord = villagerMpEntityService.isTreasuryLordVillager(villager);
        boolean villagerJudge = courtService.isJudgeEntity(villager);
        boolean townCrier = villagerMpEntityService.isTownCrierVillager(villager);
        boolean cleric = villagerMpEntityService.isClericVillager(villager);
        boolean striking = villager.getCustomName() != null
                && villager.getCustomName().toLowerCase(java.util.Locale.ROOT).contains("strike");
        return VillagerJurorEligibility.isEligible(new VillagerJurorEligibility.Flags(
                accused, seatedMpOrPremier, speaker, treasuryLord, villagerJudge, townCrier, striking, cleric));
    }

    private Set<UUID> seatedOfficeEntityIds(Kingdom kingdom) {
        Set<UUID> ids = new HashSet<>();
        for (MpSeat seat : kingdom.getElectionState().seatsView().values()) {
            if (seat.kind() == MpSeatKind.VILLAGER) {
                seat.entityId().ifPresent(ids::add);
            }
        }
        kingdom.getParliamentState().speakerVillagerEntityId().ifPresent(ids::add);
        return ids;
    }

    private void claimOne(Villager villager, String kingdomId, Location destination) {
        Location origin = villager.getLocation().clone();
        World originWorld = origin.getWorld();
        if (originWorld != null) {
            villager.getPersistentDataContainer()
                    .set(originWorldKey, PersistentDataType.STRING, originWorld.getName());
            villager.getPersistentDataContainer().set(originXKey, PersistentDataType.DOUBLE, origin.getX());
            villager.getPersistentDataContainer().set(originYKey, PersistentDataType.DOUBLE, origin.getY());
            villager.getPersistentDataContainer().set(originZKey, PersistentDataType.DOUBLE, origin.getZ());
        }
        if (villager.isSleeping()) {
            villager.wakeup();
        }
        villager.setPose(Pose.STANDING);
        villager.teleport(destination);
        villager.setAI(false);
        villager.setSilent(true);
        villager.setInvulnerable(true);
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);
        villager.getPersistentDataContainer().set(jurorTagKey, PersistentDataType.BYTE, (byte) 1);
        villager.getPersistentDataContainer().set(kingdomTagKey, PersistentDataType.STRING, kingdomId);
    }

    private void releaseOne(Villager villager) {
        String worldName = villager.getPersistentDataContainer().get(originWorldKey, PersistentDataType.STRING);
        Double x = villager.getPersistentDataContainer().get(originXKey, PersistentDataType.DOUBLE);
        Double y = villager.getPersistentDataContainer().get(originYKey, PersistentDataType.DOUBLE);
        Double z = villager.getPersistentDataContainer().get(originZKey, PersistentDataType.DOUBLE);
        if (worldName != null && x != null && y != null && z != null) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                villager.teleport(new Location(world, x, y, z));
            }
        }
        villager.setAI(true);
        villager.setSilent(false);
        villager.setInvulnerable(false);
        villager.setPersistent(false);
        villager.setRemoveWhenFarAway(true);
        villager.getPersistentDataContainer().remove(jurorTagKey);
        villager.getPersistentDataContainer().remove(kingdomTagKey);
        villager.getPersistentDataContainer().remove(originWorldKey);
        villager.getPersistentDataContainer().remove(originXKey);
        villager.getPersistentDataContainer().remove(originYKey);
        villager.getPersistentDataContainer().remove(originZKey);
    }

    private Optional<Villager> findVillager(UUID entityId) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(entityId) && entity instanceof Villager villager) {
                    return Optional.of(villager);
                }
            }
        }
        return Optional.empty();
    }
}
