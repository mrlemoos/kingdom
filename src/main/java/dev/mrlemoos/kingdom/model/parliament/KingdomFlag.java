package dev.mrlemoos.kingdom.model.parliament;

import dev.mrlemoos.kingdom.parliament.RoyalStandard;
import java.util.List;
import java.util.Objects;

/**
 * The kingdom flag: base banner colour plus loom patterns, flown as the Royal Standard beside the
 * Lords.
 */
public record KingdomFlag(String baseMaterial, List<Layer> layers) {

    /** One loom layer on the flag. */
    public record Layer(String patternId, String colour) {
        public Layer {
            Objects.requireNonNull(patternId, "patternId");
            Objects.requireNonNull(colour, "colour");
        }
    }

    public KingdomFlag {
        Objects.requireNonNull(baseMaterial, "baseMaterial");
        layers = List.copyOf(layers == null ? List.of() : layers);
    }

    public static KingdomFlag plain(String baseMaterial) {
        return new KingdomFlag(baseMaterial, List.of());
    }

    /** Default when Lords is first set with no banner in hand: Crown gold. */
    public static KingdomFlag crownDefault() {
        return plain(RoyalStandard.crownBannerMaterial());
    }
}
