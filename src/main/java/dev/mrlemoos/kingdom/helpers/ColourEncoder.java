package dev.mrlemoos.kingdom.helpers;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

public final class ColourEncoder {

  private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
  private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();

  private ColourEncoder() {
  }

  /**
   * A helper static function to translate the colour codes in a string to the new
   * colour codes
   * prefixed with '&' to the new colour codes prefixed with '§'.
   * 
   * @param text The text to translate.
   * @return The translated text.
   * @throws IllegalArgumentException if the text is null.
   */
  public static String c(@NotNull String text) {
    if (text == null) {
      throw new IllegalArgumentException("Text cannot be null");
    }
    String serialized = SECTION.serialize(AMPERSAND.deserialize(text));
    if (serialized.isEmpty() && text.indexOf('&') >= 0) {
      final char placeholder = '\u00B7';
      serialized = SECTION.serialize(AMPERSAND.deserialize(text + placeholder));
      if (serialized.endsWith(String.valueOf(placeholder))) {
        return serialized.substring(0, serialized.length() - 1);
      }
    }
    return serialized;
  }

  /** Strip legacy section-sign colour codes from {@code text}. */
  public static String strip(@NotNull String text) {
    if (text == null) {
      throw new IllegalArgumentException("Text cannot be null");
    }
    return PlainTextComponentSerializer.plainText().serialize(SECTION.deserialize(text));
  }
}
