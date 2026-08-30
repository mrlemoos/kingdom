package dev.mrlemoos.kingdom.economy.villager.merchant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Picks the Corona offers a villager currently stocks. The pick is deterministic for a given villager
 * and rotation window, so repeated refreshes within the window show the same shelf, and the shelf
 * changes when the window rolls over.
 */
public record CoronaMerchantStockRotation(int maxOffers, long periodMillis) {

    public static final int DEFAULT_MAX_OFFERS = 6;
    public static final long DEFAULT_PERIOD_MILLIS = TimeUnit.HOURS.toMillis(24);

    public CoronaMerchantStockRotation {
        if (periodMillis <= 0) {
            throw new IllegalArgumentException("Rotation period must be positive.");
        }
    }

    public static CoronaMerchantStockRotation defaults() {
        return new CoronaMerchantStockRotation(DEFAULT_MAX_OFFERS, DEFAULT_PERIOD_MILLIS);
    }

    public static CoronaMerchantStockRotation fromPluginConfig(FileConfiguration config) {
        if (config == null) {
            return defaults();
        }
        ConfigurationSection section = config.getConfigurationSection("economy.corona-merchant-rotation");
        if (section == null) {
            return defaults();
        }
        int maxOffers = section.getInt("max-offers", DEFAULT_MAX_OFFERS);
        double periodHours = section.getDouble("period-hours", 24.0);
        long periodMillis = periodHours <= 0 ? DEFAULT_PERIOD_MILLIS : (long) (periodHours * 3_600_000L);
        return new CoronaMerchantStockRotation(maxOffers, periodMillis);
    }

    public List<CoronaMerchantOffer> stock(UUID villagerId, List<CoronaMerchantOffer> offers) {
        return stock(villagerId, offers, System.currentTimeMillis());
    }

    public List<CoronaMerchantOffer> stock(UUID villagerId, List<CoronaMerchantOffer> offers, long nowMillis) {
        if (offers == null || offers.isEmpty() || maxOffers <= 0 || offers.size() <= maxOffers) {
            return offers == null ? List.of() : offers;
        }
        long window = Math.floorDiv(nowMillis, periodMillis);
        long seed = (villagerId == null ? 0L : villagerId.getMostSignificantBits() ^ villagerId.getLeastSignificantBits())
                * 31L
                + window;
        List<CoronaMerchantOffer> shuffled = new ArrayList<>(offers);
        Collections.shuffle(shuffled, new Random(seed));
        return List.copyOf(shuffled.subList(0, maxOffers));
    }
}
