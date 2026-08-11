package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.UUID;

/**
 * Vacates elected parliamentary offices when a convict is imprisoned — no resignation-letter
 * approval. Triggers the normal Premier election or Commons by-election.
 */
@FunctionalInterface
public interface ElectedOfficeVacator {

    void vacateOnPrison(String kingdomId, UUID convictId, NobleRank vacatedRank);
}
