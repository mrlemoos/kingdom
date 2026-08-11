package dev.mrlemoos.kingdom.model.election;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * What a candidate declares when they nominate: a <b>manifesto</b> line, a <b>party</b> name, and
 * the colour that party stands under. All three are free text and none is enforced — a manifesto is
 * a promise made to the realm, not a term the plugin checks against how its author later votes.
 *
 * <p>A candidate may decline to declare: a blank manifesto and a blank party are both valid, and a
 * partyless candidate stands as an <b>independent</b>. Villager MPs never carry a declaration; they
 * stand under their <b>profession bloc</b> instead.
 */
public record CandidateDeclaration(String manifesto, String partyName, String partyColour) {

    /** A manifesto is one line, read out with the result — not a pamphlet. */
    public static final int MAX_MANIFESTO_LENGTH = 60;

    /** A party name sits beside a member's name in every tally, so it is kept short. */
    public static final int MAX_PARTY_NAME_LENGTH = 20;

    /** What a candidate who declares no party stands as. */
    public static final String INDEPENDENT_LABEL = "Independent";

    /** The colour of a party that declared none. */
    public static final String DEFAULT_PARTY_COLOUR = "&f";

    private static final String COLOUR_CODES = "0123456789abcdef";

    private static final Map<String, String> COLOUR_NAMES = Map.ofEntries(
            Map.entry("black", "&0"),
            Map.entry("dark_blue", "&1"),
            Map.entry("dark_green", "&2"),
            Map.entry("dark_aqua", "&3"),
            Map.entry("dark_red", "&4"),
            Map.entry("purple", "&5"),
            Map.entry("gold", "&6"),
            Map.entry("grey", "&7"),
            Map.entry("gray", "&7"),
            Map.entry("dark_grey", "&8"),
            Map.entry("dark_gray", "&8"),
            Map.entry("blue", "&9"),
            Map.entry("green", "&a"),
            Map.entry("aqua", "&b"),
            Map.entry("red", "&c"),
            Map.entry("pink", "&d"),
            Map.entry("yellow", "&e"),
            Map.entry("white", "&f"));

    private static final CandidateDeclaration BLANK = new CandidateDeclaration("", "", DEFAULT_PARTY_COLOUR);

    public CandidateDeclaration {
        manifesto = manifesto == null ? "" : manifesto.trim();
        partyName = partyName == null ? "" : partyName.trim();
        partyColour = normaliseColour(partyColour);
    }

    /** A candidate who declared nothing at all. */
    public static CandidateDeclaration blank() {
        return BLANK;
    }

    /**
     * Builds a declaration, refusing an over-length manifesto or party name and an unknown colour.
     *
     * @throws IllegalArgumentException with the message the realm should be shown
     */
    public static CandidateDeclaration of(String manifesto, String partyName, String partyColour) {
        Optional<String> rejection = rejectionReason(manifesto, partyName, partyColour);
        if (rejection.isPresent()) {
            throw new IllegalArgumentException(rejection.get());
        }
        return new CandidateDeclaration(manifesto, partyName, partyColour);
    }

    /** Why the declaration cannot stand, or empty where it may. */
    public static Optional<String> rejectionReason(String manifesto, String partyName, String partyColour) {
        String trimmedManifesto = manifesto == null ? "" : manifesto.trim();
        String trimmedParty = partyName == null ? "" : partyName.trim();
        if (trimmedManifesto.length() > MAX_MANIFESTO_LENGTH) {
            return Optional.of("A manifesto may be no longer than " + MAX_MANIFESTO_LENGTH + " characters.");
        }
        if (trimmedParty.length() > MAX_PARTY_NAME_LENGTH) {
            return Optional.of("A party name may be no longer than " + MAX_PARTY_NAME_LENGTH + " characters.");
        }
        if (partyColour != null && !partyColour.isBlank() && parseColour(partyColour).isEmpty()) {
            return Optional.of("That is not a party colour the realm recognises.");
        }
        return Optional.empty();
    }

    /** Reads a colour written as {@code &c}, {@code c}, or a name such as {@code red}. */
    public static Optional<String> parseColour(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String trimmed = input.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        String named = COLOUR_NAMES.get(trimmed);
        if (named != null) {
            return Optional.of(named);
        }
        String code = trimmed.startsWith("&") || trimmed.startsWith("§") ? trimmed.substring(1) : trimmed;
        if (code.length() == 1 && COLOUR_CODES.indexOf(code.charAt(0)) >= 0) {
            return Optional.of("&" + code);
        }
        return Optional.empty();
    }

    public boolean hasManifesto() {
        return !manifesto.isEmpty();
    }

    public boolean hasParty() {
        return !partyName.isEmpty();
    }

    /** Nothing declared either way — the candidate said nothing at all. */
    public boolean isBlank() {
        return !hasManifesto() && !hasParty();
    }

    /** The party name, or {@code Independent} where the candidate declared none. */
    public String partyLabel() {
        return hasParty() ? partyName : INDEPENDENT_LABEL;
    }

    private static String normaliseColour(String partyColour) {
        return parseColour(partyColour).orElse(DEFAULT_PARTY_COLOUR);
    }
}
