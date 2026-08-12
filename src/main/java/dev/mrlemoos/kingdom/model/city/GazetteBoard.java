package dev.mrlemoos.kingdom.model.city;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure Gazette list helpers: announcement retention and the Town Crier ticker slice. Newest posts
 * sit at the front of the list.
 */
public final class GazetteBoard {

    /** Announcements kept on the board; oldest drop when a new one is pinned. Decrees are exempt. */
    public static final int ANNOUNCEMENT_CAP = 20;

    /** How many posts the Town Crier cycles through above its head. */
    public static final int TICKER_SIZE = 5;

    private GazetteBoard() {}

    /**
     * Pins {@code post} as newest. When the announcement count would exceed
     * {@link #ANNOUNCEMENT_CAP}, the oldest announcement is dropped; decrees never count against
     * the cap and are never dropped by it.
     */
    public static List<GazettePost> addPost(List<GazettePost> current, GazettePost post) {
        List<GazettePost> next = new ArrayList<>();
        if (post != null) {
            next.add(post);
        }
        if (current != null) {
            next.addAll(current);
        }
        if (post == null || post.kind() != GazettePostKind.ANNOUNCEMENT) {
            return List.copyOf(next);
        }
        int announcements = 0;
        List<GazettePost> trimmed = new ArrayList<>(next.size());
        for (GazettePost entry : next) {
            if (entry.kind() == GazettePostKind.ANNOUNCEMENT) {
                announcements++;
                if (announcements > ANNOUNCEMENT_CAP) {
                    continue;
                }
            }
            trimmed.add(entry);
        }
        return List.copyOf(trimmed);
    }

    /** Newest {@code n} posts, or fewer when the board is short. */
    public static List<GazettePost> newestN(List<GazettePost> posts, int n) {
        if (posts == null || posts.isEmpty() || n <= 0) {
            return List.of();
        }
        int to = Math.min(n, posts.size());
        return List.copyOf(posts.subList(0, to));
    }
}
