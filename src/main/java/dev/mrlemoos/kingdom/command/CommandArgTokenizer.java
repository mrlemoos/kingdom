package dev.mrlemoos.kingdom.command;

public final class CommandArgTokenizer {

    private CommandArgTokenizer() {}

    public static String[] tokenize(String input) {
        if (input == null || input.isBlank()) {
            return new String[0];
        }
        return input.trim().split("\\s+");
    }
}
