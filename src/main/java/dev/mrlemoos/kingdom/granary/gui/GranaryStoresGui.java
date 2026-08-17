package dev.mrlemoos.kingdom.granary.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.granary.GranaryTheftLog;
import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * What the realm's stores come to, read off a bale of hay in the granary: the stock against the room
 * for it, what it covers of the winter, what the fields are bringing in, how far short the realm
 * stands, and how its villagers are faring. The Crown alone is shown the hands caught taking grain.
 */
public final class GranaryStoresGui implements InventoryHolder {

    public static final Component TITLE = component("&6Granary Stores");

    /** The figures the stores are read by, all of them derived and none of them kept. */
    public record View(
            int stock,
            int capacity,
            int ration,
            int daysCovered,
            int shortfall,
            int farmers,
            int balesADay,
            int hungry,
            int starving) {}

    private final String kingdomId;
    private Inventory inventory;

    private GranaryStoresGui(String kingdomId) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
    }

    public String kingdomId() {
        return kingdomId;
    }

    public static GranaryStoresGui create(
            String kingdomId, View view, List<GranaryTheftLog.Entry> thefts, boolean crown) {
        GranaryStoresGui gui = new GranaryStoresGui(kingdomId);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        populate(inventory, view, thefts, crown);
        return gui;
    }

    private static void populate(
            Inventory inventory, View view, List<GranaryTheftLog.Entry> thefts, boolean crown) {
        inventory.setItem(
                10,
                item(
                        Material.HAY_BLOCK,
                        "&eThe store",
                        view.stock() + " of " + view.capacity() + " bales",
                        view.capacity() - view.stock() + " bales of room left"));
        inventory.setItem(
                12,
                item(
                        Material.CLOCK,
                        "&eThe winter",
                        "Ration " + view.ration() + (view.ration() == 1 ? " bale a day" : " bales a day"),
                        coverage(view)));
        inventory.setItem(
                14,
                item(
                        Material.WHEAT,
                        "&eThe fields",
                        view.farmers() + (view.farmers() == 1 ? " farmer" : " farmers") + " at work",
                        "About " + view.balesADay() + " bales a day at this season"));
        inventory.setItem(
                16,
                item(
                        Material.BREAD,
                        "&eThe realm",
                        shortfallLine(view),
                        view.hungry() + " hungry, " + view.starving() + " starving"));
        if (crown) {
            inventory.setItem(22, item(Material.IRON_BARS, "&cGrain theft", theftLore(thefts)));
        }
    }

    private static ItemStack item(Material material, String name, String... lore) {
        List<String> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(c("&7" + line));
        }
        return new ItemBuilder(material).displayAs(c(name)).lore(lines).build();
    }

    private static String coverage(View view) {
        if (view.ration() <= 0) {
            return "No mouths to feed";
        }
        if (view.daysCovered() <= 0) {
            return "Covers no day of winter";
        }
        return "Covers " + view.daysCovered() + (view.daysCovered() == 1 ? " day" : " days") + " of winter";
    }

    private static String shortfallLine(View view) {
        if (view.shortfall() <= 0) {
            return "Provisioned for the winter";
        }
        return "Short " + view.shortfall() + (view.shortfall() == 1 ? " bale" : " bales") + " of the winter";
    }

    private static String[] theftLore(List<GranaryTheftLog.Entry> thefts) {
        if (thefts == null || thefts.isEmpty()) {
            return new String[] {"No hand has been caught in the granary"};
        }
        List<String> lore = new ArrayList<>();
        for (GranaryTheftLog.Entry entry : thefts) {
            String name = Bukkit.getOfflinePlayer(entry.thiefId()).getName();
            lore.add((name == null ? entry.thiefId().toString() : name) + " — day " + entry.mcDay());
        }
        return lore.toArray(new String[0]);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
