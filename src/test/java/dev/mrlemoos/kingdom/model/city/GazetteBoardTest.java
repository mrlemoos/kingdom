package dev.mrlemoos.kingdom.model.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GazetteBoardTest {

    private static final UUID AUTHOR = UUID.fromString("00000000-0000-0000-0000-000000000031");

    @Test
    void announcementBeyondCapDropsOldestAnnouncement() {
        List<GazettePost> posts = new ArrayList<>();
        for (int i = 0; i < GazetteBoard.ANNOUNCEMENT_CAP; i++) {
            posts = GazetteBoard.addPost(posts, announcement("a" + i, i));
        }

        posts = GazetteBoard.addPost(posts, announcement("overflow", 100));

        assertEquals(GazetteBoard.ANNOUNCEMENT_CAP, countKind(posts, GazettePostKind.ANNOUNCEMENT));
        assertEquals("overflow", posts.get(0).title());
        assertTrue(posts.stream().noneMatch(p -> p.title().equals("a0")));
        assertTrue(posts.stream().anyMatch(p -> p.title().equals("a1")));
    }

    @Test
    void decreesAreExemptFromAnnouncementCap() {
        List<GazettePost> posts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            posts = GazetteBoard.addPost(posts, decree("d" + i, i));
        }
        for (int i = 0; i < GazetteBoard.ANNOUNCEMENT_CAP; i++) {
            posts = GazetteBoard.addPost(posts, announcement("a" + i, 10 + i));
        }
        posts = GazetteBoard.addPost(posts, announcement("overflow", 200));
        posts = GazetteBoard.addPost(posts, decree("newest-decree", 201));

        assertEquals(GazetteBoard.ANNOUNCEMENT_CAP, countKind(posts, GazettePostKind.ANNOUNCEMENT));
        assertEquals(4, countKind(posts, GazettePostKind.DECREE));
        assertEquals("newest-decree", posts.get(0).title());
        assertTrue(posts.stream().anyMatch(p -> p.title().equals("d0")));
    }

    @Test
    void newestNReturnsNewestFirstUpToLimit() {
        List<GazettePost> posts = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            posts = GazetteBoard.addPost(posts, announcement("n" + i, i));
        }

        List<GazettePost> newest = GazetteBoard.newestN(posts, 5);

        assertEquals(5, newest.size());
        assertEquals("n6", newest.get(0).title());
        assertEquals("n2", newest.get(4).title());
    }

    @Test
    void newestNOfEmptyIsEmpty() {
        assertTrue(GazetteBoard.newestN(List.of(), 5).isEmpty());
    }

    private static GazettePost announcement(String title, long mcDay) {
        return new GazettePost(title, "body", AUTHOR, mcDay, GazettePostKind.ANNOUNCEMENT, Optional.empty());
    }

    private static GazettePost decree(String title, long mcDay) {
        return new GazettePost(title, "body", AUTHOR, mcDay, GazettePostKind.DECREE, Optional.empty());
    }

    private static int countKind(List<GazettePost> posts, GazettePostKind kind) {
        int count = 0;
        for (GazettePost post : posts) {
            if (post.kind() == kind) {
                count++;
            }
        }
        return count;
    }
}
