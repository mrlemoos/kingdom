package dev.mrlemoos.kingdom.hub;

import java.util.List;

/**
 * One entry of the Realm Hub, in plain words: where things stand and how it is done ({@code lines}),
 * then who may — the {@code refusal} when it is not the reader's, else {@code who}. No colour codes
 * and no Bukkit — the GUI dresses this up.
 *
 * @param refusal empty when {@code usable}; otherwise names who may
 * @param who who may, shown when the entry is the reader's; blank when there is nothing to say
 */
public record RealmHubEntry(
        RealmHubTopic topic,
        String title,
        boolean usable,
        String refusal,
        List<String> lines,
        RealmHubAction action,
        String who) {

    public RealmHubEntry {
        refusal = refusal == null || usable ? "" : refusal;
        lines = lines == null ? List.of() : List.copyOf(lines);
        action = action == null ? RealmHubAction.NONE : action;
        who = who == null ? "" : who;
    }

    public RealmHubEntry(
            RealmHubTopic topic,
            String title,
            boolean usable,
            String refusal,
            List<String> lines,
            RealmHubAction action) {
        this(topic, title, usable, refusal, lines, action, "");
    }

    /** An entry this subject may act on. */
    public static RealmHubEntry usable(
            RealmHubTopic topic, String title, List<String> lines, RealmHubAction action) {
        return new RealmHubEntry(topic, title, true, "", lines, action);
    }

    /** An entry this subject may not act on; {@code refusal} names who may. */
    public static RealmHubEntry refused(
            RealmHubTopic topic, String title, String refusal, List<String> lines) {
        return new RealmHubEntry(topic, title, false, refusal, lines, RealmHubAction.NONE);
    }

    /** This entry, naming who may. */
    public RealmHubEntry withWho(String who) {
        return new RealmHubEntry(topic, title, usable, refusal, lines, action, who);
    }

    /** The last line of the entry: the refusal when it is not the reader's, else who may. */
    public String whoLine() {
        return usable ? who : refusal;
    }
}
