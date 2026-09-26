package dev.mrlemoos.kingdom.church;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.church.ChurchSite;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;

/**
 * Raising and clearing the church with its cleric, whichever road led there — the foundation stone or
 * the operators' command. The rules stay in {@link ChurchService}; this moves the cleric and saves.
 */
public final class ChurchSiting {

    private final KingdomService kingdomService;
    private final ChurchService churchService;
    private final ClericService clericService;
    private final YamlKingdomStore store;

    public ChurchSiting(
            KingdomService kingdomService,
            ChurchService churchService,
            ClericService clericService,
            YamlKingdomStore store) {
        this.kingdomService = kingdomService;
        this.churchService = churchService;
        this.clericService = clericService;
        this.store = store;
    }

    public ChurchResult site(String kingdomId, NobleRank actorRank, ChurchSite site) {
        ChurchResult result = churchService.setChurch(kingdomId, actorRank, site);
        if (result instanceof ChurchResult.Failure) {
            return result;
        }
        // Moving the church moves the cleric with it; reconcile alone would leave it at the old altar.
        if (churchService.clericWanted(kingdomId)) {
            kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> clericService.spawn(kingdom, site));
        }
        store.saveFrom(kingdomService);
        return result;
    }

    public ChurchResult clear(String kingdomId, NobleRank actorRank) {
        // Despawn first: clearing the church forgets the cleric's id, and a forgotten cleric
        // stands at the old altar forever. Same order as dismissing the Town Crier.
        kingdomService.getKingdom(kingdomId).ifPresent(clericService::despawn);
        ChurchResult result = churchService.clearChurch(kingdomId, actorRank);
        if (result instanceof ChurchResult.Success) {
            store.saveFrom(kingdomService);
        }
        return result;
    }
}
