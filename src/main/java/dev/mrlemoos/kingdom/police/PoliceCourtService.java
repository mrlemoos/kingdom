package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class PoliceCourtService {

    private final KingdomService kingdomService;
    private final PoliceService policeService;
    private final NamespacedKey judgeTagKey;
    private final NamespacedKey kingdomTagKey;

    public PoliceCourtService(JavaPlugin plugin, KingdomService kingdomService, PoliceService policeService) {
        JavaPlugin pluginRef = Objects.requireNonNull(plugin, "plugin");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.policeService = Objects.requireNonNull(policeService, "policeService");
        this.judgeTagKey = new NamespacedKey(pluginRef, "police_judge");
        this.kingdomTagKey = new NamespacedKey(pluginRef, "police_kingdom");
    }

    public void ensureJudge(String kingdomId) {
        Optional<CourtLocation> court = policeService.court(kingdomId);
        if (court.isEmpty()) {
            return;
        }

        List<UUID> presentJudgeIds = findPresentJudgeIds(kingdomId, court.get());
        Optional<UUID> storedId = policeService.judgeEntityId(kingdomId);
        Optional<UUID> canonicalId = selectCanonicalJudge(storedId, presentJudgeIds);

        if (canonicalId.isPresent()) {
            Optional<Villager> canonical = findVillagerById(canonicalId.get());
            if (canonical.isPresent() && isValidJudge(canonical.get(), kingdomId)) {
                configureJudge(canonical.get(), kingdomId);
                removeJudgesAtCourt(kingdomId, court.get(), canonicalId);
                if (storedId.isEmpty() || !storedId.get().equals(canonicalId.get())) {
                    policeService.setJudgeEntityId(kingdomId, canonicalId.get());
                }
                return;
            }
        }

        removeJudgesAtCourt(kingdomId, court.get(), Optional.empty());
        Villager judge = spawnJudge(kingdomId, court.get());
        policeService.setJudgeEntityId(kingdomId, judge.getUniqueId());
    }

    public void respawnAllJudges() {
        for (var kingdom : kingdomService.listKingdoms()) {
            if (policeService.hasCourt(kingdom.getId())) {
                ensureJudge(kingdom.getId());
            }
        }
    }

    public void pruneStaleJudges() {
        for (var kingdom : kingdomService.listKingdoms()) {
            String kingdomId = kingdom.getId();
            Optional<UUID> storedId = policeService.judgeEntityId(kingdomId);
            if (storedId.isEmpty()) {
                continue;
            }
            if (findVillagerById(storedId.get()).filter(v -> isValidJudge(v, kingdomId)).isEmpty()) {
                policeService.clearJudgeEntityId(kingdomId);
            }
        }
    }

    public boolean isJudgeEntity(Villager villager) {
        Byte tag = villager.getPersistentDataContainer().get(judgeTagKey, PersistentDataType.BYTE);
        return tag != null && tag == 1;
    }

    public Optional<String> kingdomIdForJudge(Villager villager) {
        if (!isJudgeEntity(villager)) {
            return Optional.empty();
        }
        String kingdomId = villager.getPersistentDataContainer().get(kingdomTagKey, PersistentDataType.STRING);
        return kingdomId == null || kingdomId.isBlank() ? Optional.empty() : Optional.of(kingdomId);
    }

    public void despawnJudge(String kingdomId) {
        Optional<CourtLocation> court = policeService.court(kingdomId);
        if (court.isEmpty()) {
            return;
        }
        removeJudgesAtCourt(kingdomId, court.get(), Optional.empty());
        policeService.clearJudgeEntityId(kingdomId);
    }

    private Optional<UUID> selectCanonicalJudge(Optional<UUID> storedId, List<UUID> presentIds) {
        if (storedId.isPresent() && presentIds.contains(storedId.get())) {
            return storedId;
        }
        if (!presentIds.isEmpty()) {
            return Optional.of(presentIds.get(0));
        }
        return storedId;
    }

    private void removeJudgesAtCourt(String kingdomId, CourtLocation court, Optional<UUID> keepJudgeId) {
        for (UUID judgeId : findPresentJudgeIds(kingdomId, court)) {
            if (keepJudgeId.isPresent() && keepJudgeId.get().equals(judgeId)) {
                continue;
            }
            removeEntityById(judgeId);
        }
        policeService.judgeEntityId(kingdomId)
                .filter(id -> keepJudgeId.isEmpty() || !id.equals(keepJudgeId.get()))
                .ifPresent(this::removeEntityById);
    }

    private List<UUID> findPresentJudgeIds(String kingdomId, CourtLocation court) {
        World world = Bukkit.getWorld(court.worldName());
        if (world == null) {
            return List.of();
        }

        List<UUID> present = new ArrayList<>();
        Location location = judgeLocation(world, court);
        for (Entity entity : world.getNearbyEntities(location, 1.0, 2.0, 1.0)) {
            if (entity instanceof Villager villager
                    && isValidJudge(villager, kingdomId)
                    && !present.contains(entity.getUniqueId())) {
                present.add(entity.getUniqueId());
            }
        }

        policeService.judgeEntityId(kingdomId)
                .flatMap(this::findVillagerById)
                .filter(villager -> isValidJudge(villager, kingdomId))
                .ifPresent(villager -> {
                    UUID id = villager.getUniqueId();
                    if (!present.contains(id)) {
                        present.add(id);
                    }
                });
        return present;
    }

    private Villager spawnJudge(String kingdomId, CourtLocation court) {
        World world = Bukkit.getWorld(court.worldName());
        if (world == null) {
            throw new IllegalStateException("World not loaded: " + court.worldName());
        }

        Location location = judgeLocation(world, court).add(0.5, 0.0, 0.5);
        return world.spawn(location, Villager.class, spawned -> configureJudge(spawned, kingdomId));
    }

    private void configureJudge(Villager villager, String kingdomId) {
        villager.setAI(false);
        villager.setSilent(true);
        villager.setInvulnerable(true);
        villager.setCollidable(false);
        villager.setRemoveWhenFarAway(false);
        villager.setCustomName(PoliceAppearance.judgeVillagerNametag());
        villager.setCustomNameVisible(true);
        villager.setProfession(Villager.Profession.LIBRARIAN);
        villager.setVillagerType(Villager.Type.PLAINS);
        villager.setRecipes(new ArrayList<>());
        villager.getPersistentDataContainer().set(judgeTagKey, PersistentDataType.BYTE, (byte) 1);
        villager.getPersistentDataContainer().set(kingdomTagKey, PersistentDataType.STRING, kingdomId);
    }

    private boolean isValidJudge(Villager villager, String kingdomId) {
        return villager.isValid()
                && !villager.isDead()
                && isJudgeEntity(villager)
                && kingdomIdForJudge(villager).filter(kingdomId::equals).isPresent();
    }

    private static Location judgeLocation(World world, CourtLocation court) {
        return new Location(world, court.x(), court.y(), court.z() - 1);
    }

    private Optional<Villager> findVillagerById(UUID entityId) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(entityId) && entity instanceof Villager villager) {
                    return Optional.of(villager);
                }
            }
        }
        return Optional.empty();
    }

    private void removeEntityById(UUID entityId) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(entityId)) {
                    entity.remove();
                    return;
                }
            }
        }
    }
}
