package dev.mrlemoos.kingdom.granary;

import java.util.UUID;

/**
 * A villager standing in a kingdom's territory on a winter day, and whether it is one of those the
 * realm never lets go short of work whatever the stores hold — a seated villager MP, the
 * Premier villager, the villager Speaker, a Lord of the Treasury or the Town Crier. The spared still
 * go hungry and still yield less for it; they neither strike nor count towards a famine grievance.
 */
public record HungerSubject(UUID villagerId, boolean spared) {}
