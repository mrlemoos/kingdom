package dev.mrlemoos.kingdom.listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.service.KingdomService;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class SeasonalSnowListenerTest {

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @Test
    void winterStartsPrecipitationWhenSkyWasClear() {
        ServerMock server = MockBukkit.mock();
        World world = server.addSimpleWorld("world");
        KingdomService kingdoms = new KingdomService();
        RealmCalendarService calendar = new RealmCalendarService(kingdoms, () -> 270L);
        calendar.restore(0L, 0L);
        SeasonalSnowListener listener = new SeasonalSnowListener(MockBukkit.createMockPlugin(), calendar, kingdoms);

        assertFalse(world.hasStorm());
        listener.run();

        assertTrue(world.hasStorm());
        world.setStorm(false);
        listener.run();
        assertFalse(world.hasStorm());
    }

    @Test
    void aCalmSeasonNeverRefusesClearing() {
        assertFalse(SeasonalSnowListener.shouldRefuseClearing(0.0, 0.0));
        assertFalse(SeasonalSnowListener.shouldRefuseClearing(0.0, 0.999));
        assertFalse(SeasonalSnowListener.shouldRefuseClearing(-1.0, 0.5));
    }

    @Test
    void winterRefusesInProportionToStormChance() {
        assertTrue(SeasonalSnowListener.shouldRefuseClearing(0.7, 0.0));
        assertTrue(SeasonalSnowListener.shouldRefuseClearing(0.7, 0.699));
        assertFalse(SeasonalSnowListener.shouldRefuseClearing(0.7, 0.7));
        assertFalse(SeasonalSnowListener.shouldRefuseClearing(0.7, 0.99));
        assertTrue(SeasonalSnowListener.shouldRefuseClearing(0.2, 0.199));
        assertFalse(SeasonalSnowListener.shouldRefuseClearing(0.2, 0.2));
    }

    @Test
    void aCertainStormAlwaysRefuses() {
        assertTrue(SeasonalSnowListener.shouldRefuseClearing(1.0, 0.0));
        assertTrue(SeasonalSnowListener.shouldRefuseClearing(1.0, 0.999));
        assertTrue(SeasonalSnowListener.shouldRefuseClearing(2.0, 0.999));
    }
}
