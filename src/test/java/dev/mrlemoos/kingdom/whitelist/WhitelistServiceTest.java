package dev.mrlemoos.kingdom.whitelist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WhitelistServiceTest {

    private static final UUID APPLICANT = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private FakeServerWhitelistGateway gateway;
    private WhitelistService whitelistService;

    @BeforeEach
    void setUp() {
        gateway = new FakeServerWhitelistGateway();
        whitelistService = new WhitelistService(gateway);
    }

    @Test
    void monarchCanEnableWhitelistAndAddPlayer() {
        WhitelistResult enabled = whitelistService.setEnabled(NobleRank.KING, false, true);
        WhitelistResult added = whitelistService.allowPlayer(NobleRank.KING, false, APPLICANT);

        assertInstanceOf(WhitelistResult.Success.class, enabled);
        assertInstanceOf(WhitelistResult.Success.class, added);
        assertTrue(gateway.isEnabled());
        assertTrue(gateway.isWhitelisted(APPLICANT));
    }

    @Test
    void nonMonarchCannotManageWhitelist() {
        WhitelistResult result = whitelistService.setEnabled(NobleRank.KNIGHT, false, true);

        assertInstanceOf(WhitelistResult.Failure.class, result);
        assertFalse(gateway.isEnabled());
    }

    @Test
    void operatorCanManageWithoutNobleRank() {
        WhitelistResult result = whitelistService.allowPlayer(null, true, APPLICANT);

        assertInstanceOf(WhitelistResult.Success.class, result);
        assertTrue(gateway.isWhitelisted(APPLICANT));
    }

    @Test
    void cannotAddPlayerTwice() {
        whitelistService.allowPlayer(NobleRank.KING, false, APPLICANT);

        WhitelistResult result = whitelistService.allowPlayer(NobleRank.KING, false, APPLICANT);

        assertInstanceOf(WhitelistResult.Failure.class, result);
        assertEquals(1, gateway.whitelistedPlayerIds().size());
    }

    private static final class FakeServerWhitelistGateway implements ServerWhitelistGateway {

        private boolean enabled;
        private final Set<UUID> whitelisted = new HashSet<>();

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public boolean isWhitelisted(UUID playerId) {
            return whitelisted.contains(playerId);
        }

        @Override
        public void setWhitelisted(UUID playerId, boolean whitelistedFlag) {
            if (whitelistedFlag) {
                whitelisted.add(playerId);
            } else {
                whitelisted.remove(playerId);
            }
        }

        @Override
        public Set<UUID> whitelistedPlayerIds() {
            return new LinkedHashSet<>(whitelisted);
        }
    }
}
