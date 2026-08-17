package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** What the granary asks of config: a farmer's day's wheat, and how much wheat makes a bale. */
class GranaryConfigTest {

    @Test
    void nineWheatMakeABaleUntilConfigSaysOtherwise() {
        assertEquals(9, GranaryConfig.defaults().wheatPerBale());
        assertEquals(3.0, GranaryConfig.defaults().wheatPerFarmerDay());
    }

    @Test
    void aBaleFeedsFourHeadsThroughAWinterDayUntilConfigSaysOtherwise() {
        assertEquals(4, GranaryConfig.defaults().headsPerHay());
    }

    @Test
    void configMovesTheRateTheBaleAndTheRation() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("granary.wheat-per-farmer-day", 5.5);
        config.set("granary.wheat-per-bale", 4);
        config.set("granary.heads-per-hay", 6);

        GranaryConfig granary = GranaryConfig.fromPluginConfig(config);

        assertEquals(5.5, granary.wheatPerFarmerDay());
        assertEquals(4, granary.wheatPerBale());
        assertEquals(6, granary.headsPerHay());
    }

    @Test
    void aMissingGranarySectionKeepsTheDefaults() {
        assertEquals(GranaryConfig.defaults(), GranaryConfig.fromPluginConfig(new YamlConfiguration()));
        assertEquals(GranaryConfig.defaults(), GranaryConfig.fromPluginConfig(null));
    }

    @Test
    void aBaleOfNoWheatAndAFarmerOfNegativeGrainAreRefused() {
        GranaryConfig granary = new GranaryConfig(-2.0, 0, 0);

        assertEquals(0.0, granary.wheatPerFarmerDay());
        assertEquals(1, granary.wheatPerBale());
        assertEquals(1, granary.headsPerHay());
    }

    @Test
    void theHungerRampRunsOneThreeAndSevenUntilConfigSaysOtherwise() {
        GranaryConfig defaults = GranaryConfig.defaults();

        assertEquals(1, defaults.hungerYieldDays());
        assertEquals(3, defaults.hungerStrikeDays());
        assertEquals(7, defaults.hungerStarveDays());
        assertEquals(0.5, defaults.hungerYieldFactor());
        assertEquals(1, defaults.famineTierSteps());
    }

    @Test
    void configMovesTheHungerRampAndTheFamineGrievance() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("granary.hunger-yield-factor", 0.25);
        config.set("granary.hunger-yield-days", 2);
        config.set("granary.hunger-strike-days", 5);
        config.set("granary.hunger-starve-days", 9);
        config.set("granary.famine-tier-steps", 2);

        GranaryConfig granary = GranaryConfig.fromPluginConfig(config);

        assertEquals(0.25, granary.hungerYieldFactor());
        assertEquals(2, granary.hungerYieldDays());
        assertEquals(5, granary.hungerStrikeDays());
        assertEquals(9, granary.hungerStarveDays());
        assertEquals(2, granary.famineTierSteps());
    }

    @Test
    void aNegativeRampIsRefused() {
        GranaryConfig granary = new GranaryConfig(3.0, 9, 4, -1.0, -1, -1, -1, -1);

        assertEquals(0.0, granary.hungerYieldFactor());
        assertEquals(0, granary.hungerYieldDays());
        assertEquals(0, granary.hungerStrikeDays());
        assertEquals(0, granary.hungerStarveDays());
        assertEquals(0, granary.famineTierSteps());
    }
}
