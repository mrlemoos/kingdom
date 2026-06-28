package dev.mrlemoos.kingdom.mint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.model.MintLocation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TreasuryLordMintSelectorTest {

    @Test
    void selectsNearestMintInSameWorld() {
        List<MintLocation> mints = List.of(
                new MintLocation("world", 0, 64, 0),
                new MintLocation("world", 100, 64, 100),
                new MintLocation("other", 1, 64, 1));

        Optional<MintLocation> nearest = TreasuryLordMintSelector.nearestInWorld(mints, "world", 5.0, 64.0, 5.0);

        assertTrue(nearest.isPresent());
        assertEquals(0, nearest.get().x());
        assertEquals(0, nearest.get().z());
    }

    @Test
    void ignoresMintsInOtherWorlds() {
        List<MintLocation> mints = List.of(new MintLocation("nether", 0, 64, 0));

        Optional<MintLocation> nearest = TreasuryLordMintSelector.nearestInWorld(mints, "world", 0.0, 64.0, 0.0);

        assertTrue(nearest.isEmpty());
    }

    @Test
    void returnsEmptyWhenNoMints() {
        Optional<MintLocation> nearest =
                TreasuryLordMintSelector.nearestInWorld(List.of(), "world", 0.0, 64.0, 0.0);

        assertTrue(nearest.isEmpty());
    }

    @Test
    void findsMintByTreasuryLordUuid() {
        UUID lordId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        MintLocation mint = new MintLocation("world", 10, 64, 20, lordId.toString());
        List<MintLocation> mints = List.of(
                new MintLocation("world", 0, 64, 0),
                mint,
                new MintLocation("world", 100, 64, 100));

        Optional<MintLocation> found = TreasuryLordMintSelector.byLordUuid(mints, lordId);

        assertTrue(found.isPresent());
        assertEquals(10, found.get().x());
        assertEquals(20, found.get().z());
    }

    @Test
    void selectForDespawnPrefersAimedLordMint() {
        UUID aimedLord = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        MintLocation aimedMint = new MintLocation("world", 100, 64, 100, aimedLord.toString());
        List<MintLocation> mints = List.of(
                new MintLocation("world", 0, 64, 0),
                aimedMint);

        Optional<MintLocation> selected = TreasuryLordMintSelector.selectForDespawn(
                mints, "world", 5.0, 64.0, 5.0, Optional.of(aimedLord));

        assertTrue(selected.isPresent());
        assertEquals(100, selected.get().x());
    }

    @Test
    void selectForDespawnFallsBackToNearestWhenNoAimedLord() {
        List<MintLocation> mints = List.of(
                new MintLocation("world", 0, 64, 0),
                new MintLocation("world", 100, 64, 100));

        Optional<MintLocation> selected = TreasuryLordMintSelector.selectForDespawn(
                mints, "world", 5.0, 64.0, 5.0, Optional.empty());

        assertTrue(selected.isPresent());
        assertEquals(0, selected.get().x());
    }

    @Test
    void forLordAtPrefersStoredLordLinkThenNearestMint() {
        UUID lordId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        MintLocation linked = new MintLocation("world", 50, 64, 50, lordId.toString());
        List<MintLocation> mints = List.of(
                new MintLocation("world", 0, 64, 0),
                linked);

        Optional<MintLocation> byUuid = TreasuryLordMintSelector.forLordAt(
                mints, "world", 48.0, 64.0, 49.0, Optional.of(lordId));
        Optional<MintLocation> byPosition = TreasuryLordMintSelector.forLordAt(
                mints, "world", 1.0, 64.0, 1.0, Optional.empty());

        assertEquals(linked, byUuid.orElseThrow());
        assertEquals(0, byPosition.orElseThrow().x());
    }
}
