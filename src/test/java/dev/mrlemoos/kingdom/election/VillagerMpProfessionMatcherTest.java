package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.entity.Villager;
import org.junit.jupiter.api.Test;

class VillagerMpProfessionMatcherTest {

    @Test
    void professionNameFromEnum_normalisesMinecraftKey() {
        assertEquals("farmer", VillagerMpProfessionMatcher.professionName(Villager.Profession.FARMER));
        assertEquals("none", VillagerMpProfessionMatcher.professionName(Villager.Profession.NONE));
    }

    @Test
    void ordinaryTerritoryNametag_usesEventProfessionNotStaleEntityState() {
        assertEquals(
                "Farmer",
                ProfessionConstituencyResolver.villagerProfessionNametag(
                        VillagerMpProfessionMatcher.professionName(Villager.Profession.FARMER)));
        assertEquals(
                "Commoner",
                ProfessionConstituencyResolver.villagerProfessionNametag(
                        VillagerMpProfessionMatcher.professionName(Villager.Profession.NONE)));
    }
}
