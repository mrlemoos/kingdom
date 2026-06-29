package dev.mrlemoos.kingdom.locate;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructureType;

public final class LocateKeyParser {

    private LocateKeyParser() {
    }

    public static Optional<Structure> parseStructure(String raw) {
        String normalised = normalise(raw);
        Structure direct = LocateRegistries.structures().get(NamespacedKey.minecraft(normalised));
        if (direct != null) {
            return Optional.of(direct);
        }
        for (Structure structure : LocateRegistries.structures()) {
            if (structureKey(structure).equalsIgnoreCase(normalised)) {
                return Optional.of(structure);
            }
        }
        return structureAlias(normalised);
    }

    public static Optional<StructureType> parseStructureType(String raw) {
        String normalised = normalise(raw);
        StructureType direct = LocateRegistries.structureTypes().get(NamespacedKey.minecraft(normalised));
        if (direct != null) {
            return Optional.of(direct);
        }
        for (StructureType structureType : LocateRegistries.structureTypes()) {
            if (structureType.getKey().getKey().equalsIgnoreCase(normalised)) {
                return Optional.of(structureType);
            }
        }
        return structureTypeAlias(normalised);
    }

    public static Optional<Biome> parseBiome(String raw) {
        String normalised = normalise(raw);
        Biome direct = LocateRegistries.biomes().get(NamespacedKey.minecraft(normalised));
        if (direct != null) {
            return Optional.of(direct);
        }
        for (Biome biome : LocateRegistries.biomes()) {
            if (biome.getKey().getKey().equalsIgnoreCase(normalised)) {
                return Optional.of(biome);
            }
        }
        return Optional.empty();
    }

    public static String displayName(String raw) {
        String normalised = normalise(raw);
        return normalised.replace('_', ' ');
    }

    private static Optional<Structure> structureAlias(String normalised) {
        return switch (normalised) {
            case "village" -> Optional.of(Structure.VILLAGE_PLAINS);
            case "mansion", "woodland_mansion" -> Optional.of(Structure.MANSION);
            case "monument", "ocean_monument" -> Optional.of(Structure.MONUMENT);
            case "fortress", "nether_fortress" -> Optional.of(Structure.FORTRESS);
            case "stronghold" -> Optional.of(Structure.STRONGHOLD);
            case "desert_pyramid" -> Optional.of(Structure.DESERT_PYRAMID);
            case "jungle_pyramid", "jungle_temple" -> Optional.of(Structure.JUNGLE_PYRAMID);
            default -> Optional.empty();
        };
    }

    private static Optional<StructureType> structureTypeAlias(String normalised) {
        return switch (normalised) {
            case "jungle_temple" -> Optional.of(StructureType.JUNGLE_TEMPLE);
            case "woodland_mansion" -> Optional.of(StructureType.WOODLAND_MANSION);
            case "ocean_monument" -> Optional.of(StructureType.OCEAN_MONUMENT);
            default -> Optional.empty();
        };
    }

    private static String structureKey(Structure structure) {
        return LocateRegistries.structures().getKey(structure).getKey();
    }

    private static String normalise(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
