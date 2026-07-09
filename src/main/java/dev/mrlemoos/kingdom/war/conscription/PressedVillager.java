package dev.mrlemoos.kingdom.war.conscription;

import java.util.Objects;
import java.util.UUID;

/**
 * A territory villager pressed into wartime service for a kingdom via {@link ConscriptionService}.
 * While pressed, the villager is removed from the villager economy (see {@link
 * ConscriptionService#isEconomicallyActive}) until released on demobilisation. Identified by
 * entity {@code UUID}, the same identity already used for villager wallets elsewhere in the
 * economy domain — never a Bukkit type.
 */
public record PressedVillager(String kingdomId, UUID villagerId, long pressedAtMs) {

    public PressedVillager {
        Objects.requireNonNull(kingdomId, "kingdomId");
        Objects.requireNonNull(villagerId, "villagerId");
    }
}
