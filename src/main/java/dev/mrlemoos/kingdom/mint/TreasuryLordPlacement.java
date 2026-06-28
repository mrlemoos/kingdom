package dev.mrlemoos.kingdom.mint;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.economy.model.MintLocation;

public final class TreasuryLordPlacement {

    public static final String LORD_DISPLAY_NAME = c("&6Lord of the Treasury");

    private TreasuryLordPlacement() {}

    public static int lordBlockX(MintLocation mint) {
        return mint.x();
    }

    public static int lordBlockY(MintLocation mint) {
        return mint.y();
    }

    public static int lordBlockZ(MintLocation mint) {
        return mint.z() - 1;
    }

    public static boolean isTreasuryLordDisplayName(String displayName) {
        return LORD_DISPLAY_NAME.equals(displayName);
    }
}
