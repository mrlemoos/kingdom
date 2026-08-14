package dev.mrlemoos.kingdom.war.siege;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import dev.mrlemoos.kingdom.loyalty.InMemoryMoraleStore;
import dev.mrlemoos.kingdom.loyalty.MoraleConfig;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Field morale decay: men kept in the field through a hard season lose heart on a clock, whatever
 * else befalls them, and the loss lands on the same ladder as the unpaid levy's.
 */
class FieldMoraleDecayServiceTest {

    private static final String WAR = "war-1";
    private static final String KINGDOM = "northmarch";
    private static final UUID SOLDIER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final SeasonProfile WINTER = SeasonProfile.defaults(Season.WINTER);
    private static final SeasonProfile SUMMER = SeasonProfile.defaults(Season.SUMMER);

    private MoraleService moraleService;
    private MilitaryParticipantRegistry registry;
    private FieldMoraleDecayService service;

    @BeforeEach
    void setUp() {
        moraleService = new MoraleService(new InMemoryMoraleStore(), MoraleConfig.enabled());
        registry = new MilitaryParticipantRegistry();
        service = new FieldMoraleDecayService(moraleService, registry);
        registry.markParticipant(WAR, KINGDOM, SOLDIER, MilitaryParticipantReason.STANDING_ROSTER);
        moraleService.openTrack(SOLDIER);
    }

    @Test
    void theDecisionIsPureAndHoldsItsFireUntilTheClockIsRun() {
        assertFalse(FieldMoraleDecayPolicy.shouldDecay(
                2, Optional.of(MoraleTier.STEADFAST), OptionalLong.empty(), 10L));
        assertFalse(FieldMoraleDecayPolicy.shouldDecay(
                2, Optional.of(MoraleTier.STEADFAST), OptionalLong.of(10L), 11L));
        assertTrue(FieldMoraleDecayPolicy.shouldDecay(
                2, Optional.of(MoraleTier.STEADFAST), OptionalLong.of(10L), 12L));
        assertFalse(FieldMoraleDecayPolicy.shouldDecay(
                0, Optional.of(MoraleTier.STEADFAST), OptionalLong.of(10L), 99L));
        assertFalse(FieldMoraleDecayPolicy.shouldDecay(
                2, Optional.of(MoraleTier.ROUT), OptionalLong.of(10L), 99L));
        assertFalse(FieldMoraleDecayPolicy.shouldDecay(2, Optional.empty(), OptionalLong.of(10L), 99L));
    }

    @Test
    void winterAloneWearsTheMenDownAndDoesSoOnTheConfiguredClock() {
        assertTrue(WINTER.siegeMoraleDecayDays() > 0, "winter must bite");
        int days = WINTER.siegeMoraleDecayDays();

        assertEquals(List.of(), service.decayDay(WAR, KINGDOM, WINTER, 0L));
        for (long day = 1; day < days; day++) {
            assertEquals(List.of(), service.decayDay(WAR, KINGDOM, WINTER, day));
        }
        assertEquals(List.of(SOLDIER), service.decayDay(WAR, KINGDOM, WINTER, days));
        assertEquals(Optional.of(MoraleTier.SHAKEN), moraleService.tierOf(SOLDIER));
    }

    @Test
    void theKinderSeasonsLeaveTheMenAlone() {
        for (Season season : Season.values()) {
            if (season == Season.WINTER) {
                continue;
            }
            assertEquals(0, SeasonProfile.defaults(season).siegeMoraleDecayDays(), season.displayName());
        }
        for (long day = 0; day < 40; day++) {
            assertEquals(List.of(), service.decayDay(WAR, KINGDOM, SUMMER, day));
        }
        assertEquals(Optional.of(MoraleTier.STEADFAST), moraleService.tierOf(SOLDIER));
    }

    @Test
    void theFieldAndTheUnpaidLevyFallOnTheSameLadder() {
        int days = WINTER.siegeMoraleDecayDays();
        service.decayDay(WAR, KINGDOM, WINTER, 0L);
        service.decayDay(WAR, KINGDOM, WINTER, days);
        assertEquals(Optional.of(MoraleTier.SHAKEN), moraleService.tierOf(SOLDIER));

        moraleService.recordUnpaidLevy(SOLDIER);
        assertEquals(Optional.of(MoraleTier.BREAKING), moraleService.tierOf(SOLDIER));

        service.decayDay(WAR, KINGDOM, WINTER, 2L * days);
        assertEquals(Optional.of(MoraleTier.ROUT), moraleService.tierOf(SOLDIER));
    }

    @Test
    void aRoutedManIsNotWornDownAnyFurther() {
        moraleService.recordUnpaidLevy(SOLDIER);
        moraleService.recordUnpaidLevy(SOLDIER);
        moraleService.recordUnpaidLevy(SOLDIER);
        assertEquals(Optional.of(MoraleTier.ROUT), moraleService.tierOf(SOLDIER));

        int days = WINTER.siegeMoraleDecayDays();
        for (long day = 0; day <= 4L * days; day++) {
            assertEquals(List.of(), service.decayDay(WAR, KINGDOM, WINTER, day));
        }
        assertEquals(Optional.of(MoraleTier.ROUT), moraleService.tierOf(SOLDIER));
    }

    @Test
    void aDeserterOffTheRegisterIsLeftOutOfTheReckoning() {
        UUID gone = UUID.fromString("22222222-2222-2222-2222-222222222222");
        moraleService.openTrack(gone);
        int days = WINTER.siegeMoraleDecayDays();
        for (long day = 0; day <= 2L * days; day++) {
            service.decayDay(WAR, KINGDOM, WINTER, day);
        }
        assertEquals(Optional.of(MoraleTier.STEADFAST), moraleService.tierOf(gone));
    }
}
