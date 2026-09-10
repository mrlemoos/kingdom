package dev.mrlemoos.kingdom.command;

import java.util.ArrayList;
import java.util.List;

/**
 * The realm's one wording for an order it does not know.
 *
 * <p>There are two doors onto it. {@link KingdomCommand#execute} refuses a subcommand it has no case
 * for, which is what any caller inside the plugin hits; and Cloud's syntax handler refuses a
 * mistyped literal, which is what a player at the keyboard hits, because Cloud rejects an
 * unparseable literal before {@code execute} is ever called. Both read from here so the realm never
 * says it two ways.
 *
 * <p>Plain domain logic with no platform dependency, so the wording is unit-testable.
 */
public final class UnknownOrderRefusal {

    /** Where a subject goes when they have forgotten the realm's orders. */
    public static final String HUB_POINTER = "Type /kingdom on its own to open the Realm Hub.";

    private UnknownOrderRefusal() {}

    /** "The realm knows no such order: parliment", or the bare refusal when nothing was named. */
    public static String refusal(String order) {
        if (order == null || order.isBlank()) {
            return "The realm knows no such order.";
        }
        return "The realm knows no such order: " + order.trim();
    }

    /**
     * The order a sender meant, read out of a raw command line such as {@code "kingdom parliment"}:
     * the word after the root. Falls back to the root itself when nothing follows it, and to nothing
     * at all when the line is empty.
     */
    public static String orderIn(String rawInput) {
        if (rawInput == null) {
            return "";
        }
        String trimmed = rawInput.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        String[] words = trimmed.split("\\s+");
        if (words.length == 0 || words[0].isBlank()) {
            return "";
        }
        if (words.length == 1) {
            return words[0];
        }
        return words[1];
    }

    /**
     * What Cloud's handler says: the refusal by name, the syntax the realm expected when it knows
     * one, and the way back to the hub.
     */
    public static List<String> lines(String rawInput, String correctSyntax) {
        List<String> lines = new ArrayList<>();
        lines.add(refusal(orderIn(rawInput)));
        if (correctSyntax != null && !correctSyntax.isBlank()) {
            lines.add("The order runs: /" + correctSyntax.trim());
        }
        lines.add(HUB_POINTER);
        return List.copyOf(lines);
    }
}
