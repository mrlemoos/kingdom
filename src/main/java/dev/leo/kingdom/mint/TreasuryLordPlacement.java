package dev.leo.kingdom.mint;

import dev.leo.kingdom.economy.model.MintLocation;
import org.bukkit.ChatColor;

public final class TreasuryLordPlacement {

    public static final String LORD_DISPLAY_NAME = ChatColor.GOLD + "Lord of the Treasury";

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
