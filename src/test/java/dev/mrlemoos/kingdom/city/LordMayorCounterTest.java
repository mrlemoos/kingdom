package dev.mrlemoos.kingdom.city;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.TitleStyle;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LordMayorCounterTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void theCrownOpensTheRegisterOnAPlainRightClick() {
        assertEquals(LordMayorCounter.Action.REGISTER, action(NobleRank.KING, "northmarch"));
        assertEquals(LordMayorCounter.Action.REGISTER, action(NobleRank.QUEEN, "northmarch"));
        assertEquals(LordMayorCounter.Action.REGISTER, action(NobleRank.PRINCE, "northmarch"));
    }

    @Test
    void aSubjectAppliesForAPermit() {
        PlayerMembership citizen = new PlayerMembership(PLAYER, "northmarch");
        assertEquals(
                LordMayorCounter.Action.APPLY,
                LordMayorCounter.action(Optional.of(citizen), "northmarch"));
    }

    @Test
    void anUnaffiliatedPlayerSwearsTheOath() {
        assertEquals(LordMayorCounter.Action.OATH, LordMayorCounter.action(Optional.empty(), "northmarch"));
    }

    @Test
    void aForeignPrinceDoesNotReadThisRegister() {
        assertEquals(LordMayorCounter.Action.FOREIGN_MEMBER, action(NobleRank.PRINCE, "southmarch"));
    }

    private static LordMayorCounter.Action action(NobleRank rank, String homeKingdom) {
        PlayerMembership membership = new PlayerMembership(PLAYER, homeKingdom);
        membership.assignTitle(rank, TitleStyle.MASCULINE);
        return LordMayorCounter.action(Optional.of(membership), "northmarch");
    }
}
