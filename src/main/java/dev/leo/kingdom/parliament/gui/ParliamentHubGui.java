package dev.leo.kingdom.parliament.gui;

import dev.leo.kingdom.helpers.ItemBuilder;
import java.util.OptionalInt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class ParliamentHubGui implements InventoryHolder {

    public static final String TITLE = ChatColor.DARK_GREEN + "Parliament";

    static final int SLOT_BILL_INFO = 4;

    static final int SLOT_TABLE_FISCAL = 19;
    static final int SLOT_TABLE_BUDGET = 20;
    static final int SLOT_TABLE_SPEND_MINT = 21;
    static final int SLOT_TABLE_SPEND_STIPEND = 22;
    static final int SLOT_STIPEND_OTHER = 23;

    static final int SLOT_BUDGET_50 = 28;
    static final int SLOT_BUDGET_100 = 29;
    static final int SLOT_BUDGET_250 = 30;
    static final int SLOT_BUDGET_500 = 31;
    static final int SLOT_CUSTOM_AMOUNT = 33;

    static final int SLOT_OPEN_DIVISION = 37;
    static final int SLOT_CLOSE_DIVISION = 38;
    static final int SLOT_CAST_AYE = 39;
    static final int SLOT_CAST_NAY = 40;

    static final int SLOT_VOTE_AYE = 46;
    static final int SLOT_VOTE_NAY = 47;
    static final int SLOT_VOTE_ABSTAIN = 48;

    static final int SLOT_ASSENT = 41;
    static final int SLOT_REJECT = 42;
    static final int SLOT_REVIEW_RESIGNATION = 43;

    private static final int[] BUDGET_PRESET_SLOTS = {SLOT_BUDGET_50, SLOT_BUDGET_100, SLOT_BUDGET_250, SLOT_BUDGET_500};
    private static final int[] BUDGET_PRESET_AMOUNTS = {50, 100, 250, 500};

    private final String kingdomId;
    private final ParliamentHubView view;
    private Inventory inventory;

    public ParliamentHubGui(String kingdomId, ParliamentHubView view) {
        this.kingdomId = kingdomId;
        this.view = view;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public ParliamentHubView view() {
        return view;
    }

    public static ParliamentHubGui create(String kingdomId, ParliamentHubView view) {
        ParliamentHubGui gui = new ParliamentHubGui(kingdomId, view);
        Inventory inventory = Bukkit.createInventory(gui, 54, TITLE);
        gui.inventory = inventory;
        populate(inventory, view);
        return gui;
    }

    static void populate(Inventory inventory, ParliamentHubView view) {
        inventory.clear();

        view.billTitle().ifPresent(title -> inventory.setItem(SLOT_BILL_INFO, billInfoItem(title, view.billState())));

        placeIfVisible(inventory, view, ParliamentHubAction.TABLE_FISCAL, SLOT_TABLE_FISCAL, fiscalItem(view));
        placeIfVisible(inventory, view, ParliamentHubAction.TABLE_BUDGET, SLOT_TABLE_BUDGET, budgetItem(view));
        placeIfVisible(inventory, view, ParliamentHubAction.TABLE_SPEND_MINT, SLOT_TABLE_SPEND_MINT, spendMintItem(view));
        placeIfVisible(
                inventory, view, ParliamentHubAction.TABLE_SPEND_STIPEND, SLOT_TABLE_SPEND_STIPEND, spendStipendItem(view));
        placeIfVisible(inventory, view, ParliamentHubAction.STIPEND_OTHER, SLOT_STIPEND_OTHER, stipendOtherItem(view));

        for (int i = 0; i < BUDGET_PRESET_SLOTS.length; i++) {
            int slot = BUDGET_PRESET_SLOTS[i];
            int amount = BUDGET_PRESET_AMOUNTS[i];
            if (view.visibleActions().contains(ParliamentHubAction.BUDGET_PRESET)) {
                inventory.setItem(slot, budgetPresetItem(amount, view.isEnabled(ParliamentHubAction.BUDGET_PRESET)));
            }
        }
        placeIfVisible(
                inventory,
                view,
                ParliamentHubAction.CUSTOM_AMOUNT,
                SLOT_CUSTOM_AMOUNT,
                ItemBuilder.labelled(Material.PAPER, ChatColor.AQUA + "Custom budget", "Enter an amount in chat"));

        placeIfVisible(
                inventory,
                view,
                ParliamentHubAction.OPEN_DIVISION,
                SLOT_OPEN_DIVISION,
                ItemBuilder.labelled(Material.LIME_BANNER, ChatColor.GREEN + "Open division", "Begin a Commons vote"));
        placeIfVisible(
                inventory,
                view,
                ParliamentHubAction.CLOSE_DIVISION,
                SLOT_CLOSE_DIVISION,
                ItemBuilder.labelled(
                        Material.RED_BANNER,
                        enabledColour(view.isEnabled(ParliamentHubAction.CLOSE_DIVISION)) + "Close division",
                        view.closeDivisionBlocked()
                                ? "Casting vote required — division is tied"
                                : "End the division and tally votes"));
        placeIfVisible(
                inventory,
                view,
                ParliamentHubAction.CAST_AYE,
                SLOT_CAST_AYE,
                ItemBuilder.labelled(Material.LIME_WOOL, ChatColor.GREEN + "Casting vote: Aye", "Break a tied division"));
        placeIfVisible(
                inventory,
                view,
                ParliamentHubAction.CAST_NAY,
                SLOT_CAST_NAY,
                ItemBuilder.labelled(Material.RED_WOOL, ChatColor.RED + "Casting vote: Nay", "Break a tied division"));

        placeIfVisible(
                inventory,
                view,
                ParliamentHubAction.VOTE_AYE,
                SLOT_VOTE_AYE,
                ItemBuilder.labelled(Material.LIME_CONCRETE, ChatColor.GREEN + "Vote Aye", "Support the bill"));
        placeIfVisible(
                inventory,
                view,
                ParliamentHubAction.VOTE_NAY,
                SLOT_VOTE_NAY,
                ItemBuilder.labelled(Material.RED_CONCRETE, ChatColor.RED + "Vote Nay", "Oppose the bill"));
        placeIfVisible(
                inventory,
                view,
                ParliamentHubAction.VOTE_ABSTAIN,
                SLOT_VOTE_ABSTAIN,
                ItemBuilder.labelled(Material.YELLOW_CONCRETE, ChatColor.YELLOW + "Abstain", "Record no vote"));

        placeIfVisible(
                inventory,
                view,
                ParliamentHubAction.ASSENT,
                SLOT_ASSENT,
                ItemBuilder.labelled(Material.EMERALD_BLOCK, ChatColor.GOLD + "Grant royal assent", "Enact the bill"));
        placeIfVisible(
                inventory,
                view,
                ParliamentHubAction.REJECT,
                SLOT_REJECT,
                ItemBuilder.labelled(Material.BARRIER, ChatColor.RED + "Withhold assent", "Reject the bill"));
        view.resignationSummary()
                .ifPresent(summary -> placeIfVisible(
                        inventory,
                        view,
                        ParliamentHubAction.REVIEW_RESIGNATION,
                        SLOT_REVIEW_RESIGNATION,
                        ItemBuilder.labelled(
                                Material.WRITABLE_BOOK,
                                ChatColor.DARK_RED + "Review resignation",
                                summary)));

        fillBackground(inventory);
    }

    public ParliamentHubAction actionForSlot(int slot) {
        return switch (slot) {
            case SLOT_TABLE_FISCAL -> ParliamentHubAction.TABLE_FISCAL;
            case SLOT_TABLE_BUDGET -> ParliamentHubAction.TABLE_BUDGET;
            case SLOT_TABLE_SPEND_MINT -> ParliamentHubAction.TABLE_SPEND_MINT;
            case SLOT_TABLE_SPEND_STIPEND -> ParliamentHubAction.TABLE_SPEND_STIPEND;
            case SLOT_STIPEND_OTHER -> ParliamentHubAction.STIPEND_OTHER;
            case SLOT_BUDGET_50, SLOT_BUDGET_100, SLOT_BUDGET_250, SLOT_BUDGET_500 -> ParliamentHubAction.BUDGET_PRESET;
            case SLOT_CUSTOM_AMOUNT -> ParliamentHubAction.CUSTOM_AMOUNT;
            case SLOT_OPEN_DIVISION -> ParliamentHubAction.OPEN_DIVISION;
            case SLOT_CLOSE_DIVISION -> ParliamentHubAction.CLOSE_DIVISION;
            case SLOT_CAST_AYE -> ParliamentHubAction.CAST_AYE;
            case SLOT_CAST_NAY -> ParliamentHubAction.CAST_NAY;
            case SLOT_VOTE_AYE -> ParliamentHubAction.VOTE_AYE;
            case SLOT_VOTE_NAY -> ParliamentHubAction.VOTE_NAY;
            case SLOT_VOTE_ABSTAIN -> ParliamentHubAction.VOTE_ABSTAIN;
            case SLOT_ASSENT -> ParliamentHubAction.ASSENT;
            case SLOT_REJECT -> ParliamentHubAction.REJECT;
            case SLOT_REVIEW_RESIGNATION -> ParliamentHubAction.REVIEW_RESIGNATION;
            default -> null;
        };
    }

    public OptionalInt budgetPresetAmountForSlot(int slot) {
        for (int i = 0; i < BUDGET_PRESET_SLOTS.length; i++) {
            if (BUDGET_PRESET_SLOTS[i] == slot) {
                return OptionalInt.of(BUDGET_PRESET_AMOUNTS[i]);
            }
        }
        return OptionalInt.empty();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static void placeIfVisible(
            Inventory inventory,
            ParliamentHubView view,
            ParliamentHubAction action,
            int slot,
            ItemStack item) {
        if (view.visibleActions().contains(action)) {
            inventory.setItem(slot, item);
        }
    }

    private static ItemStack billInfoItem(String title, dev.leo.kingdom.model.parliament.BillState billState) {
        ItemBuilder builder = new ItemBuilder(Material.WRITTEN_BOOK).displayAs(ChatColor.GOLD + title);
        if (billState != null) {
            builder.lore(ChatColor.GRAY + "State: " + formatBillState(billState));
        }
        return builder.build();
    }

    private static String formatBillState(dev.leo.kingdom.model.parliament.BillState billState) {
        return billState.name().toLowerCase(java.util.Locale.UK).replace('_', ' ');
    }

    private static ItemStack fiscalItem(ParliamentHubView view) {
        return ItemBuilder.labelled(
                Material.GOLD_INGOT,
                enabledColour(view.isEnabled(ParliamentHubAction.TABLE_FISCAL)) + "Table fiscal bill",
                "Propose tax and transfer rates");
    }

    private static ItemStack budgetItem(ParliamentHubView view) {
        return ItemBuilder.labelled(
                Material.CHEST,
                enabledColour(view.isEnabled(ParliamentHubAction.TABLE_BUDGET)) + "Table budget bill",
                "Set a treasury spending cap");
    }

    private static ItemStack spendMintItem(ParliamentHubView view) {
        boolean enabled = view.isEnabled(ParliamentHubAction.TABLE_SPEND_MINT);
        String lore = enabled
                ? "Authorise mint placement from treasury"
                : "Prepare a mint location at a lectern first";
        return ItemBuilder.labelled(Material.LECTERN, enabledColour(enabled) + "Table mint supply bill", lore);
    }

    private static ItemStack spendStipendItem(ParliamentHubView view) {
        return ItemBuilder.labelled(
                Material.EMERALD,
                enabledColour(view.isEnabled(ParliamentHubAction.TABLE_SPEND_STIPEND)) + "Table stipend bill",
                "Pay a seated noble from treasury");
    }

    private static ItemStack stipendOtherItem(ParliamentHubView view) {
        return ItemBuilder.labelled(
                Material.PLAYER_HEAD,
                enabledColour(view.isEnabled(ParliamentHubAction.STIPEND_OTHER)) + "Stipend another member",
                "Choose a recipient in chat");
    }

    private static ItemStack budgetPresetItem(int amount, boolean enabled) {
        return ItemBuilder.labelled(
                Material.GOLD_NUGGET,
                enabledColour(enabled) + String.valueOf(amount) + " Corona",
                "Table a budget bill for " + amount + " Corona");
    }

    private static ChatColor enabledColour(boolean enabled) {
        return enabled ? ChatColor.GREEN : ChatColor.DARK_GRAY;
    }

    private static void fillBackground(Inventory inventory) {
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }
}
