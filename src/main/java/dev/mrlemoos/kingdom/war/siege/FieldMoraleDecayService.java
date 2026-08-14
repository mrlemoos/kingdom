package dev.mrlemoos.kingdom.war.siege;

import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * A hard season's toll on men kept in the field: every {@link SeasonProfile#siegeMoraleDecayDays()}
 * realm days, each of a kingdom's military participants in a war loses one step of morale, whatever
 * else does or does not happen to them. The loss goes down the same ladder as the unpaid levy's, so
 * an army both besieging and unpaid sinks from both causes and the squad policy and the desertion
 * floor need know nothing of the weather.
 *
 * <p>Pure domain but for the clock it keeps per soldier, which is memory-only as the participant
 * register itself is.
 */
public final class FieldMoraleDecayService {

    private final MoraleService moraleService;
    private final MilitaryParticipantRegistry registry;
    private final Map<UUID, Long> lastDecayDay = new HashMap<>();

    public FieldMoraleDecayService(MoraleService moraleService, MilitaryParticipantRegistry registry) {
        this.moraleService = Objects.requireNonNull(moraleService, "moraleService");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    /**
     * Wears down {@code kingdomId}'s men in {@code warId} for one realm day, returning those who
     * lost a step today. A season that asks nothing of them touches nobody and does not even start
     * their clocks.
     */
    public List<UUID> decayDay(String warId, String kingdomId, SeasonProfile season, long realmDay) {
        Objects.requireNonNull(warId, "warId");
        Objects.requireNonNull(kingdomId, "kingdomId");
        Objects.requireNonNull(season, "season");
        int decayDays = season.siegeMoraleDecayDays();
        if (decayDays <= 0) {
            return List.of();
        }
        List<UUID> worn = new ArrayList<>();
        for (UUID soldier : registry.participantsOf(warId, kingdomId)) {
            Optional<MoraleTier> tier = moraleService.tierOf(soldier);
            if (tier.isEmpty() || tier.get() == MoraleTier.ROUT) {
                lastDecayDay.remove(soldier);
                continue;
            }
            Long marked = lastDecayDay.get(soldier);
            OptionalLong last = marked == null ? OptionalLong.empty() : OptionalLong.of(marked.longValue());
            if (!FieldMoraleDecayPolicy.shouldDecay(decayDays, tier, last, realmDay)) {
                if (marked == null) {
                    lastDecayDay.put(soldier, Long.valueOf(realmDay));
                }
                continue;
            }
            moraleService.recordFieldAttrition(soldier);
            lastDecayDay.put(soldier, Long.valueOf(realmDay));
            worn.add(soldier);
        }
        return List.copyOf(worn);
    }

    /** Peace bill demobilisation: forgets the field clocks kept for a war's men. */
    public void forget(UUID soldier) {
        if (soldier != null) {
            lastDecayDay.remove(soldier);
        }
    }
}
