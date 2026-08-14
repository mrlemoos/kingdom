package dev.mrlemoos.kingdom.honours;

/**
 * Words spoken when the Crown bestows a title. Presentation copy only; the grant itself is
 * {@code KingdomService.assignTitle}.
 */
public final class HonourProclamation {

    private HonourProclamation() {}

    public static String line(String subjectName, String rankLabel) {
        String name = subjectName == null || subjectName.isBlank() ? "a subject" : subjectName;
        String title = rankLabel == null || rankLabel.isBlank() ? "noble" : rankLabel;
        return "The Crown creates " + name + " " + article(title) + " " + title + ".";
    }

    public static String screenSubheading(String rankLabel) {
        return isKnighthood(rankLabel) ? "Arise and be recognised" : "The Crown confers this honour";
    }

    public static boolean isKnighthood(String rankLabel) {
        return "Knight".equals(rankLabel) || "Dame".equals(rankLabel);
    }

    private static String article(String title) {
        char first = Character.toLowerCase(title.charAt(0));
        return first == 'a' || first == 'e' || first == 'i' || first == 'o' || first == 'u' ? "an" : "a";
    }
}
