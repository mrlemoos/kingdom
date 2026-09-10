package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.granary.BukkitGranaryScan;
import dev.mrlemoos.kingdom.granary.GranaryBounds;
import dev.mrlemoos.kingdom.granary.GranarySiting;
import dev.mrlemoos.kingdom.granary.GranarySiting.Verdict;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /kingdom granary …}: the Crown links the region its realm keeps its grain in. */
public final class KingdomGranaryHandler {

    private final KingdomService kingdomService;
    private final YamlKingdomStore store;

    public KingdomGranaryHandler(KingdomService kingdomService, YamlKingdomStore store) {
        this.kingdomService = kingdomService;
        this.store = store;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(granaryHelp());
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "setregion" -> handleSetRegion(sender, args);
            case "clear" -> handleClear(sender);
            default -> {
                sender.sendMessage(granaryHelp());
                yield true;
            }
        };
    }

    private boolean handleSetRegion(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom granary setregion <region>"));
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(sender);
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
        Optional<GranaryBounds> granaryBounds = worldGuard
                ? BukkitGranaryScan.boundsOf(worldName, regionId)
                : Optional.empty();
        Optional<GranaryBounds> territoryBounds = worldGuard && granaryBounds.isPresent()
                ? kingdom.get().getWorldGuardRegions().stream()
                        .map(region -> BukkitGranaryScan.boundsOf(worldName, region))
                        .flatMap(Optional::stream)
                        .filter(bounds -> bounds.contains(granaryBounds.get()))
                        .findFirst()
                : Optional.empty();

        Verdict verdict = GranarySiting.evaluateLink(
                membership.get().getRank(),
                sender.isOp(),
                worldGuard,
                territoryRegion,
                territoryBounds,
                granaryBounds);
        if (verdict != Verdict.ALLOWED) {
            sender.sendMessage(error(GranarySiting.refusalMessage(verdict)));
            return true;
        }

        kingdom.get().setGranaryRegion(regionId);
        save();
        sender.sendMessage(success(
                "Linked granary region " + regionId + " to " + kingdom.get().getDisplayName() + "."));
        return true;
    }

    private boolean handleClear(CommandSender sender) {
        Optional<PlayerMembership> membership = requireMembership(sender);
        if (membership.isEmpty()) {
            return true;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(membership.get().getKingdomId());
        if (kingdom.isEmpty()) {
            sender.sendMessage(error("Unknown kingdom."));
            return true;
        }

        Verdict verdict = GranarySiting.evaluateClear(
                membership.get().getRank(), sender.isOp(), kingdom.get().getGranaryRegion());
        if (verdict != Verdict.ALLOWED) {
            sender.sendMessage(error(GranarySiting.refusalMessage(verdict)));
            return true;
        }

        kingdom.get().clearGranaryRegion();
        save();
        sender.sendMessage(success("The granary of " + kingdom.get().getDisplayName() + " is released."));
        return true;
    }

    private Optional<PlayerMembership> requireMembership(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players may site a granary."));
            return Optional.empty();
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            sender.sendMessage(error("You must join a kingdom first."));
            return Optional.empty();
        }
        return membership;
    }

    private void save() {
        if (store != null) {
            store.saveFrom(kingdomService);
        }
    }

    private String granaryHelp() {
        return info("Granary commands:")
                + "\n" + c("&e/kingdom granary setregion <region>")
                + c("&7 — King or Queen, inside your territory")
                + "\n" + c("&e/kingdom granary clear") + c("&7 — release the granary region");
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
