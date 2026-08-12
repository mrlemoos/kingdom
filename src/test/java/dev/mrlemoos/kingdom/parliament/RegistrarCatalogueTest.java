package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RegistrarCatalogueTest {

    @Test
    void organisesParliamentBooksIntoActsThenHansard() {
        List<RegistrarCatalogue.Volume> volumes = RegistrarCatalogue.organise(List.of(
                new RegistrarCatalogue.ShelvedBook("Finance Act", "Parliament", List.of("page")),
                new RegistrarCatalogue.ShelvedBook("Hansard, Volume I", "Parliament", List.of("day 1")),
                new RegistrarCatalogue.ShelvedBook("Budget Act", "Parliament", List.of("cap")),
                new RegistrarCatalogue.ShelvedBook("Hansard, Volume II", "Parliament", List.of("day 2")),
                new RegistrarCatalogue.ShelvedBook("Diary", "Player", List.of("secret")),
                new RegistrarCatalogue.ShelvedBook("Unsigned", "", List.of("x"))));

        assertEquals(
                List.of(
                        new RegistrarCatalogue.Volume(
                                RegistrarCatalogue.Section.ACT, "Finance Act", List.of("page")),
                        new RegistrarCatalogue.Volume(
                                RegistrarCatalogue.Section.ACT, "Budget Act", List.of("cap")),
                        new RegistrarCatalogue.Volume(
                                RegistrarCatalogue.Section.HANSARD, "Hansard, Volume I", List.of("day 1")),
                        new RegistrarCatalogue.Volume(
                                RegistrarCatalogue.Section.HANSARD, "Hansard, Volume II", List.of("day 2"))),
                volumes);
    }
}
