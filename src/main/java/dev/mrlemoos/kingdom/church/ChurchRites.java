package dev.mrlemoos.kingdom.church;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.church.gui.RiteConfirmGui;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.church.ChurchSite;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * The rites as they are held in the world: {@link ChurchService} decides, this gives back the
 * experience, pays the tithe, saves the realm and answers the people concerned. Both the rites window
 * at the cleric and the operators' {@code /kingdom church} commands go through here.
 */
public final class ChurchRites {

    private final KingdomService kingdomService;
    private final ChurchService churchService;
    private final EconomyService economyService;
    private final YamlKingdomStore store;
    private final ChurchConsentBook consentBook = new ChurchConsentBook();

    public ChurchRites(
            KingdomService kingdomService,
            ChurchService churchService,
            EconomyService economyService,
            YamlKingdomStore store) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.churchService = Objects.requireNonNull(churchService, "churchService");
        this.economyService = Objects.requireNonNull(economyService, "economyService");
        this.store = Objects.requireNonNull(store, "store");
    }

    public ChurchConsentBook consentBook() {
        return consentBook;
    }

    // --- rites held at once -----------------------------------------------

    public ChurchResult consecrate(String kingdomId, Celebrant celebrant) {
        ChurchResult result = churchService.consecrate(kingdomId, celebrant);
        saveOnSuccess(result);
        return result;
    }

    public ChurchResult annul(String kingdomId, NobleRank actorRank, UUID subject) {
        ChurchResult result = churchService.annul(kingdomId, actorRank, subject);
        saveOnSuccess(result);
        return result;
    }

    /** A member's funeral: half their held experience returned to them. */
    public FuneralOutcome funeral(String kingdomId, Celebrant celebrant, Player deceased) {
        FuneralOutcome outcome = churchService.funeral(kingdomId, celebrant, deceased.getUniqueId());
        if (outcome.result() instanceof ChurchResult.Success) {
            deceased.giveExp(outcome.experience());
            deceased.sendMessage(c("&aThe rites return " + outcome.experience() + " experience to you."));
            store.saveFrom(kingdomService);
        }
        return outcome;
    }

    /** Buries the villager that has waited longest; empty when none awaits its rites. */
    public Optional<VillagerFuneralOutcome> villagerFuneral(String kingdomId, Celebrant celebrant) {
        Optional<UUID> longestWaiting = churchService.nextVillagerAwaitingRites(kingdomId);
        if (longestWaiting.isEmpty()) {
            return Optional.empty();
        }
        VillagerFuneralOutcome outcome = churchService.villagerFuneral(kingdomId, celebrant, longestWaiting.get());
        if (outcome.result() instanceof ChurchResult.Success) {
            economyService.creditTreasury(kingdomId, outcome.treasuryShare());
            Optional<UUID> priest = churchService.priest(kingdomId);
            if (outcome.titheToTreasury() || priest.isEmpty()) {
                economyService.creditTreasury(kingdomId, outcome.tithe());
            } else {
                // The tithe is the priest's living, not a fee for whoever asked for the rite.
                economyService.creditWalletDirect(priest.get(), outcome.tithe());
            }
            store.saveFrom(kingdomService);
        }
        return Optional.of(outcome);
    }

    // --- rites that wait on consent ---------------------------------------

    /**
     * Asks {@code answerer} to consent to a marriage or divorce with {@code proposer}: their consent
     * window opens and the boss bar counts the minute down.
     *
     * @return the refusal, or empty when the request stands
     */
    public Optional<String> propose(
            ChurchConsentBook.Kind kind, String kingdomId, Player proposer, Player answerer) {
        Optional<String> refusal = consentBook.propose(
                kind, kingdomId, proposer.getUniqueId(), answerer.getUniqueId(), System.currentTimeMillis());
        if (refusal.isPresent()) {
            return refusal;
        }
        proposer.sendMessage(c("&a" + (kind == ChurchConsentBook.Kind.MARRIAGE
                        ? "Your offer of marriage stands. "
                        : "Your request for a divorce stands. ")
                + answerer.getName() + " has a minute to answer."));
        answerer.sendMessage(c("&6[Church] &f" + proposer.getName() + (kind == ChurchConsentBook.Kind.MARRIAGE
                ? " asks for your hand in marriage."
                : " asks the church to dissolve your marriage.")));
        openConsent(answerer);
        return Optional.empty();
    }

    /** Opens the Accept/Refuse window for whatever request waits on this player, if any. */
    public boolean openConsent(Player answerer) {
        Optional<ChurchConsentBook.Request> pending =
                consentBook.pendingFor(answerer.getUniqueId(), System.currentTimeMillis());
        if (pending.isEmpty()) {
            return false;
        }
        ChurchConsentBook.Request request = pending.get();
        String asker = nameOf(request.proposer());
        List<String> lines = request.kind() == ChurchConsentBook.Kind.MARRIAGE
                ? List.of(asker + " asks for your hand in marriage.", "Stand at the church to answer.")
                : List.of(asker + " asks to dissolve your marriage.", "Stand at the church to answer.");
        answerer.openInventory(RiteConfirmGui.create(
                        RiteConfirmGui.Purpose.CONSENT, request.kingdomId(), request.proposer(), lines)
                .getInventory());
        return true;
    }

    /** The answer given in the consent window. */
    public void answer(Player answerer, boolean accept) {
        long now = System.currentTimeMillis();
        Optional<ChurchConsentBook.Request> pending = consentBook.pendingFor(answerer.getUniqueId(), now);
        if (pending.isEmpty()) {
            RealmFeedback.refuse(answerer, "That request has lapsed.");
            answerer.closeInventory();
            return;
        }
        ChurchConsentBook.Request request = pending.get();
        Player proposer = Bukkit.getPlayer(request.proposer());
        if (!accept) {
            consentBook.answer(answerer.getUniqueId(), now);
            answerer.closeInventory();
            answerer.sendMessage(c("&7You refused " + nameOf(request.proposer()) + "."));
            if (proposer != null) {
                String refusal = nameOf(answerer.getUniqueId()) + " refused your "
                        + (request.kind() == ChurchConsentBook.Kind.MARRIAGE ? "offer of marriage." : "request.");
                proposer.sendMessage(c("&c" + refusal));
                RealmFeedback.refuse(proposer, refusal);
            }
            return;
        }
        // Consent is given at the altar, with the other party still there: the request keeps
        // its minute while the answerer walks to it.
        String kingdomId = request.kingdomId();
        if (!ChurchPresence.atChurch(churchService, kingdomId, answerer)) {
            RealmFeedback.refuse(answerer, "Stand at the church to answer.");
            return;
        }
        if (proposer == null || !ChurchPresence.atChurch(churchService, kingdomId, proposer)) {
            RealmFeedback.refuse(answerer, "Both parties must stand at the church.");
            return;
        }
        consentBook.answer(answerer.getUniqueId(), now);
        answerer.closeInventory();
        Celebrant celebrant = ChurchPresence.presiding(churchService, kingdomId);
        if (request.kind() == ChurchConsentBook.Kind.MARRIAGE) {
            wed(kingdomId, celebrant, proposer, answerer);
        } else {
            divorce(kingdomId, celebrant, proposer, answerer);
        }
    }

    private void wed(String kingdomId, Celebrant celebrant, Player first, Player second) {
        ChurchResult result = churchService.wed(kingdomId, celebrant, first.getUniqueId(), second.getUniqueId());
        if (!(result instanceof ChurchResult.Success)) {
            refuseBoth(first, second, result.message());
            return;
        }
        store.saveFrom(kingdomService);
        RealmFeedback.milestone(List.of(first), "&6Wed", "&eYou are wed to " + second.getName());
        RealmFeedback.milestone(List.of(second), "&6Wed", "&eYou are wed to " + first.getName());
        RealmFeedback.success(churchLocation(kingdomId).orElse(second.getLocation()));
        RealmFeedback.kingdomMessage(kingdomService, kingdomId,
                "&6[Church] &f" + first.getName() + " and " + second.getName() + " are wed at the church.");
    }

    private void divorce(String kingdomId, Celebrant celebrant, Player first, Player second) {
        ChurchResult result = churchService.divorce(kingdomId, celebrant, first.getUniqueId());
        if (!(result instanceof ChurchResult.Success)) {
            refuseBoth(first, second, result.message());
            return;
        }
        store.saveFrom(kingdomService);
        String record = "&6[Church] &f" + result.message();
        first.sendMessage(c(record));
        second.sendMessage(c(record));
        RealmFeedback.success(churchLocation(kingdomId).orElse(second.getLocation()));
    }

    private static void refuseBoth(Player first, Player second, String refusal) {
        for (Player party : List.of(first, second)) {
            party.sendMessage(c("&c" + refusal));
            RealmFeedback.refuse(party, refusal);
        }
    }

    // --- plumbing ---------------------------------------------------------

    public Optional<Location> churchLocation(String kingdomId) {
        Optional<ChurchSite> site = churchService.church(kingdomId);
        if (site.isEmpty()) {
            return Optional.empty();
        }
        World world = Bukkit.getWorld(site.get().worldName());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, site.get().x(), site.get().y() + 1.0, site.get().z()));
    }

    private void saveOnSuccess(ChurchResult result) {
        if (result instanceof ChurchResult.Success) {
            store.saveFrom(kingdomService);
        }
    }

    public static String nameOf(UUID playerId) {
        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            return online.getName();
        }
        String name = Bukkit.getOfflinePlayer(playerId).getName();
        return name != null ? name : playerId.toString();
    }
}
