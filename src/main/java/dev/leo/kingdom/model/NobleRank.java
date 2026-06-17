package dev.leo.kingdom.model;

import java.util.List;
import org.bukkit.ChatColor;

public enum NobleRank {
    KING(1, 1, "King", "Queen"),
    QUEEN(2, 1, "Queen", "Queen"),
    PREMIER(3, 1, "Premier", "Premier"),
    SPEAKER(4, 1, "Speaker", "Speaker"),
    DUKE(5, 2, "Duke", "Duchess"),
    LORD(6, 2, "Lord", "Lady"),
    COUNT(7, 4, "Count", "Countess"),
    MP(8, 8, "MP", "MP"),
    KNIGHT(9, -1, "Knight", "Dame");

    private static final int UNLIMITED = -1;

    private final int hierarchyOrder;
    private final int maxPerKingdom;
    private final String defaultMasculineTitle;
    private final String defaultFeminineTitle;

    NobleRank(int hierarchyOrder, int maxPerKingdom, String defaultMasculineTitle, String defaultFeminineTitle) {
        this.hierarchyOrder = hierarchyOrder;
        this.maxPerKingdom = maxPerKingdom;
        this.defaultMasculineTitle = defaultMasculineTitle;
        this.defaultFeminineTitle = defaultFeminineTitle;
    }

    public int hierarchyOrder() {
        return hierarchyOrder;
    }

    public int getMaxPerKingdom() {
        return maxPerKingdom;
    }

    public boolean hasSlotLimit() {
        return maxPerKingdom != UNLIMITED;
    }

    public String displayTitle(TitleStyle style) {
        return switch (style) {
            case MASCULINE -> defaultMasculineTitle;
            case FEMININE -> defaultFeminineTitle;
        };
    }

    public ChatColor chatColor() {
        return switch (this) {
            case KING, QUEEN -> ChatColor.GOLD;
            case PREMIER -> ChatColor.DARK_GREEN;
            case SPEAKER -> ChatColor.AQUA;
            case DUKE -> ChatColor.BLUE;
            case LORD -> ChatColor.LIGHT_PURPLE;
            case COUNT -> ChatColor.RED;
            case MP -> ChatColor.GRAY;
            case KNIGHT -> ChatColor.WHITE;
        };
    }

    public static NobleRank fromCommand(String input) {
        return switch (input.toLowerCase()) {
            case "king" -> KING;
            case "queen" -> QUEEN;
            case "premier" -> PREMIER;
            case "speaker" -> SPEAKER;
            case "duke" -> DUKE;
            case "lord" -> LORD;
            case "count" -> COUNT;
            case "mp" -> MP;
            case "knight" -> KNIGHT;
            default -> throw new IllegalArgumentException("Unknown rank: " + input);
        };
    }

    public static List<String> commandTokens() {
        return List.of("king", "queen", "premier", "speaker", "duke", "lord", "count", "mp", "knight");
    }
}
