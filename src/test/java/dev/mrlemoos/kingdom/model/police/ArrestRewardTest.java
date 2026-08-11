package dev.mrlemoos.kingdom.model.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArrestRewardTest {

    private static final UUID POSTER = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @Test
    void postsPositiveAmountForPoster() {
        ArrestReward reward = new ArrestReward(POSTER, 25.0);

        assertEquals(POSTER, reward.posterId());
        assertEquals(25.0, reward.amount(), 1e-9);
    }

    @Test
    void topUpIncreasesPurse() {
        ArrestReward reward = new ArrestReward(POSTER, 10.0);

        reward.topUp(5.0);

        assertEquals(15.0, reward.amount(), 1e-9);
        assertEquals(POSTER, reward.posterId());
    }

    @Test
    void rejectsNonPositivePostOrTopUp() {
        assertThrows(IllegalArgumentException.class, () -> new ArrestReward(POSTER, 0));
        ArrestReward reward = new ArrestReward(POSTER, 1.0);
        assertThrows(IllegalArgumentException.class, () -> reward.topUp(-1.0));
    }
}
