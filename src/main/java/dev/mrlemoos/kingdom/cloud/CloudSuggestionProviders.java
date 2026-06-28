package dev.mrlemoos.kingdom.cloud;

import dev.mrlemoos.kingdom.command.TpCommandSuggestions;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.TeleportService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.suggestion.SuggestionProvider;

public final class CloudSuggestionProviders {

    private CloudSuggestionProviders() {}

    public static SuggestionProvider<CommandSender> kingdomIds(KingdomService service) {
        return SuggestionProvider.blockingStrings((context, input) -> {
            String prefix = input.lastRemainingToken().toLowerCase();
            return service.listKingdoms().stream()
                    .map(Kingdom::getId)
                    .sorted()
                    .filter(id -> id.toLowerCase().startsWith(prefix))
                    .toList();
        });
    }

    public static SuggestionProvider<CommandSender> onlinePlayerNames() {
        return SuggestionProvider.blockingStrings((context, input) -> {
            String prefix = input.lastRemainingToken().toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .sorted()
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .toList();
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
}
