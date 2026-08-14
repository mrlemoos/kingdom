package dev.mrlemoos.kingdom.war.muster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.loyalty.InMemoryMoraleStore;
import dev.mrlemoos.kingdom.loyalty.MoraleConfig;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.WarService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Slice 4.7 military hook: serving a muster out to demobilisation is the act of service that
 * brings morale recovery forward. Refusing or ignoring it earns nothing.
 */
class MusterServiceCreditTest {

    private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private MusterService musterService() {
        KingdomService kingdomService = new KingdomService();
        return new MusterService(new WarService(kingdomService), kingdomService);
    }

    @Test
    void servingAMusterOutBringsMoraleRecoveryForward() {
        InMemoryMoraleStore store = new InMemoryMoraleStore();
        // Two days per tier so a one-day credit is observable.
        MoraleService morale = new MoraleService(store, new MoraleConfig(true, 2, 1));
        morale.recordSiegeHostileAction(PLAYER, true);
        morale.tickRecovery(PLAYER, 0L);

        MusterService muster = musterService();
        muster.setMoraleServiceCreditHook(morale, () -> 0L);
        muster.creditServedMuster(PLAYER);

        morale.tickRecovery(PLAYER, 1L);

        assertEquals(MoraleTier.STEADFAST, store.findTier(PLAYER).orElseThrow());
    }

    @Test
    void withoutTheHookNothingIsCredited() {
        InMemoryMoraleStore store = new InMemoryMoraleStore();
        MoraleService morale = new MoraleService(store, new MoraleConfig(true, 2, 1));
        morale.recordSiegeHostileAction(PLAYER, true);
        morale.tickRecovery(PLAYER, 0L);

        musterService().creditServedMuster(PLAYER);

        morale.tickRecovery(PLAYER, 1L);

        assertEquals(MoraleTier.SHAKEN, store.findTier(PLAYER).orElseThrow());
    }
}
