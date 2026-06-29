package dev.mrlemoos.kingdom.command;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.TeleportPlace;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.TeleportService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TpCommandSuggestions {

    static final String PERM_TELEPORT = "minecraft.command.teleport";
    static final String PERM_CHECKPOINT = "kingdom.teleport.checkpoint";

    private TpCommandSuggestions() {}

    public static List<String> suggest(
            CommandSender sender,
            String[] args,
            KingdomService kingdomService,
            TeleportService teleportService) {
        if (args.length == 0) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.isOp()) {
                options.add("checkpoint");
            }
            if (sender.hasPermission(PERM_TELEPORT)) {
                options.add("here");
                options.add("@s");
                options.addAll(onlineNames());
            }
            if (sender instanceof Player player && sender.hasPermission(PERM_CHECKPOINT)) {
                kingdomService.getMembership(player.getUniqueId()).ifPresent(membership -> options.addAll(
                        teleportService.listPlaces(membership.getKingdomId()).stream()
                                .map(TeleportPlace::name)
                                .toList()));
            }
            return filter(options, args[0]);
        }

        if ("checkpoint".equalsIgnoreCase(args[0])) {
            if (!sender.isOp()) {
                return List.of();
            }
            if (args.length == 2) {
                return filter(List.of("create", "delete", "list"), args[1]);
            }
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (args.length == 3) {
                return switch (sub) {
                    case "create", "delete", "list" -> filter(kingdomIds(kingdomService), args[2]);
                    default -> List.of();
                };
            }
            if (args.length == 4) {
                String kingdomId = kingdomService.getKingdom(args[2]).map(Kingdom::getId).orElse(null);
                if (kingdomId == null) {
                    return List.of();
                }
                return filter(
                        teleportService.listPlaces(kingdomId).stream()
                                .map(TeleportPlace::name)
                                .toList(),
                        args[3]);
            }
        }

        if (sender.hasPermission(PERM_TELEPORT)) {
            if (args.length == 2 && isBringHereToken(args[0])) {
                return filter(onlineNames(), args[1]);
            }
            if (args.length == 2 && !TeleportCoordinateParser.isCoordinateToken(args[0])) {
                List<String> destinations = new ArrayList<>(onlineNames());
                if (sender instanceof Player player) {
                    kingdomService.getMembership(player.getUniqueId()).ifPresent(membership -> destinations.addAll(
                            teleportService.listPlaces(membership.getKingdomId()).stream()
                                    .map(TeleportPlace::name)
                                    .toList()));
                }
                return filter(destinations, args[1]);
            }
        }

        return List.of();
    }

    public static String[] argsForSuggest(String remainingInput) {
        String[] tokens = CommandArgTokenizer.tokenize(remainingInput);
        if (remainingInput.endsWith(" ") && !remainingInput.isBlank()) {
            String[] withNext = new String[tokens.length + 1];
            System.arraycopy(tokens, 0, withNext, 0, tokens.length);
            withNext[tokens.length] = "";
            return withNext;
        }
        if (tokens.length == 0 && !remainingInput.isBlank()) {
            return new String[] {remainingInput};
        }
        return tokens;
    }

    private static List<String> kingdomIds(KingdomService kingdomService) {
        return kingdomService.listKingdoms().stream().map(Kingdom::getId).sorted().toList();
    }

    private static List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toList());
    }

    private static boolean isBringHereToken(String token) {
        return "here".equalsIgnoreCase(token) || "@s".equalsIgnoreCase(token);
    }
}
