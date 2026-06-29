package dev.mrlemoos.kingdom.economy;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public final class CoronaItem {

    public static final String DISPLAY_NAME_SINGULAR = c("&6Corona");
    public static final String DISPLAY_NAME_PLURAL = c("&6Coronas");

    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private CoronaItem() {}

    public static String displayNameForAmount(int amount) {
        return amount == 1 ? DISPLAY_NAME_SINGULAR : DISPLAY_NAME_PLURAL;
    }

    public static boolean isCoronaDisplayName(String displayName) {
        return DISPLAY_NAME_SINGULAR.equals(displayName) || DISPLAY_NAME_PLURAL.equals(displayName);
    }

    public static ItemStack create(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Corona stack amount must be positive.");
        }
        return new ItemBuilder(Material.GOLD_NUGGET, amount)
                .displayAs(displayNameForAmount(amount))
                .build();
    }

    public static ItemStack goldNuggets(int amount) {
        return create(amount);
    }

    public static boolean isCorona(ItemStack stack) {
        if (stack == null || stack.getType() != Material.GOLD_NUGGET) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        String name = LEGACY_SECTION.serialize(meta.displayName());
        return isCoronaDisplayName(name);
    }

    public static boolean isCoronaNugget(ItemStack stack) {
        return isCorona(stack);
    }

    public static int count(PlayerInventory inventory) {
        return countInContents(inventory.getContents());
    }

    public static int countInContents(ItemStack[] contents) {
        int count = 0;
        for (ItemStack stack : contents) {
            if (isCorona(stack)) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    public static void removeAll(PlayerInventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isCorona(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }
    }

    public static boolean hasSpace(PlayerInventory inventory, int nuggetCount) {
        int remaining = nuggetCount;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null || stack.getType().isAir()) {
                remaining -= Material.GOLD_NUGGET.getMaxStackSize();
            } else if (isCorona(stack) && stack.getAmount() < stack.getMaxStackSize()) {
                remaining -= stack.getMaxStackSize() - stack.getAmount();
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return remaining <= 0;
    }

    public static void give(PlayerInventory inventory, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, Material.GOLD_NUGGET.getMaxStackSize());
            inventory.addItem(create(stackSize));
            remaining -= stackSize;
        }
    }
}
