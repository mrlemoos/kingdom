package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.strip;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.loyalty.InMemoryMoraleStore;
import dev.mrlemoos.kingdom.loyalty.MoraleConfig;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.loyalty.gui.MoralePardonGui;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.police.PoliceConfig;
import dev.mrlemoos.kingdom.police.PoliceCourtService;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.service.KingdomService;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * The morale pardon as it is actually reached in game: a right-click on the judge at the court, and
 * a click on the roll it opens.
 */
class MoralePardonListenerTest {

    private static final String REALM = "northmarch";

    private ServerMock server;
    private JavaPlugin plugin;
    private KingdomService kingdomService;
    private InMemoryMoraleStore moraleStore;
    private MoraleService moraleService;
    private PoliceCourtService courtService;
    private MoralePardonListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        kingdomService = new KingdomService();
        kingdomService.createKingdom(REALM, "Northmarch");
        kingdomService.createKingdom("southmarch", "Southmarch");
        moraleStore = new InMemoryMoraleStore();
        moraleService = new MoraleService(moraleStore, MoraleConfig.enabled());
        courtService = new PoliceCourtService(plugin, kingdomService, new PoliceService(kingdomService, PoliceConfig.defaults()));
        listener = new MoralePardonListener(kingdomService, courtService, moraleService);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- the accept path ---------------------------------------------------

    @Test
    void aKnightRightClickingTheJudgeGetsTheRollOfTheRouted() {
        PlayerMock knight = member("Knight", NobleRank.KNIGHT);
        PlayerMock routed = member("Routed", null);
        moraleStore.putTier(routed.getUniqueId(), MoraleTier.ROUT);

        listener.onInteractJudge(interactWithJudge(knight, REALM));

        InventoryHolder holder = knight.getOpenInventory().getTopInventory().getHolder();
        assertInstanceOf(MoralePardonGui.class, holder);
        assertEquals(routed.getUniqueId(), ((MoralePardonGui) holder).subjectForSlot(0));
    }

    @Test
    void theCrownMayGrantThePardonToo() {
        PlayerMock queen = member("Queen", NobleRank.QUEEN);
        PlayerMock routed = member("Routed", null);
        moraleStore.putTier(routed.getUniqueId(), MoraleTier.ROUT);

        listener.openPardonRoll(queen, REALM);

        assertInstanceOf(MoralePardonGui.class, queen.getOpenInventory().getTopInventory().getHolder());
    }

    @Test
    void grantingThePardonRestoresTheSubjectToSteadfastAndTellsThemSo() {
        PlayerMock knight = member("Knight", NobleRank.KNIGHT);
        PlayerMock routed = member("Routed", null);
        moraleStore.putTier(routed.getUniqueId(), MoraleTier.ROUT);
        drain(knight);
        drain(routed);

        listener.grantPardon(knight, REALM, routed.getUniqueId());

        assertEquals(MoraleTier.STEADFAST, moraleService.tierOf(routed.getUniqueId()).orElseThrow());
        assertTrue(drain(knight).contains("pardoned"), "the knight is told");
        assertTrue(drain(routed).contains("morale pardon"), "the subject is told");
    }

    // --- the refuse paths --------------------------------------------------

    @Test
    void aSubjectWithoutTheRankIsRefusedByName() {
        PlayerMock duke = member("Duke", NobleRank.DUKE);
        PlayerMock routed = member("Routed", null);
        moraleStore.putTier(routed.getUniqueId(), MoraleTier.ROUT);
        drain(duke);

        listener.onInteractJudge(interactWithJudge(duke, REALM));

        assertTrue(
                drain(duke).contains("Only the Crown or a Knight may grant a morale pardon."),
                "the refusal names who may");
        assertNoGuiOpen(duke);
    }

    @Test
    void anUntitledCitizenIsRefusedTheSameWay() {
        PlayerMock citizen = member("Citizen", null);
        PlayerMock routed = member("Routed", null);
        moraleStore.putTier(routed.getUniqueId(), MoraleTier.ROUT);
        drain(citizen);

        listener.openPardonRoll(citizen, REALM);

        assertTrue(drain(citizen).contains("Only the Crown or a Knight may grant a morale pardon."));
        assertNoGuiOpen(citizen);
    }

    @Test
    void aKnightOfAnotherRealmIsTurnedAwayFromThisBench() {
        PlayerMock foreigner = server.addPlayer("Foreigner");
        kingdomService.joinKingdom(foreigner.getUniqueId(), "southmarch");
        kingdomService.assignTitle(foreigner.getUniqueId(), NobleRank.KNIGHT, TitleStyle.MASCULINE);
        drain(foreigner);

        listener.openPardonRoll(foreigner, REALM);

        assertTrue(drain(foreigner).contains("The judge hears only the subjects of this realm."));
        assertNoGuiOpen(foreigner);
    }

    @Test
    void aKnightIsRefusedWhenMilitaryMoraleIsSwitchedOff() {
        MoraleService off = new MoraleService(moraleStore, MoraleConfig.disabled());
        MoralePardonListener disabled = new MoralePardonListener(kingdomService, courtService, off);
        PlayerMock knight = member("Knight", NobleRank.KNIGHT);
        PlayerMock routed = member("Routed", null);
        moraleStore.putTier(routed.getUniqueId(), MoraleTier.ROUT);
        drain(knight);

        disabled.openPardonRoll(knight, REALM);

        assertTrue(drain(knight).contains("The realm keeps no muster roll"));
        assertNoGuiOpen(knight);

        disabled.grantPardon(knight, REALM, routed.getUniqueId());
        assertEquals(MoraleTier.ROUT, moraleStore.findTier(routed.getUniqueId()).orElseThrow());
    }

    @Test
    void aKnightIsRefusedWhenThereIsNothingToPardon() {
        PlayerMock knight = member("Knight", NobleRank.KNIGHT);
        PlayerMock steady = member("Steady", null);
        moraleStore.putTier(steady.getUniqueId(), MoraleTier.STEADFAST);
        member("Unsworn", null);
        drain(knight);

        listener.openPardonRoll(knight, REALM);

        assertTrue(drain(knight).contains("No soldier of the realm wants a pardon"));
        assertNoGuiOpen(knight);
    }

    @Test
    void aSteadfastSubjectIsNeverPardonedSoAClosedTrackIsNeverOpened() {
        PlayerMock knight = member("Knight", NobleRank.KNIGHT);
        PlayerMock unsworn = member("Unsworn", null);
        drain(knight);

        listener.grantPardon(knight, REALM, unsworn.getUniqueId());

        assertTrue(moraleService.tierOf(unsworn.getUniqueId()).isEmpty(), "the track stays closed");
        assertTrue(drain(knight).contains("nothing to pardon"));
    }

    @Test
    void aSubjectOfAnotherRealmIsNeverPardonedFromThisBench() {
        PlayerMock knight = member("Knight", NobleRank.KNIGHT);
        PlayerMock foreigner = server.addPlayer("Foreigner");
        kingdomService.joinKingdom(foreigner.getUniqueId(), "southmarch");
        moraleStore.putTier(foreigner.getUniqueId(), MoraleTier.ROUT);
        drain(knight);

        listener.grantPardon(knight, REALM, foreigner.getUniqueId());

        assertEquals(MoraleTier.ROUT, moraleStore.findTier(foreigner.getUniqueId()).orElseThrow());
        assertTrue(drain(knight).contains("The judge hears only the subjects of this realm."));
    }

    @Test
    void anOrdinaryVillagerIsNotABenchAndIsLeftAlone() {
        PlayerMock knight = member("Knight", NobleRank.KNIGHT);
        World world = server.addSimpleWorld("world");
        Villager passer = (Villager) world.spawnEntity(world.getSpawnLocation(), EntityType.VILLAGER);
        PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(knight, passer, EquipmentSlot.HAND);

        listener.onInteractJudge(event);

        assertFalse(event.isCancelled());
        assertNoGuiOpen(knight);
    }

    // --- helpers -----------------------------------------------------------

    private PlayerMock member(String name, NobleRank rank) {
        PlayerMock player = server.addPlayer(name);
        kingdomService.joinKingdom(player.getUniqueId(), REALM);
        if (rank != null) {
            kingdomService.assignTitle(player.getUniqueId(), rank, TitleStyle.MASCULINE);
        }
        return player;
    }

    /** A villager wearing the court service's own judge tags, as {@code ensureJudge} would leave it. */
    private PlayerInteractEntityEvent interactWithJudge(PlayerMock actor, String kingdomId) {
        World world = server.addSimpleWorld("world");
        Villager judge = (Villager) world.spawnEntity(world.getSpawnLocation(), EntityType.VILLAGER);
        judge.getPersistentDataContainer()
                .set(new NamespacedKey(plugin, "police_judge"), PersistentDataType.BYTE, (byte) 1);
        judge.getPersistentDataContainer()
                .set(new NamespacedKey(plugin, "police_kingdom"), PersistentDataType.STRING, kingdomId);
        assertTrue(courtService.isJudgeEntity(judge), "the tags must be the ones the court service reads");
        return new PlayerInteractEntityEvent(actor, judge, EquipmentSlot.HAND);
    }

    private static void assertNoGuiOpen(PlayerMock player) {
        org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
        assertFalse(top != null && top.getHolder() instanceof MoralePardonGui, "no roll should have opened");
    }

    private static String drain(PlayerMock player) {
        StringBuilder joined = new StringBuilder();
        String next;
        while ((next = player.nextMessage()) != null) {
            joined.append(next).append('\n');
        }
        return strip(joined.toString());
    }
}
