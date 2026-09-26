package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.church.ChurchResult;
import dev.mrlemoos.kingdom.church.ChurchService;
import dev.mrlemoos.kingdom.church.ChurchSiting;
import dev.mrlemoos.kingdom.city.CapitalSiting;
import dev.mrlemoos.kingdom.city.CityResult;
import dev.mrlemoos.kingdom.city.CityService;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.economy.service.EconomyResult;
import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.foundation.FoundationStone;
import dev.mrlemoos.kingdom.foundation.FoundationStoneItem;
import dev.mrlemoos.kingdom.foundation.FoundationStoneLaying;
import dev.mrlemoos.kingdom.foundation.gui.MintClearPickerGui;
import dev.mrlemoos.kingdom.foundation.gui.NumberClearPickerGui;
import dev.mrlemoos.kingdom.foundation.gui.SiteClearConfirmGui;
import dev.mrlemoos.kingdom.granary.GranaryRegionSiting;
import dev.mrlemoos.kingdom.granary.GranarySiting;
import dev.mrlemoos.kingdom.mint.MintSiting;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.church.ChurchSite;
import dev.mrlemoos.kingdom.model.city.CapitalLocation;
import dev.mrlemoos.kingdom.model.election.MpSeatLocation;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.model.parliament.KingdomFlag;
import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.parliament.KingdomFlagItems;
import dev.mrlemoos.kingdom.parliament.KingdomFlagResolver;
import dev.mrlemoos.kingdom.parliament.ParliamentSiting;
import dev.mrlemoos.kingdom.police.CourtBench;
import dev.mrlemoos.kingdom.police.PoliceResult;
import dev.mrlemoos.kingdom.police.PoliceSiting;
import dev.mrlemoos.kingdom.service.ParliamentResult;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Foundation stones in the world: handed out from the Realm Hub, laid to raise a site, and the
 * Hub's confirmation before a site is cleared. The siting rules live in {@link FoundationStoneLaying}
 * and the site's own service; this only carries the stone between them.
 */
public final class FoundationStoneListener implements Listener {

    private final JavaPlugin plugin;
    private final KingdomService kingdomService;
    private final KingdomTerritoryResolver territoryResolver;
    private final ChurchService churchService;
    private final ChurchSiting churchSiting;
    private final CityService cityService;
    private final CapitalSiting capitalSiting;
    private final MintSiting mintSiting;
    private final ParliamentSiting parliamentSiting;
    private final PoliceSiting policeSiting;
    private final GranaryRegionSiting granarySiting;
    private final FoundationStoneItem stones;
    private BiConsumer<Player, FoundationStone> hubReturn;

    public FoundationStoneListener(
            JavaPlugin plugin,
            KingdomService kingdomService,
            KingdomTerritoryResolver territoryResolver,
            ChurchService churchService,
            ChurchSiting churchSiting,
            CityService cityService,
            CapitalSiting capitalSiting,
            MintSiting mintSiting,
            ParliamentSiting parliamentSiting,
            PoliceSiting policeSiting,
            GranaryRegionSiting granarySiting,
            FoundationStoneItem stones) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.territoryResolver = Objects.requireNonNull(territoryResolver, "territoryResolver");
        this.churchService = Objects.requireNonNull(churchService, "churchService");
        this.churchSiting = Objects.requireNonNull(churchSiting, "churchSiting");
        this.cityService = Objects.requireNonNull(cityService, "cityService");
        this.capitalSiting = Objects.requireNonNull(capitalSiting, "capitalSiting");
        this.mintSiting = Objects.requireNonNull(mintSiting, "mintSiting");
        this.parliamentSiting = Objects.requireNonNull(parliamentSiting, "parliamentSiting");
        this.policeSiting = Objects.requireNonNull(policeSiting, "policeSiting");
        this.granarySiting = Objects.requireNonNull(granarySiting, "granarySiting");
        this.stones = Objects.requireNonNull(stones, "stones");
    }

    /** Where Back on the clear confirmation leads: the Hub section the stone belongs to. */
    public FoundationStoneListener withHubReturn(BiConsumer<Player, FoundationStone> hubReturn) {
        this.hubReturn = hubReturn;
        return this;
    }

    // --- from the Hub -----------------------------------------------------

    /** Hands the stone for a site to whoever may lay it; a full inventory drops it at their feet. */
    public void giveStone(Player player, FoundationStone kind) {
        Optional<PlayerMembership> membership = memberOf(player);
        if (membership.isEmpty()) {
            return;
        }
        if (kind.topic().isEmpty()) {
            RealmFeedback.refuse(player, "That stone cannot be cut yet.");
            return;
        }
        if (!kind.mayLay(membership.get().getRank())) {
            RealmFeedback.refuse(player, "Only " + kind.layers() + " may lay " + kind.site() + "'s foundation stone.");
            return;
        }
        String kingdomId = membership.get().getKingdomId();
        if (kind == FoundationStone.MP_SEAT && parliamentSiting.nextEmptySeat(kingdomId).isEmpty()) {
            RealmFeedback.refuse(player, ParliamentSiting.fullHouse());
            return;
        }
        player.closeInventory();
        ItemStack stone;
        if (kind == FoundationStone.LORDS) {
            Optional<KingdomFlag> held = heldBanner(player);
            Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
            stone = stones.createLords(
                    kingdomId,
                    KingdomFlagResolver.resolve(kingdom.isPresent() ? kingdom.get().getFlag() : Optional.empty(), held));
            if (held.isPresent()) {
                player.sendMessage(c("&7The stone bears the design of the banner in your hand."));
            }
        } else {
            stone = stones.create(kind, kingdomId);
        }
        for (ItemStack overflow : player.getInventory().addItem(stone).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflow);
        }
        player.sendMessage(c("&7You take " + kind.site() + "'s foundation stone."));
        RealmFeedback.instruct(player, instruction(kind));
    }

    /** The design of a plain banner in either hand; a foundation stone is not a design to copy. */
    private Optional<KingdomFlag> heldBanner(Player player) {
        for (ItemStack hand : List.of(player.getInventory().getItemInMainHand(), player.getInventory().getItemInOffHand())) {
            if (stones.kind(hand).isPresent()) {
                continue;
            }
            Optional<KingdomFlag> design = KingdomFlagItems.fromItem(hand);
            if (design.isPresent()) {
                return design;
            }
        }
        return Optional.empty();
    }

    /** Offers to clear a site, behind a confirmation; a mint is picked from the realm's mints first. */
    public void offerClear(Player player, FoundationStone kind) {
        Optional<PlayerMembership> membership = memberOf(player);
        if (membership.isEmpty()) {
            return;
        }
        if (!kind.mayClear(membership.get().getRank())) {
            RealmFeedback.refuse(player, "Only the King or Queen may clear " + kind.site() + ".");
            return;
        }
        String kingdomId = membership.get().getKingdomId();
        if (kind == FoundationStone.MINT) {
            List<MintLocation> mints = mintSiting.mints(kingdomId);
            if (mints.isEmpty()) {
                RealmFeedback.refuse(player, "Your realm has no mint to clear.");
            } else if (mints.size() == 1) {
                open(player, SiteClearConfirmGui.forMint(kingdomId, mints.get(0)).getInventory());
            } else {
                open(player, MintClearPickerGui.create(kingdomId, mints).getInventory());
            }
            return;
        }
        if (kind == FoundationStone.MP_SEAT || kind == FoundationStone.CELL) {
            List<Integer> numbers = kind == FoundationStone.CELL
                    ? policeSiting.cells(kingdomId)
                    : parliamentSiting.setSeats(kingdomId);
            if (numbers.isEmpty()) {
                RealmFeedback.refuse(player, kind == FoundationStone.CELL ? "No cell is set." : "No MP seat is set.");
            } else if (numbers.size() == 1) {
                open(player, SiteClearConfirmGui.forNumber(kind, kingdomId, numbers.get(0)).getInventory());
            } else {
                open(player, NumberClearPickerGui.create(kind, kingdomId, numbers).getInventory());
            }
            return;
        }
        Optional<String> unclearable = unclearable(kind, kingdomId);
        if (unclearable.isPresent()) {
            RealmFeedback.refuse(player, unclearable.get());
            return;
        }
        open(player, SiteClearConfirmGui.create(kind, kingdomId).getInventory());
    }

    /** Why a site cannot be cleared from the Hub; empty when it can. */
    private Optional<String> unclearable(FoundationStone kind, String kingdomId) {
        return switch (kind) {
            case CHURCH -> churchService.church(kingdomId).isEmpty()
                    ? Optional.of("The church is not yet sited.")
                    : Optional.empty();
            case CAPITAL -> cityService.capital(kingdomId).isEmpty()
                    ? Optional.of("The capital is not yet sited.")
                    : Optional.empty();
            case TOWN_CRIER -> {
                Optional<CapitalLocation> stand = cityService.crierStand(kingdomId);
                if (stand.isEmpty()) {
                    yield Optional.of("The Town Crier is not yet sited.");
                }
                yield stand.equals(cityService.capital(kingdomId))
                        ? Optional.of("The Town Crier already cries at the city hall.")
                        : Optional.empty();
            }
            case COMMONS, LORDS, SPEAKER_CHAIR, BAR, REGISTRAR -> parliamentSiting.where(kingdomId, kind).isEmpty()
                    ? Optional.of(capitalise(kind.site()) + " is not yet sited.")
                    : Optional.empty();
            case COURT -> policeSiting.court(kingdomId).isEmpty()
                    ? Optional.of("The court is not yet sited.")
                    : Optional.empty();
            case GRANARY -> granarySiting.region(kingdomId).isEmpty()
                    ? Optional.of("The granary is not yet sited.")
                    : Optional.empty();
            default -> Optional.of(capitalise(kind.site()) + " cannot be cleared from the Hub yet.");
        };
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNumberPickerClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof NumberClearPickerGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (event.getSlot() == gui.backSlot()) {
            backToHub(player, gui.kind());
            return;
        }
        OptionalInt number = gui.numberAt(event.getSlot());
        if (number.isPresent()) {
            open(player, SiteClearConfirmGui.forNumber(gui.kind(), gui.kingdomId(), number.getAsInt()).getInventory());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickerClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MintClearPickerGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (event.getSlot() == MintClearPickerGui.SLOT_BACK) {
            backToHub(player, FoundationStone.MINT);
            return;
        }
        Optional<MintLocation> mint = gui.mintAt(event.getSlot());
        if (mint.isPresent()) {
            open(player, SiteClearConfirmGui.forMint(gui.kingdomId(), mint.get()).getInventory());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConfirmClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SiteClearConfirmGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (event.getSlot() == SiteClearConfirmGui.SLOT_BACK) {
            backToHub(player, gui.kind());
            return;
        }
        if (event.getSlot() != SiteClearConfirmGui.SLOT_CONFIRM) {
            return;
        }
        player.closeInventory();
        Optional<PlayerMembership> membership = memberOf(player);
        if (membership.isEmpty() || !membership.get().getKingdomId().equals(gui.kingdomId())) {
            return;
        }
        if (!gui.kind().mayClear(membership.get().getRank())) {
            RealmFeedback.refuse(player, "Only the King or Queen may clear " + gui.kind().site() + ".");
            return;
        }
        Cleared cleared = clear(gui, membership.get().getRank());
        if (cleared.refusal().isPresent()) {
            RealmFeedback.refuse(player, cleared.refusal().get());
            return;
        }
        player.sendMessage(c("&a" + cleared.record()));
        RealmFeedback.success(cleared.at());
    }

    /** What came of clearing a site: the refusal, or the chat record and where it happened. */
    private record Cleared(Optional<String> refusal, String record, Location at) {
        static Cleared refused(String refusal) {
            return new Cleared(Optional.of(refusal), "", null);
        }
    }

    private Cleared clear(SiteClearConfirmGui gui, NobleRank rank) {
        String kingdomId = gui.kingdomId();
        switch (gui.kind()) {
            case CHURCH -> {
                Optional<ChurchSite> standing = churchService.church(kingdomId);
                ChurchResult result = churchSiting.clear(kingdomId, rank);
                if (result instanceof ChurchResult.Failure failure) {
                    return Cleared.refused(failure.message());
                }
                return new Cleared(
                        Optional.empty(),
                        result.message(),
                        standing.isPresent() ? location(standing.get().worldName(), standing.get().x(),
                                standing.get().y(), standing.get().z()) : null);
            }
            case CAPITAL -> {
                Optional<CapitalLocation> standing = cityService.capital(kingdomId);
                CityResult result = capitalSiting.clear(kingdomId, rank);
                if (result instanceof CityResult.Failure failure) {
                    return Cleared.refused(failure.message());
                }
                // The war region follows the capital.
                capitalSiting.releaseWarRegion(kingdomId);
                return new Cleared(Optional.empty(), result.message(), location(standing));
            }
            case TOWN_CRIER -> {
                CapitalSiting.Raised raised = capitalSiting.returnCrier(kingdomId, rank);
                if (raised.result() instanceof CityResult.Failure failure) {
                    return Cleared.refused(failure.message());
                }
                return new Cleared(Optional.empty(), raised.result().message(), location(cityService.capital(kingdomId)));
            }
            case MINT -> {
                Optional<MintLocation> mint = gui.mint();
                if (mint.isEmpty() || !mintSiting.remove(kingdomId, mint.get())) {
                    return Cleared.refused("That mint no longer stands.");
                }
                MintLocation gone = mint.get();
                return new Cleared(
                        Optional.empty(),
                        "The mint at " + gone.x() + ", " + gone.y() + ", " + gone.z() + " has been taken down.",
                        location(gone.worldName(), gone.x() + 0.5, gone.y(), gone.z() + 0.5));
            }
            case MP_SEAT -> {
                OptionalInt seat = gui.number();
                if (seat.isEmpty()) {
                    return Cleared.refused("That seat is not set.");
                }
                Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
                Optional<MpSeatLocation> standing = kingdom.isPresent()
                        ? kingdom.get().getElectionState().seatLocation(seat.getAsInt())
                        : Optional.empty();
                ParliamentResult result = parliamentSiting.clearMpSeat(kingdomId, seat.getAsInt());
                if (result instanceof ParliamentResult.Failure failure) {
                    return Cleared.refused(failure.message());
                }
                return new Cleared(
                        Optional.empty(),
                        ((ParliamentResult.Success) result).message(),
                        standing.isPresent()
                                ? location(standing.get().worldName(), standing.get().x(), standing.get().y(), standing.get().z())
                                : null);
            }
            case COMMONS, LORDS, SPEAKER_CHAIR, BAR, REGISTRAR -> {
                Optional<ParliamentSiting.Where> standing = parliamentSiting.where(kingdomId, gui.kind());
                ParliamentResult result = parliamentSiting.clear(kingdomId, gui.kind());
                if (result instanceof ParliamentResult.Failure failure) {
                    return Cleared.refused(failure.message());
                }
                return new Cleared(
                        Optional.empty(),
                        ((ParliamentResult.Success) result).message(),
                        standing.isPresent()
                                ? location(standing.get().worldName(), standing.get().x(), standing.get().y(), standing.get().z())
                                : null);
            }
            case COURT -> {
                Optional<CourtLocation> standing = policeSiting.court(kingdomId);
                PoliceResult result = policeSiting.clearCourt(kingdomId, rank, false);
                if (result instanceof PoliceResult.Failure failure) {
                    return Cleared.refused(failure.message());
                }
                return new Cleared(
                        Optional.empty(),
                        ((PoliceResult.Success) result).message() + " The judge rises; the lectern stays where it stands.",
                        standing.isPresent()
                                ? location(standing.get().worldName(), standing.get().x() + 0.5, standing.get().y(),
                                        standing.get().z() + 0.5)
                                : null);
            }
            case CELL -> {
                OptionalInt cell = gui.number();
                if (cell.isEmpty()) {
                    return Cleared.refused("That cell is not set.");
                }
                Optional<PrisonCellLocation> standing = policeSiting.cell(kingdomId, cell.getAsInt());
                PoliceResult result = policeSiting.clearCell(kingdomId, rank, false, cell.getAsInt());
                if (result instanceof PoliceResult.Failure failure) {
                    return Cleared.refused(failure.message());
                }
                return new Cleared(
                        Optional.empty(),
                        ((PoliceResult.Success) result).message(),
                        standing.isPresent()
                                ? location(standing.get().worldName(), standing.get().x() + 0.5, standing.get().y(),
                                        standing.get().z() + 0.5)
                                : null);
            }
            case GRANARY -> {
                Optional<String> region = granarySiting.region(kingdomId);
                GranarySiting.Verdict verdict = granarySiting.release(kingdomId, rank);
                if (verdict != GranarySiting.Verdict.ALLOWED) {
                    return Cleared.refused(GranarySiting.refusalMessage(verdict));
                }
                return new Cleared(
                        Optional.empty(),
                        "The granary" + (region.isPresent() ? " in region " + region.get() : "")
                                + " is released; the hay stays where it stands.",
                        null);
            }
            default -> {
                return Cleared.refused(capitalise(gui.kind().site()) + " cannot be cleared from the Hub yet.");
            }
        }
    }

    private void backToHub(Player player, FoundationStone kind) {
        player.closeInventory();
        if (hubReturn != null) {
            hubReturn.accept(player, kind);
        }
    }

    private static void open(Player player, Inventory inventory) {
        player.openInventory(Objects.requireNonNull(inventory));
    }

    @EventHandler
    public void onConfirmDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SiteClearConfirmGui
                || event.getInventory().getHolder() instanceof MintClearPickerGui
                || event.getInventory().getHolder() instanceof NumberClearPickerGui) {
            event.setCancelled(true);
        }
    }

    // --- in the hand, and laid -------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onHold(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Optional<FoundationStone> kind = stones.kind(player.getInventory().getItem(event.getNewSlot()));
        if (kind.isPresent()) {
            RealmFeedback.instruct(player, instruction(kind.get()));
        }
    }

    /** Laid first, before build permits or conduct have their say: the stone is never a block. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLay(BlockPlaceEvent event) {
        Optional<FoundationStone> kind = stones.kind(event.getItemInHand());
        if (kind.isEmpty()) {
            return;
        }
        // The registrar's bookshelf and the court's lectern stay where they are put; no other stone is a block.
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (kind.get().topic().isEmpty()) {
            RealmFeedback.refuse(player, "That stone cannot be laid yet.");
            return;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        Block block = event.getBlockPlaced();
        String worldName = block.getWorld().getName();
        FoundationStoneLaying laying = FoundationStoneLaying.judge(
                kind.get(),
                membership.isPresent() ? membership.get().getRank() : null,
                membership.isPresent() ? membership.get().getKingdomId() : null,
                stones.kingdomId(event.getItemInHand()).orElse(null),
                territoryResolver.owningKingdomId(worldName, block.getX(), block.getY(), block.getZ()));
        if (!laying.holds() || membership.isEmpty()) {
            RealmFeedback.refuse(player, laying.refusal());
            return;
        }
        if (kind.get().staysWhereLaid()) {
            // Let the block be placed; it is sited at MONITOR once every other rule has had its say.
            event.setCancelled(false);
            return;
        }
        String kingdomId = membership.get().getKingdomId();
        Raising raising = raise(kind.get(), kingdomId, membership.get().getRank(), block, player, event.getItemInHand());
        laying = laying.settle(raising.refusal());
        if (!laying.stoneSpent()) {
            RealmFeedback.refuse(player, laying.refusal());
            return;
        }
        spendOne(player, event.getHand());
        announce(player, kind.get(), kingdomId, block, raising);
    }

    /**
     * A stone that stays — the registrar's bookshelf, the court's lectern — is sited only once the block has truly been
     * placed; a refusal here cancels the placement, and the bookshelf goes back into the hand.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaidToStay(BlockPlaceEvent event) {
        Optional<FoundationStone> kind = stones.kind(event.getItemInHand());
        if (kind.isEmpty() || !kind.get().staysWhereLaid()) {
            return;
        }
        Player player = event.getPlayer();
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            event.setCancelled(true);
            RealmFeedback.refuse(player, "You must join a kingdom first.");
            return;
        }
        String kingdomId = membership.get().getKingdomId();
        Block block = event.getBlockPlaced();
        Raising raising = raise(kind.get(), kingdomId, membership.get().getRank(), block, player, event.getItemInHand());
        if (raising.refusal().isPresent()) {
            event.setCancelled(true);
            RealmFeedback.refuse(player, raising.refusal().get());
            return;
        }
        announce(player, kind.get(), kingdomId, block, raising);
    }

    /** Success heard and seen at the spot, a title for the one who laid it, and the chat record. */
    private void announce(Player player, FoundationStone kind, String kingdomId, Block block, Raising raising) {
        RealmFeedback.success(block.getLocation().add(0.5, 0.5, 0.5));
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        String realm = kingdom.isPresent() ? kingdom.get().getDisplayName() : kingdomId;
        if (kind == FoundationStone.CAPITAL) {
            // The founding of a capital is titled to the whole realm.
            RealmFeedback.realmMilestone(kingdomService, kingdomId, "&6" + kind.title(), "&efounded in " + realm);
            RealmFeedback.kingdomMessage(
                    kingdomService, kingdomId, "&6The capital of " + realm + " has been founded by " + player.getName() + ".");
        } else {
            RealmFeedback.milestone(
                    List.of(player), "&6" + raising.heading().orElse(kind.title()), "&efounded in " + realm);
        }
        for (String line : raising.record()) {
            player.sendMessage(c(line));
        }
    }

    /** What came of raising a site: the site's own refusal, or the chat record of what now stands. */
    private record Raising(Optional<String> refusal, List<String> record, Optional<String> heading) {
        Raising(Optional<String> refusal, List<String> record) {
            this(refusal, record, Optional.empty());
        }

        static Raising refused(String refusal) {
            return new Raising(Optional.of(refusal), List.of());
        }

        /** A point of Parliament set, or the House's refusal. */
        static Raising of(ParliamentResult result, String... more) {
            if (result instanceof ParliamentResult.Failure failure) {
                return refused(failure.message());
            }
            List<String> record = new ArrayList<>();
            record.add("&a" + ((ParliamentResult.Success) result).message());
            record.addAll(List.of(more));
            return new Raising(Optional.empty(), record);
        }
    }

    /** Raises the site on the stone's block; its NPC stands there and turns to face the one who laid it. */
    private Raising raise(
            FoundationStone kind, String kingdomId, NobleRank rank, Block block, Player player, ItemStack stone) {
        String worldName = block.getWorld().getName();
        float facing = player.getLocation().getYaw() + 180f;
        ChamberSite point = ChamberSite.of(worldName, block.getX() + 0.5, block.getY(), block.getZ() + 0.5);
        return switch (kind) {
            case COMMONS -> Raising.of(parliamentSiting.setCommons(kingdomId, point));
            case SPEAKER_CHAIR -> Raising.of(
                    parliamentSiting.setSpeakerChair(kingdomId, point),
                    "&7The villager Speaker presides from here when no player holds the Chair.");
            case BAR -> Raising.of(parliamentSiting.setBar(kingdomId, point));
            case LORDS -> {
                ParliamentSiting.Lords lords =
                        parliamentSiting.setLords(kingdomId, point, KingdomFlagItems.fromItem(stone));
                yield Raising.of(
                        lords.result(),
                        lords.flagFlies()
                                ? "&7The kingdom flag flies a block east of the Lords."
                                : "&cThe kingdom flag could not be raised; clear the block east of the Lords.");
            }
            case MP_SEAT -> {
                OptionalInt seat = parliamentSiting.nextEmptySeat(kingdomId);
                if (seat.isEmpty()) {
                    yield Raising.refused(ParliamentSiting.fullHouse());
                }
                Raising raising = Raising.of(parliamentSiting.setMpSeat(
                        kingdomId,
                        seat.getAsInt(),
                        new MpSeatLocation(worldName, point.x(), point.y(), point.z(), facing, 0f)));
                yield raising.refusal().isPresent()
                        ? raising
                        : new Raising(Optional.empty(), raising.record(), Optional.of("MP Seat " + seat.getAsInt()));
            }
            case REGISTRAR -> Raising.of(
                    parliamentSiting.setRegistrar(
                            kingdomId, RegistrarSite.of(worldName, block.getX(), block.getY(), block.getZ())),
                    "&7Acts and Hansard are shelved here and on the chiseled bookshelves joined to it.");
            case COURT -> seatCourt(kingdomId, rank, block);
            case CELL -> {
                OptionalInt cell = policeSiting.nextFreeCell(kingdomId);
                if (cell.isEmpty()) {
                    yield Raising.refused("No cell number is free.");
                }
                PoliceResult result = policeSiting.setCell(
                        kingdomId,
                        rank,
                        false,
                        cell.getAsInt(),
                        new PrisonCellLocation(worldName, block.getX(), block.getY(), block.getZ()));
                if (result instanceof PoliceResult.Failure failure) {
                    yield Raising.refused(failure.message());
                }
                yield new Raising(
                        Optional.empty(),
                        List.of(
                                "&a" + ((PoliceResult.Success) result).message(),
                                "&7A prisoner sentenced to cell " + cell.getAsInt() + " stands where the stone was laid."),
                        Optional.of("Cell " + cell.getAsInt()));
            }
            case GRANARY -> {
                GranarySiting.Stone laid =
                        granarySiting.linkAround(kingdomId, rank, worldName, block.getX(), block.getY(), block.getZ());
                if (laid.regionId().isEmpty()) {
                    yield Raising.refused(GranarySiting.refusalMessage(laid.verdict()));
                }
                yield new Raising(
                        Optional.empty(),
                        List.of(
                                "&aRegion " + laid.regionId().get() + " is linked as the granary.",
                                "&7Hay stood in it through the growing year feeds the realm's villagers through winter."));
            }
            case CHURCH -> {
                ChurchSite site = ChurchSite.of(worldName, block.getX() + 0.5, block.getY(), block.getZ() + 0.5, facing, 0f);
                ChurchResult result = churchSiting.site(kingdomId, rank, site);
                yield result instanceof ChurchResult.Failure failure
                        ? Raising.refused(failure.message())
                        : new Raising(Optional.empty(), List.of("&a" + result.message()));
            }
            case CAPITAL -> {
                CapitalLocation capital = CapitalLocation.of(
                        worldName, block.getX() + 0.5, block.getY(), block.getZ() + 0.5, facing, 0f);
                CapitalSiting.Raised raised = capitalSiting.site(kingdomId, rank, capital);
                if (raised.result() instanceof CityResult.Failure failure) {
                    yield Raising.refused(failure.message());
                }
                List<String> record = new ArrayList<>();
                record.add("&a" + raised.result().message());
                record.add(raised.mayorStanding()
                        ? "&7The Lord Mayor has taken up office at the city hall."
                        : "&cThe Lord Mayor could not be stood up; the capital's world is not loaded.");
                record.add(raised.crierStanding()
                        ? "&7The Town Crier has taken up the Gazette."
                        : "&cThe Town Crier could not be stood up; the capital's world is not loaded.");
                Optional<String> region =
                        capitalSiting.linkWarRegion(kingdomId, worldName, block.getX(), block.getY(), block.getZ());
                record.add(region.isPresent()
                        ? "&7Region " + region.get() + " is linked as the capital for capital-fall war aims."
                        : "&7No region lies around the stone inside your territory; no capital-fall region is linked.");
                yield new Raising(Optional.empty(), record);
            }
            case TOWN_CRIER -> {
                CapitalLocation stand = CapitalLocation.of(
                        worldName, block.getX() + 0.5, block.getY(), block.getZ() + 0.5, facing, 0f);
                CapitalSiting.Raised raised = capitalSiting.standCrier(kingdomId, rank, stand);
                if (raised.result() instanceof CityResult.Failure failure) {
                    yield Raising.refused(failure.message());
                }
                yield new Raising(
                        Optional.empty(),
                        List.of(
                                "&a" + raised.result().message(),
                                raised.crierStanding()
                                        ? "&7The Town Crier now cries from here."
                                        : "&cThe Town Crier could not be stood up; this world's chunks are not loaded."));
            }
            case MINT -> {
                MintLocation location =
                        new MintLocation(worldName, block.getX(), block.getY(), block.getZ(), facing, null);
                int maxMints = plugin.getConfig().getInt("economy.max-mints-per-kingdom", 3);
                MintSiting.Placed placed = mintSiting.place(kingdomId, location, maxMints);
                if (placed.result() instanceof EconomyResult.Failure failure) {
                    yield Raising.refused(failure.message());
                }
                if (placed.mint().isEmpty()) {
                    yield Raising.refused("The mint could not be placed.");
                }
                MintLocation withLord = placed.mint().get();
                yield new Raising(
                        Optional.empty(),
                        List.of(
                                "&aThe mint is raised.",
                                "&7Lord of the Treasury stationed at "
                                        + withLord.x() + ", " + withLord.y() + ", " + withLord.z() + "."));
            }
            default -> Raising.refused("That stone cannot be laid yet.");
        };
    }

    /**
     * The court's lectern is laid; the judge sits behind it, on the side away from the one who laid it,
     * looking across the lectern at them. The seat must be clear and inside the realm's territory.
     */
    private Raising seatCourt(String kingdomId, NobleRank rank, Block lectern) {
        BlockFace facing = lectern.getBlockData() instanceof Directional directional
                ? directional.getFacing()
                : BlockFace.SOUTH;
        Block seat = lectern.getRelative(facing.getOppositeFace());
        if (!seat.isPassable() || !seat.getRelative(BlockFace.UP).isPassable()) {
            return Raising.refused("There is no room behind the lectern for the judge to sit.");
        }
        Optional<String> owner = territoryResolver.owningKingdomId(
                seat.getWorld().getName(), seat.getX(), seat.getY(), seat.getZ());
        if (owner.isEmpty() || !owner.get().equals(kingdomId)) {
            return Raising.refused("The judge's seat behind the lectern must be inside your kingdom's territory.");
        }
        CourtLocation court = new CourtLocation(
                seat.getWorld().getName(),
                seat.getX(),
                seat.getY(),
                seat.getZ(),
                CourtBench.yawTowards(0, 0, facing.getModX(), facing.getModZ()));
        PoliceResult result = policeSiting.siteCourt(kingdomId, rank, false, court);
        if (result instanceof PoliceResult.Failure failure) {
            return Raising.refused(failure.message());
        }
        return new Raising(
                Optional.empty(),
                List.of(
                        "&a" + ((PoliceResult.Success) result).message(),
                        "&7The judge sits behind the lectern; the dock stands before it."));
    }

    /**
     * The placement was cancelled, so the server gives the item back this tick; take the stone a tick
     * later, and only if it is still a stone in that hand.
     */
    private void spendOne(Player player, EquipmentSlot hand) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack held = player.getInventory().getItem(hand);
            if (stones.kind(held).isEmpty()) {
                return;
            }
            int left = held.getAmount() - 1;
            if (left <= 0) {
                player.getInventory().setItem(hand, null);
            } else {
                held.setAmount(left);
                player.getInventory().setItem(hand, held);
            }
        });
    }

    // --- the plumbing -----------------------------------------------------

    private Optional<PlayerMembership> memberOf(Player player) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            RealmFeedback.refuse(player, "You must join a kingdom first.");
        }
        return membership;
    }

    private static String instruction(FoundationStone kind) {
        return switch (kind) {
            case MP_SEAT -> "Lay the stone inside your realm's territory to set the next empty MP seat there.";
            case LORDS -> "Lay the banner inside your realm's territory to raise the House of Lords; the flag flies a block east.";
            case REGISTRAR -> "Lay the bookshelf inside your realm's territory; it stays, and becomes the registrar.";
            case COURT -> "Lay the lectern facing you inside your realm's territory; it stays, and the judge sits behind it.";
            case CELL -> "Lay the stone where a prisoner should stand to set the next free cell there.";
            case GRANARY -> "Lay the hay bale inside a WorldGuard region in your territory; that region becomes the granary.";
            default -> "Lay the stone inside your realm's territory to raise " + kind.site() + " there.";
        };
    }

    private static Location location(Optional<CapitalLocation> point) {
        return point.isPresent()
                ? location(point.get().worldName(), point.get().x(), point.get().y(), point.get().z())
                : null;
    }

    private static Location location(String worldName, double x, double y, double z) {
        World world = Bukkit.getWorld(worldName);
        return world == null ? null : new Location(world, x, y + 0.5, z);
    }

    private static String capitalise(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
