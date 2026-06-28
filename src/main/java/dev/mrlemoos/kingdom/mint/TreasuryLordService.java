package dev.mrlemoos.kingdom.mint;

import dev.mrlemoos.kingdom.economy.model.KingdomEconomy;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
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

public final class TreasuryLordService {

    private final JavaPlugin plugin;
    private final EconomyService economyService;
    private final YamlEconomyStore economyStore;
    private final NamespacedKey lordTagKey;
    private final NamespacedKey kingdomTagKey;

    public TreasuryLordService(
            JavaPlugin plugin, EconomyService economyService, YamlEconomyStore economyStore) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.economyService = Objects.requireNonNull(economyService, "economyService");
        this.economyStore = Objects.requireNonNull(economyStore, "economyStore");
        this.lordTagKey = new NamespacedKey(plugin, "treasury_lord");
        this.kingdomTagKey = new NamespacedKey(plugin, "treasury_kingdom");
    }

    public MintLocation ensureLord(String kingdomId, MintLocation mint) {
        List<UUID> presentLordIds = findPresentLordIds(kingdomId, mint);
        Optional<UUID> canonicalId = TreasuryLordPresence.selectCanonicalLord(mint.lordEntityId(), presentLordIds);

        if (canonicalId.isPresent()) {
            Optional<Villager> canonical = findVillagerById(canonicalId.get());
            if (canonical.isPresent() && isValidLord(canonical.get(), kingdomId)) {
                removeLordsAtMint(kingdomId, mint, canonicalId);
                MintLocation updated = mint.lordEntityId()
                                .filter(canonicalId.get()::equals)
                                .isPresent()
                        ? mint
                        : mint.withTreasuryLordUuid(canonicalId.get().toString());
                if (!updated.equals(mint)) {
                    updateMintInEconomy(kingdomId, mint, updated);
                }
                return updated;
            }
        }

        removeLordsAtMint(kingdomId, mint, Optional.empty());
        Villager lord = spawnLord(kingdomId, mint);
        MintLocation updated = mint.withTreasuryLordUuid(lord.getUniqueId().toString());
        updateMintInEconomy(kingdomId, mint, updated);
        return updated;
    }

    public void despawnLord(String kingdomId, MintLocation mint) {
        removeLordsAtMint(kingdomId, mint, Optional.empty());
    }

    public boolean releaseLord(String kingdomId, MintLocation mint) {
        despawnLord(kingdomId, mint);
        KingdomEconomy economy = economyService.kingdomEconomies().get(kingdomId);
        if (economy == null || !economy.hasMintAt(mint)) {
            return false;
        }
        MintLocation cleared = mint.withTreasuryLordUuid(null);
        economy.replaceMintLocation(mint, cleared);
        economyStore.saveFrom(economyService);
        return true;
    }

    public Optional<Villager> findLord(MintLocation mint) {
        Optional<Villager> byId = mint.lordEntityId().flatMap(this::findVillagerById);
        if (byId.isPresent()) {
            return byId;
        }

        World world = Bukkit.getWorld(mint.worldName());
        if (world == null) {
            return Optional.empty();
        }

        Location location = lordLocation(world, mint);
        for (Entity entity : world.getNearbyEntities(location, 1.0, 2.0, 1.0)) {
            if (entity instanceof Villager villager && isLordEntity(villager)) {
                return Optional.of(villager);
            }
        }
        return Optional.empty();
    }

    public Optional<String> kingdomIdForLord(Villager villager) {
        if (!isLordEntity(villager)) {
            return Optional.empty();
        }
        String kingdomId = villager.getPersistentDataContainer().get(kingdomTagKey, PersistentDataType.STRING);
        return kingdomId == null || kingdomId.isBlank() ? Optional.empty() : Optional.of(kingdomId);
    }

    public boolean isLordEntity(Villager villager) {
        Byte tag = villager.getPersistentDataContainer().get(lordTagKey, PersistentDataType.BYTE);
        return tag != null && tag == 1;
    }

    public Optional<MintLocation> findMintForLord(Villager villager) {
        Optional<String> kingdomId = kingdomIdForLord(villager);
        if (kingdomId.isEmpty()) {
            return Optional.empty();
        }
        KingdomEconomy economy = economyService.kingdomEconomies().get(kingdomId.get());
        if (economy == null) {
            return Optional.empty();
        }
        return economy.findMintByLordUuid(villager.getUniqueId());
    }

    public void respawnAllLords() {
        for (var entry : economyService.kingdomEconomies().entrySet()) {
            String kingdomId = entry.getKey();
            for (MintLocation mint : entry.getValue().mintLocations()) {
                ensureLord(kingdomId, mint);
            }
        }
        economyStore.saveFrom(economyService);
    }

    public NamespacedKey lordTagKey() {
        return lordTagKey;
    }

    private void removeLordsAtMint(String kingdomId, MintLocation mint, Optional<UUID> keepLordId) {
        List<UUID> presentLordIds = findPresentLordIds(kingdomId, mint);
        for (UUID lordId : TreasuryLordPresence.lordIdsToRemove(presentLordIds, keepLordId)) {
            removeEntityById(lordId);
        }
        mint.lordEntityId()
                .filter(id -> keepLordId.isEmpty() || !id.equals(keepLordId.get()))
                .ifPresent(this::removeEntityById);
    }

    private List<UUID> findPresentLordIds(String kingdomId, MintLocation mint) {
        World world = Bukkit.getWorld(mint.worldName());
        if (world == null) {
            return List.of();
        }

        List<UUID> present = new ArrayList<>();
        Location location = lordLocation(world, mint);
        for (Entity entity : world.getNearbyEntities(location, 1.0, 2.0, 1.0)) {
            if (entity instanceof Villager villager
                    && isValidLord(villager, kingdomId)
                    && !present.contains(entity.getUniqueId())) {
                present.add(entity.getUniqueId());
            }
        }

        mint.lordEntityId()
                .flatMap(this::findVillagerById)
                .filter(villager -> isValidLord(villager, kingdomId))
                .map(Villager::getUniqueId)
                .filter(id -> !present.contains(id))
                .ifPresent(present::add);
        return present;
    }

    private Villager spawnLord(String kingdomId, MintLocation mint) {
        World world = Bukkit.getWorld(mint.worldName());
        if (world == null) {
            throw new IllegalStateException("World not loaded: " + mint.worldName());
        }

        Location location = lordLocation(world, mint).add(0.5, 0.0, 0.5);
        return world.spawn(location, Villager.class, spawned -> configureLord(spawned, kingdomId));
    }

    private void configureLord(Villager villager, String kingdomId) {
        villager.setAI(false);
        villager.setSilent(true);
        villager.setInvulnerable(true);
        villager.setCollidable(false);
        villager.setRemoveWhenFarAway(false);
        villager.setCustomName(TreasuryLordPlacement.LORD_DISPLAY_NAME);
        villager.setCustomNameVisible(true);
        villager.setProfession(Villager.Profession.LIBRARIAN);
        villager.setVillagerType(Villager.Type.PLAINS);
        villager.setRecipes(new java.util.ArrayList<>());
        villager.getPersistentDataContainer().set(lordTagKey, PersistentDataType.BYTE, (byte) 1);
        villager.getPersistentDataContainer().set(kingdomTagKey, PersistentDataType.STRING, kingdomId);
    }

    private void updateMintInEconomy(String kingdomId, MintLocation oldMint, MintLocation updated) {
        KingdomEconomy economy = economyService.kingdomEconomies().get(kingdomId);
        if (economy != null) {
            economy.replaceMintLocation(oldMint, updated);
        }
    }

    private static Location lordLocation(World world, MintLocation mint) {
        return new Location(
                world,
                TreasuryLordPlacement.lordBlockX(mint),
                TreasuryLordPlacement.lordBlockY(mint),
                TreasuryLordPlacement.lordBlockZ(mint));
    }

    private boolean isValidLord(Villager villager, String kingdomId) {
        return villager.isValid()
                && !villager.isDead()
                && isLordEntity(villager)
                && kingdomIdForLord(villager).filter(kingdomId::equals).isPresent();
    }

    private Optional<Villager> findVillagerById(UUID entityId) {
        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(entityId) && entity instanceof Villager villager) {
                    return Optional.of(villager);
                }
            }
        }
        return Optional.empty();
    }

    private void removeEntityById(UUID entityId) {
        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(entityId)) {
                    entity.remove();
                    return;
                }
            }
        }
    }
}
