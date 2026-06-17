package dev.leo.kingdom.model;

import org.bukkit.ChatColor;

public enum NobleRank {
    KING(1, "King", "Queen"),
    QUEEN(1, "Queen", "Queen"),
    DUKE(2, "Duke", "Duchess"),
    COUNT(4, "Count", "Countess");

    private final int maxPerKingdom;
    private final String defaultMasculineTitle;
    private final String defaultFeminineTitle;

    NobleRank(int maxPerKingdom, String defaultMasculineTitle, String defaultFeminineTitle) {
        this.maxPerKingdom = maxPerKingdom;
        this.defaultMasculineTitle = defaultMasculineTitle;
        this.defaultFeminineTitle = defaultFeminineTitle;
    }

    public int getMaxPerKingdom() {
        return maxPerKingdom;
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
            case DUKE -> ChatColor.BLUE;
            case COUNT -> ChatColor.RED;
        };
    }

    public static NobleRank fromCommand(String input) {
        return switch (input.toLowerCase()) {
            case "king" -> KING;
            case "queen" -> QUEEN;
            case "duke" -> DUKE;
            case "count" -> COUNT;
            default -> throw new IllegalArgumentException("Unknown rank: " + input);
        };
    }
}
