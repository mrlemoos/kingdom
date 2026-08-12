package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import java.util.Optional;

/**
 * The Royal Standard: the kingdom flag flown beside the House of Lords.
 *
 * <p>
 * Position is derived from the stored Lords point, one block to the east so it never stands where a
 * peer lands. The design is the persisted {@linkplain
 * dev.mrlemoos.kingdom.model.parliament.KingdomFlag kingdom flag}; when none is stored yet, Crown
 * gold is used.
 */
public final class RoyalStandard {

    /** Where the standard flies: a block position, not a landing spot. */
    public record StandardPosition(String worldName, int x, int y, int z) {
    }

    private static final String DEFAULT_BANNER = "WHITE_BANNER";

    private RoyalStandard() {
    }

    /** The block the standard flies from, derived from the Lords point. */
    public static Optional<StandardPosition> positionFor(ChamberSite site) {
        if (site == null || site.worldName().isBlank()) {
            return Optional.empty();
        }
        int x = (int) Math.floor(site.x());
        int y = (int) Math.floor(site.y());
        int z = (int) Math.floor(site.z());
        return Optional.of(new StandardPosition(site.worldName(), x + 1, y, z));
    }

    /** The banner the Crown's colour flies as. */
    public static String crownBannerMaterial() {
        return bannerMaterialFor(NobleRank.KING.chatColor());
    }

    /** Translates a legacy colour code — {@code &6}, {@code §6}, or bare {@code 6} — to a banner. */
    public static String bannerMaterialFor(String colour) {
        if (colour == null) {
            return DEFAULT_BANNER;
        }
        String stripped = colour.replace("&", "").replace("§", "").trim();
        if (stripped.length() != 1) {
            return DEFAULT_BANNER;
        }
        return switch (Character.toLowerCase(stripped.charAt(0))) {
            case '0' -> "BLACK_BANNER";
            case '1' -> "BLUE_BANNER";
            case '2' -> "GREEN_BANNER";
            case '3' -> "CYAN_BANNER";
            case '4' -> "RED_BANNER";
            case '5' -> "PURPLE_BANNER";
            case '6' -> "ORANGE_BANNER";
            case '7' -> "LIGHT_GRAY_BANNER";
            case '8' -> "GRAY_BANNER";
            case '9' -> "BLUE_BANNER";
            case 'a' -> "LIME_BANNER";
            case 'b' -> "LIGHT_BLUE_BANNER";
            case 'c' -> "RED_BANNER";
            case 'd' -> "MAGENTA_BANNER";
            case 'e' -> "YELLOW_BANNER";
            case 'f' -> "WHITE_BANNER";
            default -> DEFAULT_BANNER;
        };
    }
}
