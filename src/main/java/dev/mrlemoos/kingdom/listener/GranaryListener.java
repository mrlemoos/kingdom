package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.granary.BukkitGranaryScan;
import dev.mrlemoos.kingdom.granary.BukkitGranaryYard;
import dev.mrlemoos.kingdom.granary.BukkitTerritoryHeads;
import dev.mrlemoos.kingdom.granary.GrainTheft;
import dev.mrlemoos.kingdom.granary.GranaryBounds;
import dev.mrlemoos.kingdom.granary.GranaryConfig;
import dev.mrlemoos.kingdom.granary.GranaryContribution;
import dev.mrlemoos.kingdom.granary.GranaryStock;
import dev.mrlemoos.kingdom.granary.GranaryTheftLog;
import dev.mrlemoos.kingdom.granary.HungerLedgerStore;
import dev.mrlemoos.kingdom.granary.HungerRamp;
import dev.mrlemoos.kingdom.granary.WinterRation;
import dev.mrlemoos.kingdom.granary.gui.GranaryStoresGui;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.police.ActBreach;
import dev.mrlemoos.kingdom.police.MechanicalJusticeService;
import dev.mrlemoos.kingdom.police.PoliceResult;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * The granary as the world touches it: a bale broken by any hand but the Crown's is <b>grain
 * theft</b> and files a warrant application, and a bale right-clicked opens the realm's stores.
 *
 * <p>The break itself stands — refusing it would teach nobody anything, where a warrant makes the
 * theft the realm's business (ADR 0007).
 */
public final class GranaryListener implements Listener {

    private final KingdomService kingdomService;
    private final KingdomTerritoryResolver territoryResolver;
    private final MechanicalJusticeService justiceService;
    private final GranaryConfig granaryConfig;
    private final GranaryTheftLog theftLog;
    private final HungerLedgerStore hungerLedger;
    private final RealmCalendarService calendarService;

    public GranaryListener(
            KingdomService kingdomService,
            KingdomTerritoryResolver territoryResolver,
            MechanicalJusticeService justiceService,
            GranaryConfig granaryConfig,
            GranaryTheftLog theftLog,
            HungerLedgerStore hungerLedger,
            RealmCalendarService calendarService) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.territoryResolver = Objects.requireNonNull(territoryResolver, "territoryResolver");
        this.justiceService = Objects.requireNonNull(justiceService, "justiceService");
        this.granaryConfig = granaryConfig != null ? granaryConfig : GranaryConfig.defaults();
        this.theftLog = Objects.requireNonNull(theftLog, "theftLog");
        this.hungerLedger = hungerLedger;
        this.calendarService = calendarService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.HAY_BLOCK) {
            return;
        }
        Optional<Kingdom> kingdom = granaryKingdomAt(block.getLocation());
        if (kingdom.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        NobleRank rank = membership.isPresent() ? membership.get().getRank() : null;
        String breakerKingdomId = membership.isPresent() ? membership.get().getKingdomId() : null;
        if (!GrainTheft.isTheft(rank, breakerKingdomId, kingdom.get().getId(), player.isOp())) {
            return;
        }

        theftLog.record(kingdom.get().getId(), player.getUniqueId(), mcDayOf(block.getWorld()));
        PoliceResult result = justiceService.openFromActBreach(
                new ActBreach(kingdom.get().getId(), GrainTheft.BILL_ID, ConductKind.GRAIN_THEFT),
                player.getUniqueId());
        if (result instanceof PoliceResult.Success) {
            player.sendMessage(c("&cThat grain is the Crown's. A warrant application has been filed."));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.HAY_BLOCK) {
            return;
        }
        Optional<Kingdom> kingdom = granaryKingdomAt(block.getLocation());
        if (kingdom.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        ItemStack held = player.getInventory().getItemInMainHand();
        if (player.isSneaking() && held.getType() == Material.HAY_BLOCK) {
            contribute(player, kingdom.get(), block.getWorld(), membership, held, block.getLocation());
            return;
        }
        NobleRank rank = membership.isPresent() ? membership.get().getRank() : null;
        boolean crown = membership.isPresent()
                && kingdom.get().getId().equals(membership.get().getKingdomId())
                && (rank == NobleRank.KING || rank == NobleRank.QUEEN || rank == NobleRank.PRINCE);
        player.openInventory(GranaryStoresGui.create(
                        kingdom.get().getId(),
                        storesOf(kingdom.get(), block.getWorld()),
                        crown ? theftLog.entries(kingdom.get().getId()) : java.util.List.of(),
                        crown)
                .getInventory());
    }

    private void contribute(
            Player player,
            Kingdom kingdom,
            World world,
            Optional<PlayerMembership> membership,
            ItemStack held,
            Location at) {
        String memberKingdomId = membership.isPresent() ? membership.get().getKingdomId() : null;
        if (!kingdom.getId().equals(memberKingdomId)) {
            RealmFeedback.refuse(player, "Only this realm's members may fill its granary.");
            return;
        }
        Optional<GranaryBounds> bounds = BukkitGranaryScan.walkableBounds(world, kingdom.getGranaryRegion());
        if (bounds.isEmpty()) {
            RealmFeedback.refuse(player, "The granary cannot be read.");
            return;
        }
        GranaryStock stock = BukkitGranaryScan.stockIn(world, bounds.get());
        int heads = kingdom.getWorldGuardRegions().stream()
                .mapToInt(region -> BukkitTerritoryHeads.countIn(world, region))
                .sum();
        int ration = WinterRation.balesFor(heads, granaryConfig.headsPerHay());
        long realmDay = calendarService == null ? 0L : calendarService.currentRealmDay();
        Optional<GranaryContribution> contribution = GranaryContribution.plan(
                memberKingdomId,
                kingdom.getId(),
                stock,
                ration,
                WinterRation.winterDaysRemaining(realmDay));
        if (contribution.isEmpty() || BukkitGranaryYard.layBales(world, bounds.get(), 1) != 1) {
            RealmFeedback.refuse(player, "The granary is full. Build more room for grain.");
            return;
        }
        if (held.getAmount() == 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            held.setAmount(held.getAmount() - 1);
        }
        RealmFeedback.success(at);
        int shortfall = contribution.get().shortfallAfter();
        RealmFeedback.instruct(player, shortfall == 0
                ? "The realm is provisioned for winter."
                : shortfall + (shortfall == 1 ? " bale" : " bales") + " still needed for winter.");
        if (contribution.get().shortfallBefore() > 0 && shortfall == 0) {
            RealmFeedback.kingdomMessage(
                    kingdomService,
                    kingdom.getId(),
                    "&6" + player.getName() + " filled the final bale. The realm is provisioned for winter.");
        }
    }

    /** The kingdom whose granary the block stands in, where one is sited and WorldGuard places it. */
    private Optional<Kingdom> granaryKingdomAt(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return Optional.empty();
        }
        Optional<String> jurisdiction = territoryResolver.owningKingdomId(
                world.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (jurisdiction.isEmpty()) {
            return Optional.empty();
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(jurisdiction.get());
        if (kingdom.isEmpty()) {
            return Optional.empty();
        }
        String granaryRegion = kingdom.get().getGranaryRegion();
        if (granaryRegion == null || granaryRegion.isBlank()) {
            return Optional.empty();
        }
        Optional<GranaryBounds> bounds = BukkitGranaryScan.boundsOf(world.getName(), granaryRegion);
        if (bounds.isEmpty()
                || !bounds.get().contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
            return Optional.empty();
        }
        return kingdom;
    }

    /** The stores as they stand, every figure of them read afresh off the world and the ledgers. */
    private GranaryStoresGui.View storesOf(Kingdom kingdom, World world) {
        Optional<GranaryStock> stock = BukkitGranaryScan.stockOf(world.getName(), kingdom.getGranaryRegion());
        int stocked = stock.isPresent() ? stock.get().stock() : 0;
        int capacity = stock.isPresent() ? stock.get().capacity() : 0;
        int heads = kingdom.getWorldGuardRegions().stream()
                .mapToInt(region -> BukkitTerritoryHeads.countIn(world, region))
                .sum();
        int ration = WinterRation.balesFor(heads, granaryConfig.headsPerHay());
        long realmDay = calendarService == null ? 0L : calendarService.currentRealmDay();
        int farmers = countFarmers(world, kingdom);
        return new GranaryStoresGui.View(
                stocked,
                capacity,
                ration,
                WinterRation.daysCovered(stocked, ration),
                WinterRation.shortfall(stocked, ration, WinterRation.winterDaysRemaining(realmDay)),
                farmers,
                (int) Math.floor(farmers * granaryConfig.wheatPerFarmerDay() / granaryConfig.wheatPerBale()),
                countHunger(kingdom.getId(), false),
                countHunger(kingdom.getId(), true));
    }

    private int countFarmers(World world, Kingdom kingdom) {
        if (!kingdom.hasWorldGuardRegions()) {
            return 0;
        }
        int farmers = 0;
        for (org.bukkit.entity.Villager villager : world.getEntitiesByClass(org.bukkit.entity.Villager.class)) {
            Location at = villager.getLocation();
            Optional<String> owner =
                    territoryResolver.owningKingdomId(world.getName(), at.getBlockX(), at.getBlockY(), at.getBlockZ());
            if (owner.isPresent()
                    && owner.get().equals(kingdom.getId())
                    && villager.getProfession() == org.bukkit.entity.Villager.Profession.FARMER) {
                farmers++;
            }
        }
        return farmers;
    }

    /** How many of the realm's villagers are hungry at all, and how many long enough to be starving. */
    private int countHunger(String kingdomId, boolean starvingOnly) {
        HungerLedgerStore ledger = this.hungerLedger;
        if (ledger == null) {
            return 0;
        }
        Map<UUID, Integer> days = ledger.allView().get(kingdomId);
        if (days == null) {
            return 0;
        }
        int counted = 0;
        for (Integer hungryDays : days.values()) {
            if (hungryDays == null) {
                continue;
            }
            if (!starvingOnly || HungerRamp.strikes(hungryDays.intValue(), granaryConfig)) {
                counted++;
            }
        }
        return counted;
    }

    private static long mcDayOf(World world) {
        return world == null ? 0L : world.getFullTime() / 24000L;
    }
}
