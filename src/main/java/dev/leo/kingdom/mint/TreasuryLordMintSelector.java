package dev.leo.kingdom.mint;

import dev.leo.kingdom.economy.model.MintLocation;
import java.util.List;
import java.util.Optional;

public final class TreasuryLordMintSelector {

    private TreasuryLordMintSelector() {}

    public static Optional<MintLocation> nearestInWorld(
            List<MintLocation> mints, String worldName, double x, double y, double z) {
        MintLocation nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (MintLocation mint : mints) {
            if (!mint.worldName().equals(worldName)) {
                continue;
            }
            double distance = squaredDistance(x, y, z, mint);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = mint;
            }
        }
        return Optional.ofNullable(nearest);
    }

    private static double squaredDistance(double x, double y, double z, MintLocation mint) {
        double dx = x - mint.x();
        double dy = y - mint.y();
        double dz = z - mint.z();
        return dx * dx + dy * dy + dz * dz;
    }
}
