package dev.mrlemoos.kingdom.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class CommandArgTokenizerTest {

    @Test
    void emptyInputProducesNoArgs() {
        assertArrayEquals(new String[0], CommandArgTokenizer.tokenize(null));
        assertArrayEquals(new String[0], CommandArgTokenizer.tokenize(""));
        assertArrayEquals(new String[0], CommandArgTokenizer.tokenize("   "));
    }

    @Test
    void tokenizeSplitsOnWhitespace() {
        assertArrayEquals(
                new String[] {"join", "avalon"},
                CommandArgTokenizer.tokenize("join avalon"));
        assertArrayEquals(
                new String[] {"create", "avalon", "Kingdom", "of", "Avalon"},
                CommandArgTokenizer.tokenize("create avalon Kingdom of Avalon"));
    }
}
