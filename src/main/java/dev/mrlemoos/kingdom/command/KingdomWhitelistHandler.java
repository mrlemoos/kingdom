package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.whitelist.WhitelistResult;
import dev.mrlemoos.kingdom.whitelist.WhitelistService;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class KingdomWhitelistHandler {

    private final WhitelistService whitelistService;
    private final KingdomService kingdomService;

    public KingdomWhitelistHandler(WhitelistService whitelistService, KingdomService kingdomService) {
        this.whitelistService = whitelistService;
        this.kingdomService = kingdomService;
    }

    public boolean handleWhitelist(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(whitelistHelp());
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on" -> handleToggle(sender, true);
            case "off" -> handleToggle(sender, false);
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "status" -> handleStatus(sender);
            default -> {
                sender.sendMessage(whitelistHelp());
                yield true;
            }
        };
    }

    private boolean handleToggle(CommandSender sender, boolean enabled) {
        Optional<PlayerMembership> membership = requireAuthorisedMembership(sender);
        if (membership.isEmpty()) {
            return true;
        }

        WhitelistResult result = whitelistService.setEnabled(
                membership.get().getRank(),
                sender.isOp(),
                enabled);
        sender.sendMessage(formatWhitelist(result));
        return true;
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom whitelist add <player>"));
            return true;
        }
        Optional<PlayerMembership> membership = requireAuthorisedMembership(sender);
        if (membership.isEmpty()) {
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(error("Unknown player."));
            return true;
        }

        WhitelistResult result = whitelistService.allowPlayer(
                membership.get().getRank(),
                sender.isOp(),
                target.getUniqueId());
        sender.sendMessage(formatWhitelist(result));
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom whitelist remove <player>"));
            return true;
        }
        Optional<PlayerMembership> membership = requireAuthorisedMembership(sender);
        if (membership.isEmpty()) {
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        WhitelistResult result = whitelistService.disallowPlayer(
                membership.get().getRank(),
                sender.isOp(),
                target.getUniqueId());
        sender.sendMessage(formatWhitelist(result));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Optional<PlayerMembership> membership = requireAuthorisedMembership(sender);
        if (membership.isEmpty()) {
            return true;
        }

        sender.sendMessage(info("Server whitelist:"));
        sender.sendMessage(c("&7Status: ") + c("&f" + (whitelistService.isEnabled() ? "enabled" : "disabled")));
        Set<UUID> allowed = whitelistService.whitelistedPlayerIds();
        if (allowed.isEmpty()) {
            sender.sendMessage(c("&7No players listed."));
            return true;
        }
        for (UUID playerId : allowed) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(playerId);
            String name = member.getName() != null ? member.getName() : playerId.toString();
            sender.sendMessage(c("&7 - ") + c("&f" + name));
        }
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        Optional<PlayerMembership> membership = requireAuthorisedMembership(sender);
        if (membership.isEmpty()) {
            return true;
        }

        sender.sendMessage(info("Server whitelist: " + (whitelistService.isEnabled() ? "enabled" : "disabled")));
        sender.sendMessage(c("&7Listed players: ") + c("&f" + whitelistService.whitelistedPlayerIds().size()));
        return true;
    }

    private Optional<PlayerMembership> requireAuthorisedMembership(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players may use this command."));
            return Optional.empty();
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            player.sendMessage(error("You must join a kingdom first."));
            return Optional.empty();
        }
        return membership;
    }

    private String whitelistHelp() {
        return info("Whitelist commands:")
                + "\n" + c("&e/kingdom whitelist on") + c("&7 — King, Queen, or OP")
                + "\n" + c("&e/kingdom whitelist off")
                + "\n" + c("&e/kingdom whitelist add <player>")
                + "\n" + c("&e/kingdom whitelist remove <player>")
                + "\n" + c("&e/kingdom whitelist list")
                + "\n" + c("&e/kingdom whitelist status");
    }

    private String formatWhitelist(WhitelistResult result) {
        return switch (result) {
            case WhitelistResult.Success success -> success(success.message());
            case WhitelistResult.Failure failure -> error(failure.message());
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
