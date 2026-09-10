package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.city.CapitalSitingPolicy;
import dev.mrlemoos.kingdom.city.CapitalSitingPolicy.Verdict;
import dev.mrlemoos.kingdom.city.CityResult;
import dev.mrlemoos.kingdom.city.CityService;
import dev.mrlemoos.kingdom.city.LordMayorService;
import dev.mrlemoos.kingdom.city.TownCrierService;
import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.city.CapitalLocation;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.war.capital.CapitalRegionBox;
import dev.mrlemoos.kingdom.war.capital.CapitalService;
import dev.mrlemoos.kingdom.war.capital.CapitalSubregionSiting;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

/** {@code /kingdom capital …} and {@code /kingdom permit …}: the Crown's city commands. */
public final class KingdomCityHandler {

    private final KingdomService kingdomService;
    private final CityService cityService;
    private final LordMayorService lordMayorService;
    private final TownCrierService townCrierService;
    private final KingdomTerritoryResolver territoryResolver;
    private final YamlKingdomStore store;
    private final CapitalService capitalService;

    public KingdomCityHandler(
            KingdomService kingdomService,
            CityService cityService,
            LordMayorService lordMayorService,
            TownCrierService townCrierService,
            KingdomTerritoryResolver territoryResolver,
            YamlKingdomStore store) {
        this(kingdomService, cityService, lordMayorService, townCrierService, territoryResolver, store, null);
    }

    public KingdomCityHandler(
            KingdomService kingdomService,
            CityService cityService,
            LordMayorService lordMayorService,
            TownCrierService townCrierService,
            KingdomTerritoryResolver territoryResolver,
            YamlKingdomStore store,
            CapitalService capitalService) {
        this.kingdomService = kingdomService;
        this.cityService = cityService;
        this.lordMayorService = lordMayorService;
        this.townCrierService = townCrierService;
        this.territoryResolver = territoryResolver;
        this.store = store;
        this.capitalService = capitalService;
    }

    public boolean handleCapital(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(capitalHelp());
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> handleCapitalSet(sender);
            case "clear" -> handleCapitalClear(sender);
            case "setregion" -> handleCapitalSetRegion(sender, args);
            case "clearregion" -> handleCapitalClearRegion(sender);
            default -> {
                sender.sendMessage(capitalHelp());
                yield true;
            }
        };
    }

    public boolean handlePermit(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(permitHelp());
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "grant" -> handlePermitGrant(sender, args);
            case "revoke" -> handlePermitRevoke(sender, args);
            default -> {
                sender.sendMessage(permitHelp());
                yield true;
            }
        };
    }

    private boolean handleCapitalSet(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players can site a capital."));
            return true;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            sender.sendMessage(error("You must join a kingdom first."));
            return true;
        }
        String kingdomId = membership.get().getKingdomId();
        Location location = player.getLocation();
        Optional<String> owner = territoryResolver.owningKingdomId(
                location.getWorld() == null ? "" : location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());

        Verdict verdict = CapitalSitingPolicy.evaluate(membership.get().getRank(), kingdomId, owner);
        if (verdict != Verdict.ALLOWED) {
            sender.sendMessage(error(CapitalSitingPolicy.refusalMessage(verdict)));
            return true;
        }

        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            sender.sendMessage(error("Unknown kingdom."));
            return true;
        }

        CapitalLocation capital = CapitalLocation.of(
                location.getWorld() == null ? "" : location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
        CityResult result = cityService.setCapital(kingdomId, membership.get().getRank(), capital);
        if (result instanceof CityResult.Failure failure) {
            sender.sendMessage(error(failure.message()));
            return true;
        }

        Optional<Wolf> mayor = lordMayorService.spawn(kingdom.get(), capital);
        // The Crier has no implicit home: siting a capital stands one there until it is dismissed.
        cityService.setTownCrierStand(kingdomId, membership.get().getRank(), capital);
        Optional<org.bukkit.entity.Villager> crier = townCrierService.spawnAtStand(kingdom.get());
        save();
        sender.sendMessage(success(result.message()));
        sender.sendMessage(mayor.isPresent()
                ? info("The Lord Mayor has taken up office at the city hall.")
                : error("The Lord Mayor could not be stood up; the capital's world is not loaded."));
        sender.sendMessage(crier.isPresent()
                ? info("The Town Crier has taken up the Gazette.")
                : error("The Town Crier could not be stood up; the capital's world is not loaded."));
        return true;
    }

    public boolean handleCrier(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(crierHelp());
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> handleCrierSet(sender);
            case "clear" -> handleCrierClear(sender);
            default -> {
                sender.sendMessage(crierHelp());
                yield true;
            }
        };
    }

    private boolean handleCrierSet(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players can site the Town Crier."));
            return true;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            sender.sendMessage(error("You must join a kingdom first."));
            return true;
        }
        String kingdomId = membership.get().getKingdomId();
        Location location = player.getLocation();
        Optional<String> owner = territoryResolver.owningKingdomId(
                location.getWorld() == null ? "" : location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());

        Verdict verdict = CapitalSitingPolicy.evaluate(membership.get().getRank(), kingdomId, owner);
        if (verdict != Verdict.ALLOWED) {
            String refusal = switch (verdict) {
                case NOT_THE_CROWN -> "Only the King or Queen may site the Town Crier.";
                case NO_KINGDOM -> "You must join a kingdom first.";
                case UNCLAIMED_LAND -> "The Town Crier must stand inside your kingdom's territory.";
                case FOREIGN_TERRITORY -> "You may not site the Town Crier in another realm's territory.";
                case ALLOWED -> "";
            };
            sender.sendMessage(error(refusal));
            return true;
        }

        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            sender.sendMessage(error("Unknown kingdom."));
            return true;
        }

        CapitalLocation stand = CapitalLocation.of(
                location.getWorld() == null ? "" : location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
        CityResult result = cityService.setTownCrierStand(kingdomId, membership.get().getRank(), stand);
        if (result instanceof CityResult.Failure failure) {
            sender.sendMessage(error(failure.message()));
            return true;
        }

        Optional<org.bukkit.entity.Villager> crier = townCrierService.spawnAtStand(kingdom.get());
        save();
        sender.sendMessage(success(result.message()));
        sender.sendMessage(crier.isPresent()
                ? info("The Town Crier now cries from here.")
                : error("The Town Crier could not be stood up; this world's chunks are not loaded."));
        return true;
    }

    private boolean handleCrierClear(CommandSender sender) {
        Optional<PlayerMembership> membership = requireCrown(sender);
        if (membership.isEmpty()) {
            return true;
        }
        String kingdomId = membership.get().getKingdomId();
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            sender.sendMessage(error("Unknown kingdom."));
            return true;
        }

        // Despawn first: dismissal needs the stand to find a crier whose chunk was unloaded.
        townCrierService.despawn(kingdom.get());
        CityResult result = cityService.clearTownCrierStand(kingdomId, membership.get().getRank());
        if (result instanceof CityResult.Failure failure) {
            sender.sendMessage(error(failure.message()));
            return true;
        }
        save();
        sender.sendMessage(success(result.message()));
        return true;
    }

    private boolean handleCapitalClear(CommandSender sender) {
        Optional<PlayerMembership> membership = requireCrown(sender);
        if (membership.isEmpty()) {
            return true;
        }
        String kingdomId = membership.get().getKingdomId();
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            sender.sendMessage(error("Unknown kingdom."));
            return true;
        }

        lordMayorService.despawn(kingdom.get());
        townCrierService.despawn(kingdom.get());
        CityResult result = cityService.clearCapital(kingdomId, membership.get().getRank());
        if (result instanceof CityResult.Failure failure) {
            sender.sendMessage(error(failure.message()));
            return true;
        }
        save();
        sender.sendMessage(success(result.message()));
        return true;
    }

    private boolean handlePermitGrant(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom permit grant <player>"));
            return true;
        }
        Optional<PlayerMembership> membership = requireMember(sender);
        if (membership.isEmpty()) {
            return true;
        }
        Optional<UUID> target = resolveTarget(sender, args[1]);
        if (target.isEmpty()) {
            return true;
        }

        CityResult result = cityService.grantPermit(
                membership.get().getKingdomId(), membership.get().getRank(), target.get());
        sender.sendMessage(format(result));
        if (result instanceof CityResult.Success) {
            save();
            Player holder = Bukkit.getPlayer(target.get());
            if (holder != null) {
                holder.sendMessage(success("You have been granted a build permit."));
            }
        }
        return true;
    }

    private boolean handlePermitRevoke(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom permit revoke <player>"));
            return true;
        }
        Optional<PlayerMembership> membership = requireMember(sender);
        if (membership.isEmpty()) {
            return true;
        }
        Optional<UUID> target = resolveTarget(sender, args[1]);
        if (target.isEmpty()) {
            return true;
        }

        CityResult result = cityService.revokePermit(
                membership.get().getKingdomId(), membership.get().getRank(), target.get());
        sender.sendMessage(format(result));
        if (result instanceof CityResult.Success) {
            save();
            // Told in chat if they are here to hear it; no letter and no login queue otherwise.
            Player holder = Bukkit.getPlayer(target.get());
            if (holder != null) {
                holder.sendMessage(error("Your build permit has been revoked."));
            }
        }
        return true;
    }

    private Optional<UUID> resolveTarget(CommandSender sender, String name) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(error("Unknown player."));
            return Optional.empty();
        }
        return Optional.of(target.getUniqueId());
    }

    /** A seat in a kingdom and nothing more; the rank gate is the service's business. */
    private Optional<PlayerMembership> requireMember(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players may use this command."));
            return Optional.empty();
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            sender.sendMessage(error("You must join a kingdom first."));
        }
        return membership;
    }

    private boolean handleCapitalSetRegion(CommandSender sender, String[] args) {
        if (capitalService == null) {
            sender.sendMessage(error("Capital-fall regions are not enabled."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom capital setregion <region>"));
            return true;
        }
        Optional<PlayerMembership> membership = requireCrown(sender);
        if (membership.isEmpty()) {
            return true;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(membership.get().getKingdomId());
        if (kingdom.isEmpty()) {
            sender.sendMessage(error("Unknown kingdom."));
            return true;
        }
        String worldName = kingdomService.resolveWorldName(kingdom.get());
        if (Bukkit.getWorld(worldName) == null) {
            sender.sendMessage(error("World '" + worldName + "' is not loaded."));
            return true;
        }
        String regionId = Kingdom.normaliseId(args[1]);
        String territoryRegion = kingdom.get().hasWorldGuardRegions() ? "linked territory" : null;
        boolean worldGuard = WorldGuardBridge.isAvailable();
        Optional<CapitalRegionBox> capitalBounds = boxOf(worldName, regionId);
        Optional<CapitalRegionBox> territoryBounds = worldGuard && capitalBounds.isPresent()
                ? kingdom.get().getWorldGuardRegions().stream()
                        .map(region -> boxOf(worldName, region))
                        .flatMap(Optional::stream)
                        .filter(bounds -> bounds.contains(capitalBounds.get()))
                        .findFirst()
                : Optional.empty();
        CapitalSubregionSiting.Verdict verdict = CapitalSubregionSiting.evaluateLink(
                membership.get().getRank(),
                worldGuard,
                territoryRegion,
                territoryBounds,
                capitalBounds);
        if (verdict != CapitalSubregionSiting.Verdict.ALLOWED) {
            sender.sendMessage(error(CapitalSubregionSiting.refusalMessage(verdict)));
            return true;
        }
        capitalService.setCapital(kingdom.get().getId(), regionId, worldName);
        save();
        sender.sendMessage(success(
                "Linked capital region " + regionId + " for capital-fall war aims in "
                        + kingdom.get().getDisplayName() + "."));
        return true;
    }

    private boolean handleCapitalClearRegion(CommandSender sender) {
        if (capitalService == null) {
            sender.sendMessage(error("Capital-fall regions are not enabled."));
            return true;
        }
        Optional<PlayerMembership> membership = requireCrown(sender);
        if (membership.isEmpty()) {
            return true;
        }
        String kingdomId = membership.get().getKingdomId();
        CapitalSubregionSiting.Verdict verdict =
                CapitalSubregionSiting.evaluateClear(membership.get().getRank(), capitalService.hasCapital(kingdomId));
        if (verdict != CapitalSubregionSiting.Verdict.ALLOWED) {
            sender.sendMessage(error(CapitalSubregionSiting.refusalMessage(verdict)));
            return true;
        }
        capitalService.clearCapital(kingdomId);
        save();
        sender.sendMessage(success("The capital-fall region of your realm is released."));
        return true;
    }

    private static Optional<CapitalRegionBox> boxOf(String worldName, String regionId) {
        return WorldGuardBridge.regionBounds(worldName, regionId)
                .map(bounds -> new CapitalRegionBox(
                        bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()));
    }

    private Optional<PlayerMembership> requireCrown(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players may use this command."));
            return Optional.empty();
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            sender.sendMessage(error("You must join a kingdom first."));
            return Optional.empty();
        }
        if (!CapitalSitingPolicy.isCrown(membership.get().getRank())) {
            sender.sendMessage(error("Only the King or Queen may use this command."));
            return Optional.empty();
        }
        return membership;
    }

    private void save() {
        if (store != null) {
            store.saveFrom(kingdomService);
        }
    }

    private String capitalHelp() {
        return info("Capital commands:")
                + "\n" + c("&e/kingdom capital set") + c("&7 — King or Queen, inside your territory")
                + "\n" + c("&e/kingdom capital clear") + c("&7 — dissolve the city hall")
                + "\n" + c("&e/kingdom capital setregion <region>")
                + c("&7 — WorldGuard subregion for capital fall")
                + "\n" + c("&e/kingdom capital clearregion") + c("&7 — release the capital-fall region");
    }

    private String crierHelp() {
        return info("Town Crier commands:")
                + "\n" + c("&e/kingdom crier set") + c("&7 — stand the Crier where you are")
                + "\n" + c("&e/kingdom crier clear") + c("&7 — dismiss the Crier");
    }

    private String permitHelp() {
        return info("Build permit commands:")
                + "\n" + c("&e/kingdom permit grant <player>")
                + c("&7 — King, Queen, Duke or Count")
                + "\n" + c("&e/kingdom permit revoke <player>");
    }

    private String format(CityResult result) {
        return switch (result) {
            case CityResult.Success success -> success(success.message());
            case CityResult.Failure failure -> error(failure.message());
        };
    }

    private String success(String message) {
        return c("&a" + message);
    }

    private String error(String message) {
        return c("&c" + message);
    }

    private String info(String message) {
        return c("&b" + message);
    }
}
