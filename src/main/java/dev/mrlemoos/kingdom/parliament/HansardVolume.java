package dev.mrlemoos.kingdom.parliament;

import java.util.List;

/** One bound volume of Hansard, ready to be written out and shelved in the registrar. */
public record HansardVolume(int number, String title, List<String> pages) {

    public HansardVolume {
        pages = pages == null ? List.of() : List.copyOf(pages);
    }
}
