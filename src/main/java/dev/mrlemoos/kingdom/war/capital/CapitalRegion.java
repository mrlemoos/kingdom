package dev.mrlemoos.kingdom.war.capital;

/**
 * A kingdom's designated {@code Capital} (see the glossary entry in {@code CONTEXT.md}): a
 * WorldGuard subregion id the monarch sets within linked territory, used for capital-fall war
 * aims. {@code worldName} is optional metadata for later WorldGuard lookups and is not validated
 * here — the Bukkit layer resolves the region against the kingdom's linked world when absent.
 */
public record CapitalRegion(String regionId, String worldName) {

    public CapitalRegion {
        if (regionId == null || regionId.isBlank()) {
            throw new IllegalArgumentException("regionId must not be blank");
        }
    }

    public CapitalRegion(String regionId) {
        this(regionId, null);
    }
}
