package dev.mrlemoos.kingdom.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.listener.SeasonalCropGrowthListener.GrowthVerdict;
import org.junit.jupiter.api.Test;

class SeasonalCropGrowthListenerTest {

    @Test
    void aNeutralSeasonNeverInterferes() {
        assertEquals(GrowthVerdict.NORMAL, SeasonalCropGrowthListener.decide(1.0, 0.0));
        assertEquals(GrowthVerdict.NORMAL, SeasonalCropGrowthListener.decide(1.0, 0.5));
        assertEquals(GrowthVerdict.NORMAL, SeasonalCropGrowthListener.decide(1.0, 0.999));
    }

    @Test
    void winterCancelsInProportionToTheShortfall() {
        assertEquals(GrowthVerdict.CANCEL, SeasonalCropGrowthListener.decide(0.5, 0.0));
        assertEquals(GrowthVerdict.CANCEL, SeasonalCropGrowthListener.decide(0.5, 0.499));
        assertEquals(GrowthVerdict.NORMAL, SeasonalCropGrowthListener.decide(0.5, 0.5));
        assertEquals(GrowthVerdict.NORMAL, SeasonalCropGrowthListener.decide(0.5, 0.9));
    }

    @Test
    void nothingGrowsWhenTheFactorIsNoneAtAll() {
        assertEquals(GrowthVerdict.CANCEL, SeasonalCropGrowthListener.decide(0.0, 0.0));
        assertEquals(GrowthVerdict.CANCEL, SeasonalCropGrowthListener.decide(0.0, 0.999));
        assertEquals(GrowthVerdict.CANCEL, SeasonalCropGrowthListener.decide(-1.0, 0.5));
    }

    @Test
    void summerBoostsInProportionToTheSurplus() {
        assertEquals(GrowthVerdict.BOOST, SeasonalCropGrowthListener.decide(1.25, 0.0));
        assertEquals(GrowthVerdict.BOOST, SeasonalCropGrowthListener.decide(1.25, 0.249));
        assertEquals(GrowthVerdict.NORMAL, SeasonalCropGrowthListener.decide(1.25, 0.25));
        assertEquals(GrowthVerdict.NORMAL, SeasonalCropGrowthListener.decide(1.25, 0.99));
    }

    @Test
    void aDoubledFactorAlwaysBoosts() {
        assertEquals(GrowthVerdict.BOOST, SeasonalCropGrowthListener.decide(2.0, 0.0));
        assertEquals(GrowthVerdict.BOOST, SeasonalCropGrowthListener.decide(2.0, 0.999));
        assertEquals(GrowthVerdict.BOOST, SeasonalCropGrowthListener.decide(3.0, 0.999));
    }
}
