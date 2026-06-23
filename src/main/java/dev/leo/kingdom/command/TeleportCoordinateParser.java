package dev.leo.kingdom.command;

import org.bukkit.Location;
import org.bukkit.World;

public final class TeleportCoordinateParser {

    private TeleportCoordinateParser() {}

    public static double parseComponent(String token, double base) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Coordinate cannot be empty.");
        }
        if (token.charAt(0) == '~') {
            if (token.length() == 1) {
                return base;
            }
            return base + Double.parseDouble(token.substring(1));
        }
        return Double.parseDouble(token);
    }

    public static boolean isCoordinateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        if (token.charAt(0) == '~') {
            if (token.length() == 1) {
                return true;
            }
            try {
                Double.parseDouble(token.substring(1));
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static Location parseLocation(World world, String[] args, int startIndex, Location origin) {
        if (args.length - startIndex < 3) {
            throw new IllegalArgumentException("Expected at least three coordinates.");
        }
        double x = parseComponent(args[startIndex], origin.getX());
        double y = parseComponent(args[startIndex + 1], origin.getY());
        double z = parseComponent(args[startIndex + 2], origin.getZ());
        float yaw = origin.getYaw();
        float pitch = origin.getPitch();
        if (args.length - startIndex >= 5) {
            yaw = (float) parseComponent(args[startIndex + 3], yaw);
            pitch = (float) parseComponent(args[startIndex + 4], pitch);
        }
        return new Location(world, x, y, z, yaw, pitch);
    }
}
