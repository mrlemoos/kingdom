package dev.mrlemoos.kingdom.hub;

import java.util.List;

/**
 * One entry of the Realm Hub, in plain words: what it is, whether it is yours to use, who may use it
 * when it is not, and what a click does. No colour codes and no Bukkit — the GUI dresses this up.
 *
 * @param refusal empty when {@code usable}; otherwise names who may
 */
public record RealmHubEntry(
        RealmHubTopic topic, String title, boolean usable, String refusal, List<String> lines, RealmHubAction action) {

    public RealmHubEntry {
        refusal = refusal == null || usable ? "" : refusal;
        lines = lines == null ? List.of() : List.copyOf(lines);
        action = action == null ? RealmHubAction.NONE : action;
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
}
