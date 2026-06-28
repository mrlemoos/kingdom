package dev.leo.kingdom.mint;

import dev.leo.kingdom.helpers.ItemBuilder;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class TreasuryWithdrawGui implements InventoryHolder {

    public static final String TITLE = ChatColor.GOLD + "Treasury Withdrawal";

    static final int SLOT_BALANCE = 4;
    static final int SLOT_ONE = 10;
    static final int SLOT_FIVE = 11;
    static final int SLOT_TEN = 12;
    static final int SLOT_THIRTY_TWO = 13;
    static final int SLOT_SIXTY_FOUR = 14;
    static final int SLOT_MAX = 15;
    static final int SLOT_ALL = 16;
    static final int SLOT_CUSTOM = 22;

    private final String kingdomId;
    private Inventory inventory;

    public TreasuryWithdrawGui(String kingdomId) {
        this.kingdomId = kingdomId;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public static TreasuryWithdrawGui create(String kingdomId, double walletBalance) {
        TreasuryWithdrawGui gui = new TreasuryWithdrawGui(kingdomId);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        populate(inventory, walletBalance);
        return gui;
    }

    static void populate(Inventory inventory, double walletBalance) {
        inventory.clear();
        int wholeBalance = (int) Math.floor(walletBalance);

        inventory.setItem(SLOT_BALANCE, balanceItem(wholeBalance));
        inventory.setItem(SLOT_ONE, amountButton(1, wholeBalance));
        inventory.setItem(SLOT_FIVE, amountButton(5, wholeBalance));
        inventory.setItem(SLOT_TEN, amountButton(10, wholeBalance));
        inventory.setItem(SLOT_THIRTY_TWO, amountButton(32, wholeBalance));
        inventory.setItem(SLOT_SIXTY_FOUR, amountButton(64, wholeBalance));
        inventory.setItem(SLOT_MAX, amountButton(Math.min(64, wholeBalance), wholeBalance, "Max stack"));
        inventory.setItem(SLOT_ALL, amountButton(wholeBalance, wholeBalance, "Withdraw all"));
        inventory.setItem(SLOT_CUSTOM, customAmountButton());
        fillBackground(inventory);
    }

    public Integer amountForSlot(int slot, double walletBalance) {
        int wholeBalance = (int) Math.floor(walletBalance);
        return switch (slot) {
            case SLOT_ONE -> wholeBalance >= 1 ? 1 : null;
            case SLOT_FIVE -> wholeBalance >= 5 ? 5 : null;
            case SLOT_TEN -> wholeBalance >= 10 ? 10 : null;
            case SLOT_THIRTY_TWO -> wholeBalance >= 32 ? 32 : null;
            case SLOT_SIXTY_FOUR -> wholeBalance >= 64 ? 64 : null;
            case SLOT_MAX -> wholeBalance >= 1 ? Math.min(64, wholeBalance) : null;
            case SLOT_ALL -> wholeBalance >= 1 ? wholeBalance : null;
            default -> null;
        };
    }

    public boolean isCustomSlot(int slot) {
        return slot == SLOT_CUSTOM;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static ItemStack balanceItem(int wholeBalance) {
        return new ItemBuilder(Material.GOLD_BLOCK)
                .displayAs(ChatColor.YELLOW + "Your wallet")
                .lore(
                        ChatColor.WHITE + String.valueOf(wholeBalance) + ChatColor.GRAY + " Corona available",
                        ChatColor.GRAY + "1 gold nugget = 1 Corona")
                .build();
    }

    private static ItemStack amountButton(int amount, int wholeBalance) {
        return amountButton(amount, wholeBalance, null);
    }

    private static ItemStack amountButton(int amount, int wholeBalance, String labelOverride) {
        boolean enabled = amount > 0 && wholeBalance >= amount;
        String label = labelOverride != null ? labelOverride : "Withdraw " + amount;
        ItemBuilder builder = new ItemBuilder(Material.GOLD_NUGGET, Math.min(amount, 64))
                .displayAs((enabled ? ChatColor.GREEN : ChatColor.DARK_GRAY) + label)
                .lore(enabled
                        ? ChatColor.GRAY + "Click to withdraw " + amount + " Corona"
                        : ChatColor.RED + "Insufficient balance");
        if (!enabled) {
            builder.type(Material.GRAY_DYE);
        }
        return builder.build();
    }

    private static ItemStack customAmountButton() {
        return new ItemBuilder(Material.PAPER)
                .displayAs(ChatColor.AQUA + "Custom amount")
                .lore(
                        ChatColor.GRAY + "Click, then type the amount in chat",
                        ChatColor.GRAY + "Whole Corona only")
                .build();
    }

    private static void fillBackground(Inventory inventory) {
        ItemStack filler = ItemBuilder.fillerPane(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    static String formatCorona(int amount) {
        return String.format(Locale.UK, "%d", amount);
    }
}
