package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.model.parliament.KingdomFlag;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class KingdomFlagResolverTest {

    @Test
    void heldBannerBecomesTheKingdomFlag() {
        KingdomFlag held = KingdomFlag.plain("RED_BANNER");
        KingdomFlag existing = KingdomFlag.plain("BLUE_BANNER");

        assertEquals(held, KingdomFlagResolver.resolve(Optional.of(existing), Optional.of(held)));
    }

    @Test
    void emptyHandKeepsExistingFlag() {
        KingdomFlag existing = new KingdomFlag(
                "WHITE_BANNER", List.of(new KingdomFlag.Layer("minecraft:stripe_middle", "RED")));

        assertEquals(existing, KingdomFlagResolver.resolve(Optional.of(existing), Optional.empty()));
    }

    @Test
    void firstSetWithoutBannerUsesCrownOrange() {
        KingdomFlag resolved = KingdomFlagResolver.resolve(Optional.empty(), Optional.empty());

        assertEquals(KingdomFlag.crownDefault(), resolved);
        assertEquals("ORANGE_BANNER", resolved.baseMaterial());
    }
}
