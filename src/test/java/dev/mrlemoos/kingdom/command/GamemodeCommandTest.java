package dev.mrlemoos.kingdom.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.listener.CreativeVisibilityListener;
import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

class GamemodeCommandTest {

    @Test
    void parsesNamesAbbreviationsAndNumerals() {
        assertEquals(GameMode.SURVIVAL, GamemodeCommand.parse("0").orElseThrow());
        assertEquals(GameMode.CREATIVE, GamemodeCommand.parse("1").orElseThrow());
        assertEquals(GameMode.ADVENTURE, GamemodeCommand.parse("2").orElseThrow());
        assertEquals(GameMode.SPECTATOR, GamemodeCommand.parse("3").orElseThrow());
        assertEquals(GameMode.SURVIVAL, GamemodeCommand.parse("Survival").orElseThrow());
        assertEquals(GameMode.CREATIVE, GamemodeCommand.parse("c").orElseThrow());
        assertEquals(GameMode.ADVENTURE, GamemodeCommand.parse("a").orElseThrow());
        assertEquals(GameMode.SPECTATOR, GamemodeCommand.parse("sp").orElseThrow());
    }

    @Test
    void rejectsUnknownModes() {
        assertTrue(GamemodeCommand.parse("4").isEmpty());
        assertTrue(GamemodeCommand.parse("fly").isEmpty());
    }

    @Test
    void creativePlayersHiddenFromNonCreative() {
        assertTrue(!CreativeVisibilityListener.canSee(GameMode.SURVIVAL, GameMode.CREATIVE));
        assertTrue(!CreativeVisibilityListener.canSee(GameMode.ADVENTURE, GameMode.CREATIVE));
        assertTrue(CreativeVisibilityListener.canSee(GameMode.CREATIVE, GameMode.CREATIVE));
        assertTrue(CreativeVisibilityListener.canSee(GameMode.SURVIVAL, GameMode.SURVIVAL));
        assertTrue(CreativeVisibilityListener.canSee(GameMode.CREATIVE, GameMode.SURVIVAL));
    }
}
