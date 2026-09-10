package dev.mrlemoos.kingdom.war.siege;

/** Hub copy for live siege presence plus captured-chunk count. */
public final class SiegeStatusLine {

    private SiegeStatusLine() {}

    public static String format(
            String attackerLabel, int attackerCount, String defenderLabel, int defenderCount, int capturedChunks) {
        return "Defender territory: "
                + attackerLabel
                + " "
                + attackerCount
                + "; "
                + defenderLabel
                + " "
                + defenderCount
                + ". Captured chunks: "
                + capturedChunks
                + ".";
    }
}
