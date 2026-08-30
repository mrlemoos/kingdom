package dev.mrlemoos.kingdom.task;

import dev.mrlemoos.kingdom.church.Celebrant;
import dev.mrlemoos.kingdom.church.ChurchPresence;
import dev.mrlemoos.kingdom.church.ChurchResult;
import dev.mrlemoos.kingdom.church.ChurchService;
import dev.mrlemoos.kingdom.church.MassCeremony;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Calls each realm's mass on the day it falls due, and blesses every subject who comes to the altar
 * while it sits. Attendance is sampled rather than watched: a subject standing at the church is
 * blessed within a few seconds of arriving.
 */
public final class MassTask implements Runnable {

    public static final long DEFAULT_INTERVAL_TICKS = 100L;

    private final JavaPlugin plugin;
    private final KingdomService kingdomService;
    private final ChurchService churchService;
    private final MassCeremony ceremony;
    private final YamlKingdomStore store;

    public MassTask(
            JavaPlugin plugin,
            KingdomService kingdomService,
            ChurchService churchService,
            MassCeremony ceremony,
            YamlKingdomStore store) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.churchService = Objects.requireNonNull(churchService, "churchService");
        this.ceremony = Objects.requireNonNull(ceremony, "ceremony");
        this.store = Objects.requireNonNull(store, "store");
    }

    public void schedule(long intervalTicks) {
        long interval = intervalTicks > 0 ? intervalTicks : DEFAULT_INTERVAL_TICKS;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, interval, interval);
    }

    @Override
    public void run() {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            String kingdomId = kingdom.getId();
            if (!churchService.isConsecrated(kingdomId)) {
                continue;
            }
            Celebrant celebrant = ChurchPresence.presiding(churchService, kingdomId);
            if (celebrant == Celebrant.NONE) {
                continue;
            }
            if (churchService.massDue(kingdomId)
                    && churchService.callMass(kingdomId, celebrant) instanceof ChurchResult.Success) {
                ceremony.open(kingdom, celebrant);
                store.saveFrom(kingdomService);
            }
            if (!churchService.massInSession(kingdomId)) {
                continue;
            }
            for (Player member : RealmFeedback.onlineMembers(kingdomService, kingdomId)) {
                if (ChurchPresence.atChurch(churchService, kingdomId, member)
                        && churchService.attend(kingdomId, member.getUniqueId())
                                instanceof ChurchResult.Success) {
                    ceremony.bless(kingdom, celebrant, member);
                }
            }
        }
    }
}
