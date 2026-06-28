package dev.mrlemoos.kingdom.model;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import java.util.List;

public enum NobleRank {
    KING(1, 1, "King", "Queen"),
    QUEEN(2, 1, "Queen", "Queen"),
    PRINCE(3, 2, "Prince", "Princess"),
    PREMIER(4, 1, "Premier", "Premier"),
    SPEAKER(5, 1, "Speaker", "Speaker"),
    DUKE(6, 2, "Duke", "Duchess"),
    LORD(7, 2, "Lord", "Lady"),
    COUNT(8, 4, "Count", "Countess"),
    MP(9, 8, "MP", "MP"),
    KNIGHT(10, -1, "Knight", "Dame");

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

    public String chatColor() {
        return switch (this) {
            case KING, QUEEN -> c("&6");
            case PRINCE -> c("&e");
            case PREMIER -> c("&2");
            case SPEAKER -> c("&b");
            case DUKE -> c("&9");
            case LORD -> c("&d");
            case COUNT -> c("&c");
            case MP -> c("&7");
            case KNIGHT -> c("&f");
        };
    }

    public static NobleRank fromCommand(String input) {
        return switch (input.toLowerCase()) {
            case "king" -> KING;
            case "queen" -> QUEEN;
            case "prince", "princess" -> PRINCE;
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
        return List.of("king", "queen", "prince", "princess", "premier", "speaker", "duke", "lord", "count", "mp", "knight");
    }
}
