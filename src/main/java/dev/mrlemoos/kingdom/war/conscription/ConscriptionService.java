package dev.mrlemoos.kingdom.war.conscription;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.WarResult;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Pressed villager conscription: a kingdom presses territory villagers into wartime service, up
 * to a configured per-kingdom {@link ConscriptionConfig#cap}. A pressed villager is removed from
 * the villager economy for the duration — see {@link #isEconomicallyActive} — and is returned on
 * demobilisation via {@link #release} or {@link #releaseAll}. A seated villager MP (see {@link
 * dev.mrlemoos.kingdom.model.election.KingdomElectionState#seatIndexForVillagerEntity}) is never
 * eligible to be pressed, since Commons office is never interrupted by conscription. Domain-only
 * — no Bukkit entity or PDC access; villagers are identified purely by entity {@code UUID}, the
 * same identity already used for villager wallets elsewhere in the economy domain.
 */
public final class ConscriptionService {

    private final KingdomService kingdomService;
    private final ConscriptionStore store;
    private final ConscriptionConfig config;
    private final Supplier<Long> clockMs;

    public ConscriptionService(KingdomService kingdomService, ConscriptionStore store, ConscriptionConfig config) {
        this(kingdomService, store, config, System::currentTimeMillis);
    }

    public ConscriptionService(
            KingdomService kingdomService,
            ConscriptionStore store,
            ConscriptionConfig config,
            Supplier<Long> clockMs) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.store = Objects.requireNonNull(store, "store");
        this.config = Objects.requireNonNull(config, "config");
        this.clockMs = Objects.requireNonNull(clockMs, "clockMs");
    }

    public ConscriptionConfig config() {
        return config;
    }

    /**
     * Presses {@code villagerId} into {@code kingdomId}'s wartime service. Fails when
     * conscription is disabled, the kingdom is unknown, the villager is already pressed
     * (anywhere), the villager is a seated villager MP, or the kingdom's press cap is already
     * full.
     */
    public WarResult press(String kingdomId, UUID villagerId) {
        Objects.requireNonNull(kingdomId, "kingdomId");
        Objects.requireNonNull(villagerId, "villagerId");
        if (!config.enabled()) {
            return WarResult.fail("Conscription is disabled.");
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return WarResult.fail("Unknown kingdom.");
        }
        String normalisedKingdomId = kingdom.get().getId();
        if (isPressed(villagerId)) {
            return WarResult.fail("That villager is already pressed into service.");
        }
        if (isSeatedVillagerMp(normalisedKingdomId, villagerId)) {
            return WarResult.fail("A seated villager MP cannot be pressed into service.");
        }
        if (pressedCount(normalisedKingdomId) >= config.cap()) {
            return WarResult.fail("The conscription cap is full (" + config.cap() + ").");
        }
        store.press(new PressedVillager(normalisedKingdomId, villagerId, clockMs.get()));
        return WarResult.ok("Villager pressed into service.");
    }

    /** Demobilises a single pressed villager, returning it to the villager economy. */
    public WarResult release(UUID villagerId) {
        Objects.requireNonNull(villagerId, "villagerId");
        if (!isPressed(villagerId)) {
            return WarResult.fail("That villager is not pressed into service.");
        }
        store.release(villagerId);
        return WarResult.ok("Villager demobilised and returned.");
    }

    /**
     * Peace bill demobilisation hook: releases every villager pressed by {@code kingdomId},
     * returning each to the villager economy. Other kingdoms' pressed villagers are untouched.
     * Returns the number of villagers released.
     */
    public int releaseAll(String kingdomId) {
        Objects.requireNonNull(kingdomId, "kingdomId");
        int released = 0;
        for (PressedVillager pressed : store.findByKingdom(kingdomId)) {
            store.release(pressed.villagerId());
            released++;
        }
        return released;
    }

    public boolean isPressed(UUID villagerId) {
        return store.find(villagerId).isPresent();
    }

    /**
     * Predicate for the villager economy's GDP tick (see {@code VillagerEconomyProcessor} /
     * {@code EconomyCoordinator}): a pressed villager is not economically active and must be
     * excluded from that kingdom's daily GDP credit while pressed.
     */
    public boolean isEconomicallyActive(UUID villagerId) {
        return !isPressed(villagerId);
    }

    /** Negation of {@link #isEconomicallyActive}, offered under the exclusion-flavoured name. */
    public boolean shouldExcludeFromGdp(UUID villagerId) {
        return isPressed(villagerId);
    }

    /**
     * Port used to keep seated villager MPs off the conscription levy: true when {@code
     * villagerId} currently occupies a villager Commons seat for {@code kingdomId}.
     */
    public boolean isSeatedVillagerMp(String kingdomId, UUID villagerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return false;
        }
        return kingdom.get().getElectionState().seatIndexForVillagerEntity(villagerId).isPresent();
    }

    public Set<UUID> pressedView(String kingdomId) {
        Objects.requireNonNull(kingdomId, "kingdomId");
        Set<UUID> ids = new LinkedHashSet<>();
        for (PressedVillager pressed : store.findByKingdom(kingdomId)) {
            ids.add(pressed.villagerId());
        }
        return ids;
    }

    public int pressedCount(String kingdomId) {
        return store.findByKingdom(kingdomId).size();
    }

    public int capRemaining(String kingdomId) {
        return Math.max(0, config.cap() - pressedCount(kingdomId));
    }
}
