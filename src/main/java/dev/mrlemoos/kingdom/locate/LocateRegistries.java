package dev.mrlemoos.kingdom.locate;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructureType;

public final class LocateRegistries {

    private LocateRegistries() {
    }

    public static Registry<Structure> structures() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE);
    }

    public static Registry<StructureType> structureTypes() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE_TYPE);
    }

    public static Registry<Biome> biomes() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
    }
}
