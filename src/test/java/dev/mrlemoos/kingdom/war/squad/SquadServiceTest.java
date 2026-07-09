package dev.mrlemoos.kingdom.war.squad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.conscription.ConscriptionConfig;
import dev.mrlemoos.kingdom.war.conscription.ConscriptionService;
import dev.mrlemoos.kingdom.war.conscription.InMemoryConscriptionStore;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadConfig;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadService;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Squad assignment and officer command (Phase 5, Slice 5.3): an officer who is a military
 * participant may be assigned a capped squad of rank-and-file, then command it idle/follow/attack
 * — a simple domain state machine, with no Bukkit pathfinding here. When the officer's morale
 * reaches Rout, every squad they command routs: pressed villagers are released back to the
 * villager economy and crown units are destroyed, per the war glossary's Squad rout entry.
 */
class SquadServiceTest {

    private static final String KINGDOM_ID = "northmarch";
    private static final UUID OFFICER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_OFFICER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID INELIGIBLE_OFFICER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID VILLAGER_ONE = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID VILLAGER_TWO = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID CROWN_UNIT_ONE = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private final Map<UUID, MoraleTier> moraleByOfficer = new HashMap<>();
    private Predicate<UUID> eligibility;
    private SquadService squadService;

    @BeforeEach
    void setUp() {
        moraleByOfficer.clear();
        moraleByOfficer.put(OFFICER, MoraleTier.STEADFAST);
        moraleByOfficer.put(OTHER_OFFICER, MoraleTier.STEADFAST);
        eligibility = officerId -> !officerId.equals(INELIGIBLE_OFFICER);
        squadService = new SquadService(
                new SquadConfig(true, 2, 1, 2),
                eligibility,
                officerId -> moraleByOfficer.getOrDefault(officerId, MoraleTier.STEADFAST));
    }

    private Set<SquadMember> oneVillager() {
        return Set.of(new SquadMember.PressedVillager(VILLAGER_ONE));
    }

    @Test
    void assigningASquadWhenDisabledFails() {
        SquadService disabled = new SquadService(SquadConfig.off(), eligibility, id -> MoraleTier.STEADFAST);

        WarResult result = disabled.assign(KINGDOM_ID, OFFICER, oneVillager());

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(disabled.squadsForOfficer(OFFICER).isEmpty());
    }

    @Test
    void assigningASquadOverTheMemberCapFails() {
        Set<SquadMember> tooMany =
                Set.of(new SquadMember.PressedVillager(VILLAGER_ONE), new SquadMember.PressedVillager(VILLAGER_TWO),
                        new SquadMember.CrownUnit(CROWN_UNIT_ONE));

        WarResult result = squadService.assign(KINGDOM_ID, OFFICER, tooMany);

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(squadService.squadsForOfficer(OFFICER).isEmpty());
    }

    @Test
    void assigningAnEmptySquadFails() {
        WarResult result = squadService.assign(KINGDOM_ID, OFFICER, Set.of());

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void anOfficerCannotExceedTheirPerOfficerSquadCap() {
        squadService.assign(KINGDOM_ID, OFFICER, oneVillager());

        WarResult result = squadService.assign(KINGDOM_ID, OFFICER, Set.of(new SquadMember.CrownUnit(CROWN_UNIT_ONE)));

        assertInstanceOf(WarResult.Failure.class, result);
        assertEquals(1, squadService.squadsForOfficer(OFFICER).size());
    }

    @Test
    void theKingdomSquadCountCapIsEnforcedAcrossOfficers() {
        squadService.assign(KINGDOM_ID, OFFICER, oneVillager());
        squadService.assign(KINGDOM_ID, OTHER_OFFICER, Set.of(new SquadMember.CrownUnit(CROWN_UNIT_ONE)));
        UUID thirdOfficer = UUID.fromString("77777777-7777-7777-7777-777777777777");
        moraleByOfficer.put(thirdOfficer, MoraleTier.STEADFAST);

        WarResult result = squadService.assign(KINGDOM_ID, thirdOfficer, Set.of(new SquadMember.PressedVillager(VILLAGER_TWO)));

        assertInstanceOf(WarResult.Failure.class, result);
        assertEquals(2, squadService.squadsForKingdom(KINGDOM_ID).size());
    }

    @Test
    void aNonMilitaryParticipantOfficerCannotBeAssignedASquad() {
        WarResult result = squadService.assign(KINGDOM_ID, INELIGIBLE_OFFICER, oneVillager());

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(squadService.squadsForOfficer(INELIGIBLE_OFFICER).isEmpty());
    }

    @Test
    void anOfficerAlreadyAtRoutCannotBeAssignedANewSquad() {
        moraleByOfficer.put(OFFICER, MoraleTier.ROUT);

        WarResult result = squadService.assign(KINGDOM_ID, OFFICER, oneVillager());

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(squadService.squadsForOfficer(OFFICER).isEmpty());
    }

    @Test
    void assigningASquadSucceedsAndStartsIdle() {
        WarResult result = squadService.assign(KINGDOM_ID, OFFICER, oneVillager());

        assertInstanceOf(WarResult.Success.class, result);
        assertEquals(1, squadService.squadsForOfficer(OFFICER).size());
        Squad squad = squadService.squadsForOfficer(OFFICER).iterator().next();
        assertEquals(SquadState.IDLE, squad.state());
        assertEquals(KINGDOM_ID, squad.kingdomId());
        assertEquals(OFFICER, squad.officerId());
    }

    @Test
    void commandingAnUnknownSquadFails() {
        WarResult result = squadService.command(UUID.randomUUID(), SquadState.FOLLOW);

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void commandingASquadToFollowThenAttackThenIdleTransitionsState() {
        squadService.assign(KINGDOM_ID, OFFICER, oneVillager());
        UUID squadId = squadService.squadsForOfficer(OFFICER).iterator().next().id();

        WarResult followResult = squadService.command(squadId, SquadState.FOLLOW);
        assertInstanceOf(WarResult.Success.class, followResult);
        assertEquals(SquadState.FOLLOW, squadService.find(squadId).orElseThrow().state());

        WarResult attackResult = squadService.command(squadId, SquadState.ATTACK);
        assertInstanceOf(WarResult.Success.class, attackResult);
        assertEquals(SquadState.ATTACK, squadService.find(squadId).orElseThrow().state());

        WarResult idleResult = squadService.command(squadId, SquadState.IDLE);
        assertInstanceOf(WarResult.Success.class, idleResult);
        assertEquals(SquadState.IDLE, squadService.find(squadId).orElseThrow().state());
    }

    @Test
    void commandingASquadDirectlyIntoRoutedIsRejected() {
        squadService.assign(KINGDOM_ID, OFFICER, oneVillager());
        UUID squadId = squadService.squadsForOfficer(OFFICER).iterator().next().id();

        WarResult result = squadService.command(squadId, SquadState.ROUTED);

        assertInstanceOf(WarResult.Failure.class, result);
        assertEquals(SquadState.IDLE, squadService.find(squadId).orElseThrow().state());
    }

    @Test
    void whenDisabledCommandingFails() {
        squadService.assign(KINGDOM_ID, OFFICER, oneVillager());
        UUID squadId = squadService.squadsForOfficer(OFFICER).iterator().next().id();
        SquadService disabled = new SquadService(SquadConfig.off(), eligibility, id -> MoraleTier.STEADFAST);

        WarResult result = disabled.command(squadId, SquadState.FOLLOW);

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void officerReachingRoutRoutsEveryAssignedSquadReleasingPressedVillagersAndDestroyingCrownUnits() {
        KingdomService kingdomService = new KingdomService();
        kingdomService.createKingdom(KINGDOM_ID, "Northmarch");
        ConscriptionService conscriptionService =
                new ConscriptionService(kingdomService, new InMemoryConscriptionStore(), new ConscriptionConfig(true, 16));
        conscriptionService.press(KINGDOM_ID, VILLAGER_ONE);
        EconomyService economyService = new EconomyService();
        economyService.creditTreasury(KINGDOM_ID, 500.0);
        economyService.enactBudget(KINGDOM_ID, 500.0);
        CrownSquadService crownSquadService =
                new CrownSquadService(economyService, new CrownSquadConfig(true, 50.0, 4), () -> CROWN_UNIT_ONE);
        crownSquadService.purchase(KINGDOM_ID);
        squadService.setConscriptionService(conscriptionService);
        squadService.setCrownSquadService(crownSquadService);
        squadService.assign(
                KINGDOM_ID, OFFICER, Set.of(new SquadMember.PressedVillager(VILLAGER_ONE), new SquadMember.CrownUnit(CROWN_UNIT_ONE)));
        UUID squadId = squadService.squadsForOfficer(OFFICER).iterator().next().id();

        moraleByOfficer.put(OFFICER, MoraleTier.ROUT);
        int routedCount = squadService.tickOfficerMorale(OFFICER);

        assertEquals(1, routedCount);
        assertTrue(squadService.find(squadId).isEmpty());
        assertTrue(squadService.squadsForOfficer(OFFICER).isEmpty());
        assertFalse(conscriptionService.isPressed(VILLAGER_ONE));
        assertTrue(crownSquadService.unitsOf(KINGDOM_ID).isEmpty());
    }

    @Test
    void tickOfficerMoraleIsANoOpWhenTheOfficerIsNotAtRout() {
        squadService.assign(KINGDOM_ID, OFFICER, oneVillager());
        UUID squadId = squadService.squadsForOfficer(OFFICER).iterator().next().id();

        int routedCount = squadService.tickOfficerMorale(OFFICER);

        assertEquals(0, routedCount);
        assertTrue(squadService.find(squadId).isPresent());
    }

    @Test
    void commandingASquadWhoseOfficerHasReachedRoutFailsAndRemovesTheSquad() {
        squadService.assign(KINGDOM_ID, OFFICER, oneVillager());
        UUID squadId = squadService.squadsForOfficer(OFFICER).iterator().next().id();
        moraleByOfficer.put(OFFICER, MoraleTier.ROUT);

        WarResult result = squadService.command(squadId, SquadState.FOLLOW);

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(squadService.find(squadId).isEmpty());
    }

    @Test
    void routingWithoutOptionalHooksStillRemovesTheSquad() {
        SquadService bare = new SquadService(
                new SquadConfig(true, 2, 1, 2), eligibility, id -> moraleByOfficer.getOrDefault(id, MoraleTier.STEADFAST));
        bare.assign(KINGDOM_ID, OFFICER, oneVillager());
        UUID squadId = bare.squadsForOfficer(OFFICER).iterator().next().id();

        moraleByOfficer.put(OFFICER, MoraleTier.ROUT);
        int routedCount = bare.tickOfficerMorale(OFFICER);

        assertEquals(1, routedCount);
        assertTrue(bare.find(squadId).isEmpty());
    }

    @Test
    void aSteadfastOfficersFollowingSquadStaysFollowingAfterAMoralePolicyTick() {
        squadService.assign(KINGDOM_ID, OFFICER, oneVillager());
        UUID squadId = squadService.squadsForOfficer(OFFICER).iterator().next().id();
        squadService.command(squadId, SquadState.FOLLOW);

        boolean changed = squadService.applyMoralePolicy(squadId);

        assertFalse(changed);
        assertEquals(SquadState.FOLLOW, squadService.find(squadId).orElseThrow().state());
    }

    @Test
    void aShakenOfficersFollowingSquadHesitatesBackToIdleAfterAMoralePolicyTick() {
        squadService.assign(KINGDOM_ID, OFFICER, oneVillager());
        UUID squadId = squadService.squadsForOfficer(OFFICER).iterator().next().id();
        squadService.command(squadId, SquadState.FOLLOW);
        moraleByOfficer.put(OFFICER, MoraleTier.SHAKEN);

        boolean changed = squadService.applyMoralePolicy(squadId);

        assertTrue(changed);
        assertEquals(SquadState.IDLE, squadService.find(squadId).orElseThrow().state());
    }

    @Test
    void aBreakingOfficersAttackingSquadScattersBackToIdleAfterAMoralePolicyTick() {
        squadService.assign(KINGDOM_ID, OFFICER, oneVillager());
        UUID squadId = squadService.squadsForOfficer(OFFICER).iterator().next().id();
        squadService.command(squadId, SquadState.ATTACK);
        moraleByOfficer.put(OFFICER, MoraleTier.BREAKING);

        boolean changed = squadService.applyMoralePolicy(squadId);

        assertTrue(changed);
        assertEquals(SquadState.IDLE, squadService.find(squadId).orElseThrow().state());
    }

    @Test
    void aRoutOfficersSquadIsRoutedByAMoralePolicyTickReleasingPressedVillagers() {
        KingdomService kingdomService = new KingdomService();
        kingdomService.createKingdom(KINGDOM_ID, "Northmarch");
        ConscriptionService conscriptionService =
                new ConscriptionService(kingdomService, new InMemoryConscriptionStore(), new ConscriptionConfig(true, 16));
        conscriptionService.press(KINGDOM_ID, VILLAGER_ONE);
        squadService.setConscriptionService(conscriptionService);
        squadService.assign(KINGDOM_ID, OFFICER, oneVillager());
        UUID squadId = squadService.squadsForOfficer(OFFICER).iterator().next().id();
        moraleByOfficer.put(OFFICER, MoraleTier.ROUT);

        boolean changed = squadService.applyMoralePolicy(squadId);

        assertTrue(changed);
        assertTrue(squadService.find(squadId).isEmpty());
        assertFalse(conscriptionService.isPressed(VILLAGER_ONE));
    }

    @Test
    void applyMoralePolicyOnAnUnknownSquadReturnsFalse() {
        boolean changed = squadService.applyMoralePolicy(UUID.randomUUID());

        assertFalse(changed);
    }

    @Test
    void applyMoralePolicyIsANoOpWhenSquadsAreDisabled() {
        SquadService disabled = new SquadService(SquadConfig.off(), eligibility, id -> MoraleTier.SHAKEN);

        boolean changed = disabled.applyMoralePolicy(UUID.randomUUID());

        assertFalse(changed);
    }

    @Test
    void tickMoralePoliciesSweepsEveryAssignedSquadAndReturnsHowManyChanged() {
        squadService.assign(KINGDOM_ID, OFFICER, oneVillager());
        UUID followingSquadId = squadService.squadsForOfficer(OFFICER).iterator().next().id();
        squadService.command(followingSquadId, SquadState.FOLLOW);
        squadService.assign(KINGDOM_ID, OTHER_OFFICER, Set.of(new SquadMember.CrownUnit(CROWN_UNIT_ONE)));
        UUID steadfastSquadId = squadService.squadsForOfficer(OTHER_OFFICER).iterator().next().id();
        moraleByOfficer.put(OFFICER, MoraleTier.BREAKING);

        int changedCount = squadService.tickMoralePolicies();

        assertEquals(1, changedCount);
        assertEquals(SquadState.IDLE, squadService.find(followingSquadId).orElseThrow().state());
        assertEquals(SquadState.IDLE, squadService.find(steadfastSquadId).orElseThrow().state());
    }

    @Test
    void tickMoralePoliciesIsANoOpWhenSquadsAreDisabled() {
        SquadService disabled = new SquadService(SquadConfig.off(), eligibility, id -> MoraleTier.SHAKEN);

        int changedCount = disabled.tickMoralePolicies();

        assertEquals(0, changedCount);
    }
}
