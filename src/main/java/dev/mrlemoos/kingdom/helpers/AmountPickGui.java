package dev.mrlemoos.kingdom.helpers;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * A small window for choosing a sum of Corona: a row of preset buttons, a custom amount typed in
 * chat, and Back. What the sum is for is the caller's — an arrest reward at the court, war debt
 * from the Hub — and is carried as a callback; {@code listener/AmountPickListener} routes the clicks.
 */
public final class AmountPickGui implements InventoryHolder {

    public record Preset(String label, double amount) {}

    public static final int SLOT_HEADER = 4;
    public static final int FIRST_PRESET_SLOT = 10;
    public static final int MAX_PRESETS = 7;
    public static final int SLOT_CUSTOM = 22;
    public static final int SLOT_BACK = 18;

    private final Map<Integer, Double> amounts;
    private final String prompt;
    private final BiConsumer<Player, Double> onAmount;
    private final Consumer<Player> onBack;
    private Inventory inventory;

    private AmountPickGui(
            Map<Integer, Double> amounts, String prompt, BiConsumer<Player, Double> onAmount, Consumer<Player> onBack) {
        this.amounts = Map.copyOf(amounts);
        this.prompt = prompt;
        this.onAmount = Objects.requireNonNull(onAmount, "onAmount");
        this.onBack = onBack;
    }

    /**
     * @param prompt the chat line asking for a custom amount
     * @param onBack where Back leads; null for no Back button
     */
    public static AmountPickGui create(
            String title,
            String heading,
            List<String> headerLore,
            List<Preset> presets,
            String prompt,
            BiConsumer<Player, Double> onAmount,
            Consumer<Player> onBack) {
        Map<Integer, Double> amounts = new HashMap<>();
        List<Preset> shown = presets.subList(0, Math.min(presets.size(), MAX_PRESETS));
        for (int i = 0; i < shown.size(); i++) {
            amounts.put(FIRST_PRESET_SLOT + i, shown.get(i).amount());
        }
        AmountPickGui gui = new AmountPickGui(amounts, prompt, onAmount, onBack);
        Inventory inventory = Bukkit.createInventory(gui, 27, component(title));
        gui.inventory = inventory;
        ItemBuilder header = new ItemBuilder(Material.GOLD_BLOCK).displayAs(c("&6" + heading));
        for (String line : headerLore) {
            header.lore(c("&7" + line));
        }
        inventory.setItem(SLOT_HEADER, header.build());
        for (int i = 0; i < shown.size(); i++) {
            Preset preset = shown.get(i);
            inventory.setItem(FIRST_PRESET_SLOT + i, new ItemBuilder(Material.GOLD_NUGGET)
                    .displayAs(c("&a" + preset.label()))
                    .lore(c("&7Click to give " + formatCorona(preset.amount()) + " Corona"))
                    .build());
        }
        inventory.setItem(SLOT_CUSTOM, new ItemBuilder(Material.PAPER)
                .displayAs(c("&bCustom amount"))
                .lore(c("&7Click, then type the amount in chat"))
                .build());
        if (onBack != null) {
            inventory.setItem(SLOT_BACK, ItemBuilder.labelled(Material.OAK_DOOR, c("&eBack"), "Return"));
        }
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
        return gui;
    }

    /** The preset amount on this slot, or null. */
    public Double amountForSlot(int slot) {
        return amounts.get(slot);
    }

    public boolean isCustomSlot(int slot) {
        return slot == SLOT_CUSTOM;
    }

    public boolean isBackSlot(int slot) {
        return slot == SLOT_BACK && onBack != null;
    }

    public String prompt() {
        return prompt;
    }

    public void choose(Player player, double amount) {
        onAmount.accept(player, amount);
    }

    public void back(Player player) {
        if (onBack != null) {
            onBack.accept(player);
        }
    }

    public static String formatCorona(double amount) {
        if (Math.rint(amount) == amount) {
            return String.format(Locale.UK, "%.0f", amount);
        }
        return String.format(Locale.UK, "%.2f", amount);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
