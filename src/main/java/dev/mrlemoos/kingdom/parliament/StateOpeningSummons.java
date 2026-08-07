package dev.mrlemoos.kingdom.parliament;

public enum StateOpeningSummons {
    /** The Crown has been summoned to open Parliament; business stays blocked until it does. */
    AWAITING_CROWN,
    /** No Lords chamber to gather in, so the session opened by royal commission instead. */
    COMMISSIONED,
    /** Parliament is already in session; nothing to open. */
    NOT_NEEDED
}
