package dev.mrlemoos.kingdom.listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SeasonalSnowListenerTest {

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
