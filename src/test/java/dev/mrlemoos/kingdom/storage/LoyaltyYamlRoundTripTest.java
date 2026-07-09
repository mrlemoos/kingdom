package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LoyaltyYamlRoundTripTest {

    @Test
    void loyaltyTiersRoundTripUnderWarLoyaltySection() {
        UUID player = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Map<UUID, LoyaltyTier> tiers = new HashMap<>();
        tiers.put(player, LoyaltyTier.DOUBTFUL);

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeLoyalty(config, "loyalty", tiers);

        Map<UUID, LoyaltyTier> loaded = YamlKingdomStore.readLoyalty(config.getConfigurationSection("loyalty"));

        assertEquals(LoyaltyTier.DOUBTFUL, loaded.get(player));
    }

    @Test
    void missingLoyaltySectionLoadsEmpty() {
        Map<UUID, LoyaltyTier> loaded = YamlKingdomStore.readLoyalty(null);
        assertEquals(Map.of(), loaded);
    }

    @Test
    void faithfulTiersAreOmittedOnWrite() {
        UUID player = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Map<UUID, LoyaltyTier> tiers = Map.of(player, LoyaltyTier.FAITHFUL);

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeLoyalty(config, "loyalty", tiers);

        assertEquals(Map.of(), YamlKingdomStore.readLoyalty(config.getConfigurationSection("loyalty")));
    }
}
