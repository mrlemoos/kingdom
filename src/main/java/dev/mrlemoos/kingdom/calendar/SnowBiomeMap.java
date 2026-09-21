package dev.mrlemoos.kingdom.calendar;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * The injective pairing of a warm biome to a snowy counterpart, by which winter snows linked territory and the
 * thaw knows what to put back. Nothing is snapshotted. Two warm biomes sharing one snowy counterpart make the
 * inverse ambiguous, so the colliding pair is dropped and logged rather than snowing a region that cannot thaw.
 */
public record SnowBiomeMap(Map<String, String> toSnowy, Map<String, String> toWarm) {

    public SnowBiomeMap {
        toSnowy = Map.copyOf(toSnowy);
        toWarm = Map.copyOf(toWarm);
    }

    public static SnowBiomeMap empty() {
        return new SnowBiomeMap(Map.of(), Map.of());
    }

    public boolean isEmpty() {
        return toSnowy.isEmpty();
    }

    public Optional<String> freeze(String biomeKey) {
        return Optional.ofNullable(toSnowy.get(normalise(biomeKey)));
    }

    public Optional<String> thaw(String biomeKey) {
        return Optional.ofNullable(toWarm.get(normalise(biomeKey)));
    }

    public Optional<Biome> freeze(Biome biome) {
        if (biome == null) {
            return Optional.empty();
        }
        Optional<String> snowy = freeze(biome.getKey().getKey());
        if (snowy.isEmpty()) {
            return Optional.empty();
        }
        return lookupBiome(snowy.get());
    }

    public Optional<Biome> thaw(Biome biome) {
        if (biome == null) {
            return Optional.empty();
        }
        Optional<String> warm = thaw(biome.getKey().getKey());
        if (warm.isEmpty()) {
            return Optional.empty();
        }
        return lookupBiome(warm.get());
    }

    public static SnowBiomeMap fromPluginConfig(FileConfiguration config, Consumer<String> warn) {
        return fromPluginConfig(config, SnowBiomeMap::lookupRegistryName, warn);
    }

    public static SnowBiomeMap fromPluginConfig(
            FileConfiguration config, Function<String, Optional<String>> lookup, Consumer<String> warn) {
        return parse(readRaw(config), lookup, warn);
    }

    public static SnowBiomeMap parse(
            Map<String, String> raw, Function<String, Optional<String>> lookup, Consumer<String> warn) {
        Objects.requireNonNull(lookup, "lookup");
        Objects.requireNonNull(warn, "warn");
        if (raw == null || raw.isEmpty()) {
            return empty();
        }
        Map<String, String> freeze = new LinkedHashMap<>();
        Map<String, String> thaw = new LinkedHashMap<>();
        for (Map.Entry<String, String> pair : raw.entrySet()) {
            Optional<String> warm = resolveEnd(pair.getKey(), lookup, warn);
            Optional<String> snowy = resolveEnd(pair.getValue(), lookup, warn);
            if (warm.isEmpty() || snowy.isEmpty()) {
                continue;
            }
            String warmKey = warm.get();
            String snowyKey = snowy.get();
            if (thaw.containsKey(snowyKey)) {
                warn.accept("Snow biome-swap dropped colliding pair "
                        + warmKey
                        + " -> "
                        + snowyKey
                        + "; "
                        + snowyKey
                        + " already thaws to "
                        + thaw.get(snowyKey)
                        + ".");
                continue;
            }
            freeze.put(warmKey, snowyKey);
            thaw.put(snowyKey, warmKey);
        }
        if (freeze.isEmpty()) {
            return empty();
        }
        return new SnowBiomeMap(freeze, thaw);
    }

    private static Optional<String> resolveEnd(
            String raw, Function<String, Optional<String>> lookup, Consumer<String> warn) {
        String normalised = normalise(raw);
        if (normalised.isEmpty()) {
            warn.accept("Snow biome-swap dropped an empty biome name.");
            return Optional.empty();
        }
        Optional<String> resolved = lookup.apply(normalised);
        if (resolved == null || resolved.isEmpty()) {
            warn.accept("Snow biome-swap dropped unknown biome '" + normalised + "'.");
            return Optional.empty();
        }
        return Optional.of(normalise(resolved.get()));
    }

    private static Map<String, String> readRaw(FileConfiguration config) {
        if (config == null) {
            return Map.of();
        }
        ConfigurationSection section = config.getConfigurationSection("snow.biome-swap");
        if (section == null) {
            return Map.of();
        }
        Map<String, String> raw = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            if (value != null) {
                raw.put(key, value);
            }
        }
        return raw;
    }

    private static Optional<String> lookupRegistryName(String raw) {
        Optional<Biome> biome = lookupBiome(raw);
        if (biome.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(biome.get().getKey().getKey());
    }

    private static Optional<Biome> lookupBiome(String raw) {
        String normalised = normalise(raw);
        if (normalised.isEmpty()) {
            return Optional.empty();
        }
        try {
            Registry<Biome> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
            Biome biome = registry.get(NamespacedKey.minecraft(normalised));
            return biome == null ? Optional.empty() : Optional.of(biome);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static String normalise(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (trimmed.startsWith("minecraft:")) {
            return trimmed.substring("minecraft:".length());
        }
        return trimmed;
    }
}
