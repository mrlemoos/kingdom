package dev.mrlemoos.kingdom.cloud;

import dev.mrlemoos.kingdom.command.LocateCommandSuggestions;
import dev.mrlemoos.kingdom.command.TpCommandSuggestions;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.TeleportService;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.suggestion.SuggestionProvider;

public final class CloudSuggestionProviders {

    private CloudSuggestionProviders() {
    }

    public static SuggestionProvider<CommandSender> kingdomIds(KingdomService service) {
        return SuggestionProvider.blockingStrings((context, input) -> {
            String prefix = input.lastRemainingToken().toLowerCase();
            List<String> ids = new ArrayList<>();
            for (Kingdom kingdom : service.listKingdoms()) {
                String id = kingdom.getId();
                if (id.toLowerCase().startsWith(prefix)) {
                    ids.add(id);
                }
            }
            ids.sort((left, right) -> left.compareTo(right));
            return ids;
        });
    }

    public static SuggestionProvider<CommandSender> onlinePlayerNames() {
        return SuggestionProvider.blockingStrings((context, input) -> {
            String prefix = input.lastRemainingToken().toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                String name = player.getName();
                if (name.toLowerCase().startsWith(prefix)) {
                    names.add(name);
                }
            }
            names.sort((left, right) -> left.compareTo(right));
            return names;
        });
    }

    public static SuggestionProvider<CommandSender> tpArgs(
            KingdomService kingdomService, TeleportService teleportService) {
        return SuggestionProvider.blockingStrings((context, input) -> TpCommandSuggestions.suggest(
                context.sender(),
                TpCommandSuggestions.argsForSuggest(input.remainingInput()),
                kingdomService,
                teleportService));
    }

    public static SuggestionProvider<CommandSender> locateArgs(
            KingdomService kingdomService, TeleportService teleportService) {
        return SuggestionProvider.blockingStrings((context, input) -> LocateCommandSuggestions.suggest(
                context.sender(),
                LocateCommandSuggestions.argsForSuggest(input.remainingInput()),
                kingdomService,
                teleportService));
    }
}
