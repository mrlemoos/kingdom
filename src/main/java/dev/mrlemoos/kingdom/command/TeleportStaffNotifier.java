package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TeleportStaffNotifier {

    public static final String PERM_TELEPORT = "minecraft.command.teleport";

    private TeleportStaffNotifier() {
    }

    public static void notifyCrossPlayerTeleport(CommandSender actor, Player target, String destination) {
        if (!actor.hasPermission(PERM_TELEPORT)) {
            return;
        }
        String message = c("&7" + actorLabel(actor) + " teleported " + target.getName() + " to " + destination + ".");
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission(PERM_TELEPORT)) {
                continue;
            }
            if (actor instanceof Player playerActor && online.equals(playerActor)) {
                continue;
            }
            online.sendMessage(message);
        }
    }

    public static String formatCoordinates(Location location) {
        return formatCoord(location.getX())
                + " "
                + formatCoord(location.getY())
                + " "
                + formatCoord(location.getZ());
    }

    public static String formatCheckpointDestination(String kingdomDisplayName, String checkpointName) {
        return kingdomDisplayName + " checkpoint " + checkpointName;
    }

    private static String actorLabel(CommandSender actor) {
        if (actor instanceof Player player) {
            return player.getName();
        }
        return "Console";
    }

    private static String formatCoord(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.UK, "%.0f", value);
        }
        return String.format(Locale.UK, "%.2f", value);
    }
}
