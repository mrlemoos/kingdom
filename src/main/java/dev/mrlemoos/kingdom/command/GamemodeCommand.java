package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Plugin {@code /gamemode}: names, short forms ({@code s/c/a/sp}) and numerals {@code 0–3}. */
public final class GamemodeCommand {

    private static final String PERM_GAMEMODE = "minecraft.command.gamemode";

    public static Optional<GameMode> parse(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "0", "s", "survival" -> Optional.of(GameMode.SURVIVAL);
            case "1", "c", "creative" -> Optional.of(GameMode.CREATIVE);
            case "2", "a", "adventure" -> Optional.of(GameMode.ADVENTURE);
            case "3", "sp", "spectator" -> Optional.of(GameMode.SPECTATOR);
            default -> Optional.empty();
        };
    }

    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_GAMEMODE)) {
            sender.sendMessage(c("&cYou do not have permission to change game mode."));
            return;
        }
        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(c("&cUsage: /gamemode <survival|creative|adventure|spectator|0-3> [player]"));
            return;
        }
        Optional<GameMode> mode = parse(args[0]);
        if (mode.isEmpty()) {
            sender.sendMessage(c("&cUnknown game mode: " + args[0]));
            return;
        }
        Player target;
        if (args.length == 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(c("&cPlayer not found: " + args[1]));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(c("&cConsole must name a player."));
            return;
        }
        String name = mode.get().name().charAt(0) + mode.get().name().substring(1).toLowerCase(Locale.ROOT);
        target.setGameMode(mode.get());
        target.sendMessage(c("&aGame mode set to " + name + "."));
        if (target != sender) {
            sender.sendMessage(c("&aSet " + target.getName() + "'s game mode to " + name + "."));
        }
    }
}
