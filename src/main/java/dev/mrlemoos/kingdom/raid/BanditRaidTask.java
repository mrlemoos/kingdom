package dev.mrlemoos.kingdom.raid;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.calendar.RealmDate;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.city.CapitalLocation;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Dawn warnings and dusk city-hall raids. Active fights are deliberately memory-only. */
public final class BanditRaidTask implements Runnable, org.bukkit.event.Listener {
    private static final long RAID_DURATION_MS = 5 * 60 * 1000L;
    private static final int DEFENCE_RADIUS = 48;
    private final JavaPlugin plugin;
    private final KingdomService kingdoms;
    private final RealmCalendarService calendar;
    private final EconomyService economy;
    private final Runnable save;
    private final NamespacedKey raidKey;
    private final NamespacedKey kingdomKey;
    private final Random random = new Random();
    private final Map<String, Pending> warned = new HashMap<>();
    private final Map<String, Active> active = new HashMap<>();

    public BanditRaidTask(JavaPlugin plugin, KingdomService kingdoms, RealmCalendarService calendar,
            EconomyService economy, Runnable save) {
        this.plugin = plugin;
        this.kingdoms = kingdoms;
        this.calendar = calendar;
        this.economy = economy;
        this.save = save;
        raidKey = new NamespacedKey(plugin, "bandit_raid");
        kingdomKey = new NamespacedKey(plugin, "bandit_raid_kingdom");
        removeOrphans();
        boolean staleNotice = false;
        for (Kingdom kingdom : kingdoms.listKingdoms()) {
            if (kingdom.getCityState().banditRaidNotice().orElse("").startsWith("Bandit raid active")) {
                kingdom.getCityState().setBanditRaidNotice("");
                staleNotice = true;
            }
        }
        if (staleNotice) save.run();
    }

    @Override public void run() {
        long realmDay = calendar.currentRealmDay();
        RealmDate date = calendar.today();
        String season = date.realmYear() + ":" + date.month().season().name();
        for (Kingdom kingdom : kingdoms.listKingdoms()) {
            String id = kingdom.getId();
            Pending previousWarning = warned.get(id);
            if (previousWarning != null && (!previousWarning.season.equals(season) || previousWarning.day != realmDay)) {
                warned.remove(id);
            }
            if (active.containsKey(id)) {
                if (kingdom.getCityState().capital().isEmpty()) { cancel(id, active.get(id)); continue; }
                tickActive(kingdom, active.get(id));
                continue;
            }
            if (kingdom.getCityState().lastBanditRaidSeason().filter(season::equals).isPresent()) continue;
            CapitalLocation capital = kingdom.getCityState().capital().orElse(null);
            if (capital == null) continue;
            World world = Bukkit.getWorld(capital.worldName());
            if (world == null) continue;
            long time = world.getTime();
            Location hall = new Location(world, capital.x(), capital.y(), capital.z());
            if (time >= 0L && time < 100L && !warned.containsKey(id)) {
                List<Player> near = nearbyMembers(id, hall, 96);
                if (near.isEmpty()) continue;
                warned.put(id, new Pending(season, realmDay));
                String coordinates = world.getName() + " " + hall.getBlockX() + ", " + hall.getBlockY() + ", " + hall.getBlockZ();
                kingdom.getCityState().setBanditRaidNotice("Defend city hall at dusk. Any member may fight. " + coordinates);
                String message = "&6Bandits threaten city hall at dawn. Defend city hall at dusk. Any member may fight. &e" + coordinates;
                RealmFeedback.kingdomMessage(kingdoms, id, message);
                near.forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1f, 0.7f));
                continue;
            }
            Pending pending = warned.get(id);
            if (pending == null || !BanditRaidRules.warningMatchesDusk(
                    pending.season, pending.day, season, realmDay, time)) continue;
            List<Player> near = nearbyMembers(id, hall, 96);
            if (near.isEmpty()) {
                RealmFeedback.kingdomMessage(kingdoms, id, "&7The bandits wait; no defenders are near city hall. The warning will be repeated.");
                warned.remove(id);
                continue;
            }
            List<Pillager> spawned = spawnBandits(kingdom, hall, BanditRaidRules.banditCount(near.size()));
            if (spawned.size() != BanditRaidRules.banditCount(near.size())) {
                spawned.forEach(Entity::remove);
                continue;
            }
            BossBar bar = Bukkit.createBossBar(c("&cBandits at city hall"), BarColor.RED, BarStyle.SOLID);
            Active fight = new Active(season, System.currentTimeMillis() + RAID_DURATION_MS, hall, spawned, bar);
            active.put(id, fight);
            kingdom.getCityState().setBanditRaidNotice("Bandit raid active at city hall.");
            warned.remove(id);
            syncBar(id, fight);
            RealmFeedback.kingdomMessage(kingdoms, id, "&cBandits attack city hall!");
        }
    }

    private List<Pillager> spawnBandits(Kingdom kingdom, Location hall, int count) {
        World world = hall.getWorld();
        if (world == null) return List.of();
        List<Pillager> result = new ArrayList<>();
        for (int i = 0; i < count * 12 && result.size() < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int distance = 24 + random.nextInt(13);
            int x = hall.getBlockX() + (int) (Math.cos(angle) * distance);
            int z = hall.getBlockZ() + (int) (Math.sin(angle) * distance);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            int y = world.getHighestBlockYAt(x, z) + 1;
        Location at = new Location(world, x + 0.5, y, z + 0.5);
            if (!safe(at, kingdom)) continue;
            Pillager pillager = world.spawn(at, Pillager.class, entity -> {
                entity.setPatrolLeader(false);
                entity.setRemoveWhenFarAway(false);
                entity.getPersistentDataContainer().set(raidKey, PersistentDataType.BYTE, (byte) 1);
                entity.getPersistentDataContainer().set(kingdomKey, PersistentDataType.STRING, kingdom.getId());
            });
            result.add(pillager);
        }
        return result;
    }

    private boolean safe(Location at, Kingdom kingdom) {
        if (!at.getBlock().isPassable() || !at.clone().add(0, 1, 0).getBlock().isPassable()
                || !at.clone().add(0, -1, 0).getBlock().getType().isSolid()
                || at.getBlock().isLiquid() || at.clone().add(0, -1, 0).getBlock().isLiquid()) return false;
        org.bukkit.Material floor = at.clone().add(0, -1, 0).getBlock().getType();
        if (floor == org.bukkit.Material.MAGMA_BLOCK || floor == org.bukkit.Material.CACTUS
                || floor == org.bukkit.Material.CAMPFIRE || floor == org.bukkit.Material.SOUL_CAMPFIRE
                || floor == org.bukkit.Material.POWDER_SNOW) return false;
        for (String region : dev.mrlemoos.kingdom.worldguard.WorldGuardBridge.regionsAt(
                at.getWorld().getName(), at.getBlockX(), at.getBlockY(), at.getBlockZ())) {
            if ("__global__".equalsIgnoreCase(region)) continue;
            if (kingdom.containsWorldGuardRegion(region)) continue;
            return false;
        }
        return true;
    }

    private void tickActive(Kingdom kingdom, Active fight) {
        if (fight.hall.getWorld() == null || !fight.hall.getWorld().isChunkLoaded(
                fight.hall.getBlockX() >> 4, fight.hall.getBlockZ() >> 4)) { cancel(kingdom.getId(), fight); return; }
        List<Player> defenders = nearbyMembers(kingdom.getId(), fight.hall, 96);
        if (defenders.isEmpty()) { cancel(kingdom.getId(), fight); return; }
        fight.bandits.removeIf(entity -> {
            boolean drivenOff = entity.isValid() && !entity.isDead()
                    && horizontalDistanceSquared(entity.getLocation(), fight.hall) > DEFENCE_RADIUS * DEFENCE_RADIUS;
            if (drivenOff) entity.remove();
            return !entity.isValid() || entity.isDead() || drivenOff;
        });
        if (fight.bandits.isEmpty()) { finish(kingdom, fight, true); return; }
        if (System.currentTimeMillis() >= fight.deadline) { finish(kingdom, fight, false); return; }
        syncBar(kingdom.getId(), fight);
    }

    private void syncBar(String kingdomId, Active fight) {
        int remaining = fight.bandits.size();
        long ms = Math.max(0L, fight.deadline - System.currentTimeMillis());
        String timer = (ms / 60000) + ":" + String.format("%02d", (ms / 1000) % 60);
        fight.bar.setTitle(c("&cBandits at city hall &f· " + remaining + " remain · " + timer));
        kingdoms.getKingdom(kingdomId).ifPresent(kingdom -> kingdom.getCityState()
                .setBanditRaidNotice("Bandit raid active: " + remaining + " remain, " + timer + "."));
        fight.bar.setProgress(Math.max(0.0, Math.min(1.0, ms / (double) RAID_DURATION_MS)));
        fight.bar.removeAll();
        for (Player member : nearbyMembers(kingdomId, fight.hall, Double.MAX_VALUE)) fight.bar.addPlayer(member);
        fight.bar.setVisible(true);
    }

    private void finish(Kingdom kingdom, Active fight, boolean won) {
        String id = kingdom.getId();
        fight.bandits.forEach(Entity::remove);
        clearBar(fight);
        active.remove(id);
        kingdom.getCityState().setLastBanditRaidSeason(fight.season);
        double loss = won ? 0.0 : economy.debitTreasury(id, BanditRaidRules.loss(economy.getTreasuryBalance(id)));
        RealmFeedback.kingdomMessage(kingdoms, id, won ? "&aCity hall is safe; the bandits are driven off." : "&cCity hall falls. &e" + loss + " Corona taken from the treasury.");
        kingdom.getCityState().setBanditRaidNotice(won ? "Raid won: bandits driven from city hall." : "Raid lost: " + loss + " Corona taken from the treasury.");
        if (fight.hall.getWorld() != null) {
            fight.hall.getWorld().playSound(fight.hall, won ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            fight.hall.getWorld().spawnParticle(won ? Particle.HAPPY_VILLAGER : Particle.ANGRY_VILLAGER, fight.hall, 24);
        }
        kingdom.getCityState().addGazettePost(new dev.mrlemoos.kingdom.model.city.GazettePost(
                "Bandit raid", won ? "The bandits were driven from city hall." : "The raid failed; " + loss + " Corona were taken from the treasury.",
                new UUID(0L, 0L), fight.hall.getWorld() == null ? 0 : fight.hall.getWorld().getFullTime() / 24000L,
                dev.mrlemoos.kingdom.model.city.GazettePostKind.ANNOUNCEMENT, java.util.Optional.empty()));
        save.run();
    }

    private void cancel(String id, Active fight) {
        fight.bandits.forEach(Entity::remove);
        clearBar(fight);
        active.remove(id);
        kingdoms.getKingdom(id).ifPresent(kingdom -> kingdom.getCityState().setBanditRaidNotice(""));
        save.run();
    }
    private static void clearBar(Active fight) { fight.bar.removeAll(); fight.bar.setVisible(false); }
    private void removeOrphans() {
        for (World world : Bukkit.getWorlds()) for (Entity entity : world.getEntities())
            if (entity.getPersistentDataContainer().has(raidKey, PersistentDataType.BYTE)) entity.remove();
    }
    public void stop() {
        for (Map.Entry<String, Active> entry : Map.copyOf(active).entrySet()) cancel(entry.getKey(), entry.getValue());
        warned.clear();
    }

    private boolean isRaidBandit(Entity entity) {
        return entity != null && entity.getPersistentDataContainer().has(raidKey, PersistentDataType.BYTE);
    }

    @org.bukkit.event.EventHandler
    public void protectRealmNpcs(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Entity shooter) damager = shooter;
        if (!isRaidBandit(damager)) return;
        if (event.getEntity() instanceof org.bukkit.entity.Villager
                || event.getEntity().getPersistentDataContainer().has(new NamespacedKey(plugin, "lord_mayor"), PersistentDataType.BYTE)
                || event.getEntity().getPersistentDataContainer().has(new NamespacedKey(plugin, "town_crier"), PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    @org.bukkit.event.EventHandler
    public void targetOnlyDefenders(org.bukkit.event.entity.EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Pillager pillager) || !isRaidBandit(pillager)) return;
        String kingdomId = pillager.getPersistentDataContainer().get(kingdomKey, PersistentDataType.STRING);
        Entity target = event.getTarget();
        if (target == null) return;
        boolean defender = target instanceof Player player && kingdoms.getMembership(player.getUniqueId())
                .map(membership -> membership.getKingdomId().equals(kingdomId)).orElse(false);
        boolean localGolem = target instanceof org.bukkit.entity.IronGolem
                && target.getPersistentDataContainer().has(new NamespacedKey(plugin, "police_golem"), PersistentDataType.BYTE)
                && kingdomId != null && kingdomId.equals(target.getPersistentDataContainer()
                        .get(new NamespacedKey(plugin, "police_kingdom"), PersistentDataType.STRING))
                && distance(target.getLocation(), pillager.getLocation()) <= DEFENCE_RADIUS;
        boolean localSquad = target.getPersistentDataContainer().has(new NamespacedKey(plugin, "crown_squad_unit"), PersistentDataType.STRING)
                && kingdomId != null && kingdomId.equals(target.getPersistentDataContainer()
                        .get(new NamespacedKey(plugin, "crown_squad_kingdom"), PersistentDataType.STRING))
                && distance(target.getLocation(), pillager.getLocation()) <= DEFENCE_RADIUS;
        if (!defender && !localGolem && !localSquad) {
            event.setCancelled(true);
        }
    }

    @org.bukkit.event.EventHandler
    public void removeOrphansOnChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (isRaidBandit(entity) && active.values().stream().noneMatch(fight -> fight.bandits.contains(entity))) entity.remove();
        }
    }
    private List<Player> nearbyMembers(String id, Location centre, double radius) {
        List<Player> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerMembership membership = kingdoms.getMembership(player.getUniqueId()).orElse(null);
            if (membership != null && id.equals(membership.getKingdomId()) && player.getWorld().equals(centre.getWorld())
                    && distance(player.getLocation(), centre) <= radius) result.add(player);
        }
        return result;
    }
    private static double distance(Location a, Location b) { return a.getWorld() == b.getWorld() ? a.distance(b) : Double.MAX_VALUE; }
    private static double horizontalDistanceSquared(Location a, Location b) {
        double dx = a.getX() - b.getX(); double dz = a.getZ() - b.getZ(); return dx * dx + dz * dz;
    }
    private record Pending(String season, long day) {}
    private static final class Active {
        private final String season; private final long deadline; private final Location hall; private final List<Pillager> bandits; private final BossBar bar;
        private Active(String season, long deadline, Location hall, List<Pillager> bandits, BossBar bar) {
            this.season = season; this.deadline = deadline; this.hall = hall; this.bandits = bandits; this.bar = bar;
        }
    }
}
