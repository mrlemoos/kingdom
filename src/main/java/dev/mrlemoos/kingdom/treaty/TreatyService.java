package dev.mrlemoos.kingdom.treaty;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.TreatyKind;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** One bilateral pact per kind. A proposal needs both realms' royal assent before it takes effect. */
public final class TreatyService {

    public static final int PROPOSAL_EXPIRY_MC_DAYS = 7;

    private final KingdomService kingdomService;
    private final java.util.function.LongSupplier mcDayClock;
    private final Map<Key, Treaty> treaties = new HashMap<>();

    public TreatyService(KingdomService kingdomService) {
        this(kingdomService, () -> 0L);
    }

    public TreatyService(KingdomService kingdomService, java.util.function.LongSupplier mcDayClock) {
        this.kingdomService = kingdomService;
        this.mcDayClock = mcDayClock != null ? mcDayClock : () -> 0L;
    }

    public TreatyResult propose(String kingdomId, String counterpartId, TreatyKind kind, long mcDay) {
        TreatyResult validation = validate(kingdomId, counterpartId, kind);
        if (validation instanceof TreatyResult.Failure failure) {
            return failure;
        }
        Key key = Key.of(kingdomId, counterpartId, kind);
        Treaty existing = treaties.get(key);
        if (existing != null && existing.active) {
            return TreatyResult.fail("That treaty is already active.");
        }
        if (existing == null || existing.expiresOnMcDay <= mcDay) {
            treaties.put(key, new Treaty(mcDay + PROPOSAL_EXPIRY_MC_DAYS));
        }
        return TreatyResult.ok("Treaty proposal is before both realms.");
    }

    public TreatyResult assent(String kingdomId, String counterpartId, TreatyKind kind, long mcDay) {
        TreatyResult validation = validate(kingdomId, counterpartId, kind);
        if (validation instanceof TreatyResult.Failure failure) {
            return failure;
        }
        Key key = Key.of(kingdomId, counterpartId, kind);
        Treaty treaty = treaties.get(key);
        if (treaty == null || (!treaty.active && treaty.expiresOnMcDay <= mcDay)) {
            treaty = new Treaty(mcDay + PROPOSAL_EXPIRY_MC_DAYS);
            treaties.put(key, treaty);
        }
        treaty.assentedBy.add(Kingdom.normaliseId(kingdomId));
        if (treaty.assentedBy.size() == 2) {
            treaty.active = true;
            return TreatyResult.ok("Treaty is now active.");
        }
        return TreatyResult.ok("Treaty awaits the counterpart Crown's assent.");
    }

    public TreatyResult assent(String kingdomId, String counterpartId, TreatyKind kind) {
        return assent(kingdomId, counterpartId, kind, mcDayClock.getAsLong());
    }

    public TreatyResult repeal(String kingdomId, String counterpartId, TreatyKind kind) {
        return repeal(kingdomId, counterpartId, kind, mcDayClock.getAsLong());
    }

    public TreatyResult repeal(String kingdomId, String counterpartId, TreatyKind kind, long mcDay) {
        Treaty treaty = treaties.get(Key.of(kingdomId, counterpartId, kind));
        if (treaty == null || !treaty.active) {
            return TreatyResult.fail("No active treaty exists.");
        }
        if (treaty.repealExpiresOnMcDay == Long.MAX_VALUE || treaty.repealExpiresOnMcDay <= mcDay) {
            treaty.repealAssentedBy.clear();
            treaty.repealExpiresOnMcDay = mcDay + PROPOSAL_EXPIRY_MC_DAYS;
        }
        treaty.repealAssentedBy.add(Kingdom.normaliseId(kingdomId));
        if (treaty.repealAssentedBy.size() == 2) {
            treaties.remove(Key.of(kingdomId, counterpartId, kind));
            return TreatyResult.ok("Treaty repealed.");
        }
        return TreatyResult.ok("Treaty repeal awaits the counterpart Crown's assent.");
    }

    public void expire(long mcDay) {
        treaties.entrySet().removeIf(entry -> !entry.getValue().active && entry.getValue().expiresOnMcDay <= mcDay);
        treaties.values().forEach(treaty -> {
            if (treaty.active && treaty.repealExpiresOnMcDay <= mcDay) {
                treaty.repealAssentedBy.clear();
                treaty.repealExpiresOnMcDay = Long.MAX_VALUE;
            }
        });
    }

    public boolean isActive(String kingdomId, String counterpartId, TreatyKind kind) {
        Treaty treaty = treaties.get(Key.of(kingdomId, counterpartId, kind));
        return treaty != null && treaty.active;
    }

    public boolean waivesTariff(String territoryKingdomId, String visitorKingdomId) {
        return isActive(territoryKingdomId, visitorKingdomId, TreatyKind.TRADE_PACT);
    }

    public Collection<TreatyState> treatiesView() {
        return treaties.entrySet().stream()
                .map(entry -> new TreatyState(
                        entry.getKey().first, entry.getKey().second, entry.getKey().kind,
                        entry.getValue().expiresOnMcDay, entry.getValue().assentedBy, entry.getValue().active,
                        entry.getValue().repealExpiresOnMcDay, entry.getValue().repealAssentedBy))
                .toList();
    }

    public void replaceAll(Collection<TreatyState> loaded) {
        treaties.clear();
        if (loaded == null) return;
        for (TreatyState state : loaded) {
            if (state == null) continue;
            Treaty treaty = new Treaty(state.expiresOnMcDay());
            treaty.assentedBy.addAll(state.assentedBy());
            treaty.active = state.active();
            treaty.repealExpiresOnMcDay = state.repealExpiresOnMcDay();
            treaty.repealAssentedBy.addAll(state.repealAssentedBy());
            treaties.put(Key.of(state.firstKingdomId(), state.secondKingdomId(), state.kind()), treaty);
        }
    }

    public TreatyResult validate(String kingdomId, String counterpartId, TreatyKind kind) {
        if (kingdomId == null || counterpartId == null || kind == null) {
            return TreatyResult.fail("Treaty kingdom and kind are required.");
        }
        String kingdom = Kingdom.normaliseId(kingdomId);
        String counterpart = Kingdom.normaliseId(counterpartId);
        if (kingdom.equals(counterpart)) {
            return TreatyResult.fail("A kingdom cannot make a treaty with itself.");
        }
        if (kingdomService.getKingdom(kingdom).isEmpty() || kingdomService.getKingdom(counterpart).isEmpty()) {
            return TreatyResult.fail("Unknown treaty kingdom.");
        }
        return TreatyResult.ok("Treaty pair valid.");
    }

    private record Key(String first, String second, TreatyKind kind) {
        static Key of(String first, String second, TreatyKind kind) {
            String normalFirst = Kingdom.normaliseId(first);
            String normalSecond = Kingdom.normaliseId(second);
            return normalFirst.compareTo(normalSecond) < 0
                    ? new Key(normalFirst, normalSecond, kind)
                    : new Key(normalSecond, normalFirst, kind);
        }
    }

    private static final class Treaty {
        private final long expiresOnMcDay;
        private final java.util.Set<String> assentedBy = new java.util.HashSet<>();
        private final java.util.Set<String> repealAssentedBy = new java.util.HashSet<>();
        private long repealExpiresOnMcDay = Long.MAX_VALUE;
        private boolean active;

        Treaty(long expiresOnMcDay) {
            this.expiresOnMcDay = expiresOnMcDay;
        }
    }
}
