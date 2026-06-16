package dev.leo.kingdom.model;

public enum TitleStyle {
    MASCULINE,
    FEMININE;

    public static TitleStyle fromCommand(String input) {
        return switch (input.toLowerCase()) {
            case "duchess", "countess", "feminine", "f" -> FEMININE;
            case "duke", "count", "masculine", "m" -> MASCULINE;
            default -> throw new IllegalArgumentException("Unknown title style: " + input);
        };
    }

    public static boolean isStyleToken(String input) {
        return switch (input.toLowerCase()) {
            case "duchess", "countess", "feminine", "f", "duke", "count", "masculine", "m" -> true;
            default -> false;
        };
    }
}
