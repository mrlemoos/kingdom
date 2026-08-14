package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.loyalty.RecoveryMark;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class RecoveryMarkYamlRoundTripTest {

    private static final UUID PLAYER = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void loyaltyMarksRoundTripUnderTheirOwnSection() {
        Map<UUID, RecoveryMark<LoyaltyTier>> marks = Map.of(PLAYER, new RecoveryMark<>(LoyaltyTier.DISLOYAL, 12L));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeLoyaltyMarks(config, "loyalty-clocks", marks);

        Map<UUID, RecoveryMark<LoyaltyTier>> loaded =
                YamlKingdomStore.readLoyaltyMarks(config.getConfigurationSection("loyalty-clocks"));

        assertEquals(new RecoveryMark<>(LoyaltyTier.DISLOYAL, 12L), loaded.get(PLAYER));
    }

    @Test
    void moraleMarksRoundTripUnderTheirOwnSection() {
        Map<UUID, RecoveryMark<MoraleTier>> marks = Map.of(PLAYER, new RecoveryMark<>(MoraleTier.BREAKING, 7L));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeMoraleMarks(config, "morale-clocks", marks);

        Map<UUID, RecoveryMark<MoraleTier>> loaded =
                YamlKingdomStore.readMoraleMarks(config.getConfigurationSection("morale-clocks"));

        assertEquals(new RecoveryMark<>(MoraleTier.BREAKING, 7L), loaded.get(PLAYER));
    }

    @Test
    void missingSectionLoadsEmpty() {
        assertEquals(Map.of(), YamlKingdomStore.readLoyaltyMarks(null));
        assertEquals(Map.of(), YamlKingdomStore.readMoraleMarks(null));
    }

    @Test
    void malformedEntriesAreSkipped() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("loyalty-clocks.not-a-uuid", "doubtful:4");
        config.set("loyalty-clocks." + PLAYER, "not-a-tier:4");

        assertTrue(YamlKingdomStore.readLoyaltyMarks(config.getConfigurationSection("loyalty-clocks")).isEmpty());
    }
}
