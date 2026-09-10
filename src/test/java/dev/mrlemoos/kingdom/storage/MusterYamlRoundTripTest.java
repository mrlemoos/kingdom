package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.war.muster.MusterAnswer;
import dev.mrlemoos.kingdom.war.conscription.PressedVillager;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MusterYamlRoundTripTest {

    @Test
    void activeMusterRoundTripsAcrossRestart() {
        UUID answered = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID waiting = UUID.fromString("66666666-7777-8888-9999-aaaaaaaaaaaa");
        YamlConfiguration config = new YamlConfiguration();

        YamlKingdomStore.writeMuster(
                config, "muster", Map.of("war-1", Set.of(answered, waiting)),
                Map.of("war-1", Map.of(answered, MusterAnswer.ANSWERED)),
                Map.of(answered, MoraleTier.STEADFAST));

        assertEquals(Set.of(answered, waiting), YamlKingdomStore.readMusterEligible(config.getConfigurationSection("muster")).get("war-1"));
        assertEquals(MusterAnswer.ANSWERED, YamlKingdomStore.readMusterAnswers(config.getConfigurationSection("muster")).get("war-1").get(answered));
        assertEquals(MoraleTier.STEADFAST, YamlKingdomStore.readMusterMorale(config.getConfigurationSection("muster")).get(answered));
    }

    @Test
    void pressedVillagersRoundTripAcrossRestart() {
        UUID villager = UUID.fromString("bbbbbbbb-2222-3333-4444-555555555555");
        YamlConfiguration config = new YamlConfiguration();

        YamlKingdomStore.writePressedVillagers(
                config, "conscription", java.util.List.of(new PressedVillager("avalon", villager, 123L)));

        assertEquals(
                java.util.List.of(new PressedVillager("avalon", villager, 123L)),
                YamlKingdomStore.readPressedVillagers(config.getConfigurationSection("conscription")));
    }

    @Test
    void crownSquadLedgerRoundTripsAcrossRestart() {
        UUID unit = UUID.fromString("cccccccc-2222-3333-4444-555555555555");
        YamlConfiguration config = new YamlConfiguration();

        YamlKingdomStore.writeCrownSquads(
                config, "crown-squads", java.util.List.of(new CrownSquadUnit(unit, "avalon", 123L)));

        assertEquals(
                java.util.List.of(new CrownSquadUnit(unit, "avalon", 123L)),
                YamlKingdomStore.readCrownSquads(config.getConfigurationSection("crown-squads")));
    }
}
