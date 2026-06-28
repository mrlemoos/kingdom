package dev.mrlemoos.kingdom.mint;

import dev.mrlemoos.kingdom.economy.model.MintLocation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class TreasuryLordMintSelector {

    private TreasuryLordMintSelector() {}

    public static Optional<MintLocation> byLordUuid(List<MintLocation> mints, UUID lordUuid) {
        return mints.stream()
                .filter(mint -> mint.lordEntityId().filter(lordUuid::equals).isPresent())
                .findFirst();
    }

    public static Optional<MintLocation> forLordAt(
            List<MintLocation> mints,
            String worldName,
            double lordX,
            double lordY,
            double lordZ,
            Optional<UUID> lordEntityId) {
        if (lordEntityId.isPresent()) {
            Optional<MintLocation> byUuid = byLordUuid(mints, lordEntityId.get());
            if (byUuid.isPresent()) {
                return byUuid;
            }
        }
        return nearestInWorld(mints, worldName, lordX, lordY, lordZ);
    }

    public static Optional<MintLocation> selectForDespawn(
            List<MintLocation> mints,
            String worldName,
            double playerX,
            double playerY,
            double playerZ,
            Optional<UUID> aimedLordId) {
        if (aimedLordId.isPresent()) {
            Optional<MintLocation> byLord = byLordUuid(mints, aimedLordId.get());
            if (byLord.isPresent()) {
                return byLord;
            }
        }
        return nearestInWorld(mints, worldName, playerX, playerY, playerZ);
    }

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
