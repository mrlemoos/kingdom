package dev.mrlemoos.kingdom.parliament;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Pure catalogue layout for registrar written books: Acts, then Hansard. */
public final class RegistrarCatalogue {

    public static final String AUTHOR = "Parliament";

    public enum Section {
        ACT,
        HANSARD
    }

    public record ShelvedBook(String title, String author, List<String> pages) {
        public ShelvedBook {
            title = title == null ? "" : title;
            author = author == null ? "" : author;
            pages = pages == null ? List.of() : List.copyOf(pages);
        }
    }

    public record Volume(Section section, String title, List<String> pages) {
        public Volume {
            Objects.requireNonNull(section, "section");
            title = title == null ? "" : title;
            pages = pages == null ? List.of() : List.copyOf(pages);
        }
    }

    private RegistrarCatalogue() {}

    public static List<Volume> organise(List<ShelvedBook> books) {
        if (books == null || books.isEmpty()) {
            return List.of();
        }
        List<Volume> acts = new ArrayList<>();
        List<Volume> hansard = new ArrayList<>();
        for (ShelvedBook book : books) {
            if (book == null || !AUTHOR.equals(book.author())) {
                continue;
            }
            String title = book.title().isBlank() ? "Act" : book.title();
            if (isHansardTitle(title)) {
                hansard.add(new Volume(Section.HANSARD, title, book.pages()));
            } else {
                acts.add(new Volume(Section.ACT, title, book.pages()));
            }
        }
        List<Volume> ordered = new ArrayList<>(acts.size() + hansard.size());
        ordered.addAll(acts);
        ordered.addAll(hansard);
        return List.copyOf(ordered);
    }

    static boolean isHansardTitle(String title) {
        String normalised = title.trim().toLowerCase(Locale.ROOT);
        return normalised.startsWith("hansard");
    }
}
