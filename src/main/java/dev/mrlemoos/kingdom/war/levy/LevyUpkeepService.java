package dev.mrlemoos.kingdom.war.levy;

import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The levy's daily reckoning: the wage bill charged on the treasury, the shortfall kept as arrears,
 * one public warning the first day the levy goes unpaid, morale lost by the unpaid every day
 * thereafter, and desertion off the standing roster for the soldier whose morale sinks to the floor.
 *
 * <p>Pure domain — the Bukkit layer supplies the treasury, the roster and the day, and reads the
 * outcome back out to the realm.
 */
public final class LevyUpkeepService {

    // Corona are settled to the penny; anything under that is not a debt worth keeping.
    private static final double SETTLED_EPSILON = 1.0e-6;

    private final LevyArrearsStore store;
    private final LevyUpkeepConfig config;
    private final MoraleService moraleService;
    private final LevyTreasury treasury;
    private final LevyRoster roster;

    public LevyUpkeepService(
            LevyArrearsStore store,
            LevyUpkeepConfig config,
            MoraleService moraleService,
            LevyTreasury treasury,
            LevyRoster roster) {
        this.store = Objects.requireNonNull(store, "store");
        this.config = Objects.requireNonNull(config, "config");
        this.moraleService = Objects.requireNonNull(moraleService, "moraleService");
        this.treasury = Objects.requireNonNull(treasury, "treasury");
        this.roster = Objects.requireNonNull(roster, "roster");
    }

    public LevyUpkeepConfig config() {
        return config;
    }

    public LevyArrearsStore store() {
        return store;
    }

    /** The arrears standing against a kingdom, nothing when the levy is paid up. */
    public double arrearsOf(String kingdomId) {
        Optional<LevyArrears> standing = store.find(kingdomId);
        return standing.isPresent() ? standing.get().amount() : 0.0;
    }

    /**
     * Settles one realm day's levy upkeep for a kingdom. {@code musteredAnswerers} are those who
     * answered the muster; any of them already kept on the standing roster is charged once, at the
     * standing rate. Conscripts are never charged.
     */
    public LevyDayOutcome settleDay(
            String kingdomId, Set<UUID> musteredAnswerers, SeasonProfile season, long realmDay) {
        if (!config.enabled()) {
            return LevyDayOutcome.nothing();
        }
        Set<UUID> standing = new LinkedHashSet<>(roster.standingRoster(kingdomId));
        Set<UUID> mustered = new LinkedHashSet<>(musteredAnswerers == null ? Set.of() : musteredAnswerers);
        mustered.removeAll(standing);

        LevyWageBill bill = LevyWageBill.reckon(standing.size(), mustered.size(), config, season);
        Optional<LevyArrears> previous = store.find(kingdomId);
        double carried = previous.isPresent() ? previous.get().amount() : 0.0;
        double owed = bill.total() + carried;
        if (owed <= SETTLED_EPSILON) {
            store.clear(kingdomId);
            return new LevyDayOutcome(bill.total(), 0.0, 0.0, false, List.of(), List.of(), List.of());
        }

        double paid = treasury.debit(kingdomId, owed);
        double shortfall = owed - paid;
        List<String> announcements = new ArrayList<>();

        if (shortfall <= SETTLED_EPSILON) {
            store.clear(kingdomId);
            if (carried > SETTLED_EPSILON) {
                announcements.add("The levy's arrears are settled. The men under arms are paid up once more.");
            }
            return new LevyDayOutcome(bill.total(), paid, 0.0, false, List.of(), List.of(), List.copyOf(announcements));
        }

        boolean alreadyWarned = previous.isPresent() && previous.get().warned();
        store.put(kingdomId, new LevyArrears(shortfall, realmDay, true));

        if (!alreadyWarned) {
            // The warning day: the realm is told once, and nobody loses heart for it yet.
            announcements.add("The levy stands unpaid: " + corona(shortfall)
                    + " Corona owed to those under arms. Find it by tomorrow, or morale will suffer.");
            return new LevyDayOutcome(
                    bill.total(), paid, shortfall, true, List.of(), List.of(), List.copyOf(announcements));
        }

        Set<UUID> unpaid = new LinkedHashSet<>(standing);
        unpaid.addAll(mustered);
        List<UUID> demoralised = new ArrayList<>();
        List<UUID> deserters = new ArrayList<>();
        for (UUID soldier : unpaid) {
            moraleService.recordUnpaidLevy(soldier);
            demoralised.add(soldier);
            Optional<MoraleTier> tier = moraleService.tierOf(soldier);
            if (tier.isPresent() && tier.get().ordinal() >= config.desertionFloorTier().ordinal()
                    && standing.contains(soldier)) {
                roster.desert(kingdomId, soldier);
                deserters.add(soldier);
            }
        }
        announcements.add("The levy remains unpaid: " + corona(shortfall)
                + " Corona stand in arrears, and the men are losing heart.");
        return new LevyDayOutcome(
                bill.total(),
                paid,
                shortfall,
                false,
                List.copyOf(demoralised),
                List.copyOf(deserters),
                List.copyOf(announcements));
    }

    private static String corona(double amount) {
        return String.format(Locale.UK, "%.2f", amount);
    }
}
