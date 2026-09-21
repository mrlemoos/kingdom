package dev.mrlemoos.kingdom.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class SeasonProfileTest {

    @Test
    void winterAloneAsksTheHearthsToBurn() {
        assertTrue(SeasonProfile.defaults(Season.WINTER).hearthsRequired());
        assertFalse(SeasonProfile.defaults(Season.SPRING).hearthsRequired());
        assertFalse(SeasonProfile.defaults(Season.SUMMER).hearthsRequired());
        assertFalse(SeasonProfile.defaults(Season.AUTUMN).hearthsRequired());
    }

    @Test
    void winterIsHarshAndSummerKind() {
        SeasonProfile winter = SeasonProfile.defaults(Season.WINTER);
        SeasonProfile summer = SeasonProfile.defaults(Season.SUMMER);
        assertTrue(winter.cropGrowthFactor() < summer.cropGrowthFactor());
        assertTrue(winter.outdoorYieldFactor() < summer.outdoorYieldFactor());
        assertTrue(winter.hostileSpawnFactor() > summer.hostileSpawnFactor());
        assertTrue(winter.moraleRecoveryFactor() < summer.moraleRecoveryFactor());
        assertTrue(winter.standingLevyUpkeepFactor() > summer.standingLevyUpkeepFactor());
    }

    @Test
    void musteredMenAlwaysCostMoreThanTheStandingRoster() {
        for (Season season : Season.values()) {
            SeasonProfile profile = SeasonProfile.defaults(season);
            assertTrue(
                    profile.musteredLevyUpkeepFactor() > profile.standingLevyUpkeepFactor(),
                    season.displayName());
        }
    }

    @Test
    void indoorTradesYieldTheSameTheYearRound() {
        for (Season season : Season.values()) {
            assertEquals(1.0, SeasonProfile.defaults(season).indoorYieldFactor(), 1.0e-9, season.displayName());
        }
    }

    @Test
    void springIsTheNeutralYardstick() {
        SeasonProfile spring = SeasonProfile.defaults(Season.SPRING);
        assertEquals(1.0, spring.cropGrowthFactor(), 1.0e-9);
        assertEquals(1.0, spring.outdoorYieldFactor(), 1.0e-9);
        assertEquals(1.0, spring.moraleRecoveryFactor(), 1.0e-9);
    }

    @Test
    void winterKeepsTheSkyFromClearingAndSummerLetsItBe() {
        assertEquals(0.7, SeasonProfile.defaults(Season.WINTER).stormChance(), 1.0e-9);
        assertEquals(0.2, SeasonProfile.defaults(Season.AUTUMN).stormChance(), 1.0e-9);
        assertEquals(0.1, SeasonProfile.defaults(Season.SPRING).stormChance(), 1.0e-9);
        assertEquals(0.0, SeasonProfile.defaults(Season.SUMMER).stormChance(), 1.0e-9);
    }

    @Test
    void fromPluginConfigReadsTheSeasonKeySpace() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("season.winter.crop-growth-factor", 0.1);
        yaml.set("season.winter.outdoor-yield-factor", 0.2);
        yaml.set("season.winter.indoor-yield-factor", 0.3);
        yaml.set("season.winter.standing-levy-upkeep-factor", 3.0);
        yaml.set("season.winter.mustered-levy-upkeep-factor", 4.0);
        yaml.set("season.winter.hostile-spawn-factor", 2.5);
        yaml.set("season.winter.hearths-required", false);
        yaml.set("season.winter.morale-recovery-factor", 0.4);
        yaml.set("season.winter.storm-chance", 0.9);

        SeasonProfile winter = SeasonProfile.fromPluginConfig(yaml, Season.WINTER);

        assertEquals(0.1, winter.cropGrowthFactor(), 1.0e-9);
        assertEquals(0.2, winter.outdoorYieldFactor(), 1.0e-9);
        assertEquals(0.3, winter.indoorYieldFactor(), 1.0e-9);
        assertEquals(3.0, winter.standingLevyUpkeepFactor(), 1.0e-9);
        assertEquals(4.0, winter.musteredLevyUpkeepFactor(), 1.0e-9);
        assertEquals(2.5, winter.hostileSpawnFactor(), 1.0e-9);
        assertFalse(winter.hearthsRequired());
        assertEquals(0.4, winter.moraleRecoveryFactor(), 1.0e-9);
        assertEquals(0.9, winter.stormChance(), 1.0e-9);
    }

    @Test
    void fromPluginConfigFallsBackToTheSeasonDefaults() {
        SeasonProfile summer = SeasonProfile.fromPluginConfig(new YamlConfiguration(), Season.SUMMER);
        assertEquals(SeasonProfile.defaults(Season.SUMMER), summer);
    }
}
