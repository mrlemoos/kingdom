package dev.mrlemoos.kingdom.locate;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

public final class LocateCompassFactory {

        private LocateCompassFactory() {
        }

        public static ItemStack create(Location target, String label) {
                ItemStack compass = new ItemBuilder(Material.COMPASS)
                                .displayAs(c("&6Kingdom compass"))
                                .lore(
                                                c("&7→ " + label),
                                                c("&7" + LocateCoordFormatter.format(target.getX(), target.getY(),
                                                                target.getZ())))
                                .build();

                CompassMeta meta = (CompassMeta) compass.getItemMeta();
                Location lodestone = new Location(
                                target.getWorld(),
                                target.getBlockX(),
                                target.getBlockY(),
                                target.getBlockZ());
                meta.setLodestone(lodestone);
                meta.setLodestoneTracked(false);
                compass.setItemMeta(meta);
                return compass;
        }
}
