package dev.mrlemoos.kingdom.display;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;

/**
 * Splices a noble prefix in front of the victim's name inside a vanilla death
 * message, leaving the rest of the wording (which varies per damage source)
 * exactly as the server wrote it.
 */
public final class DeathMessageTitler {

    private DeathMessageTitler() {
    }

    /**
     * Returns {@code message} with the first occurrence of {@code victimName}
     * preceded by {@code prefix}. An empty prefix or an absent name leaves the
     * message untouched.
     */
    public static Component titled(Component message, String victimName, Component prefix) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(victimName, "victimName");
        Objects.requireNonNull(prefix, "prefix");
        if (victimName.isEmpty() || Component.empty().equals(prefix)) {
            return message;
        }
        TextReplacementConfig replacement = TextReplacementConfig.builder()
                .matchLiteral(victimName)
                .once()
                .replacement(builder -> Component.text().append(prefix).append(builder.build()))
                .build();
        return message.replaceText(replacement);
    }
}
