package dev.mrlemoos.kingdom.command;

import dev.mrlemoos.kingdom.locate.LocateCheckpointResolver;
import dev.mrlemoos.kingdom.locate.LocateKeyParser;
import dev.mrlemoos.kingdom.locate.LocateRegistries;
import dev.mrlemoos.kingdom.model.TeleportPlace;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.TeleportService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructureType;

public final class LocateCommandSuggestions {

    static final String PERM_CHECKPOINT = "kingdom.teleport.checkpoint";
    static final String PERM_LOCATE = "minecraft.command.locate";

    private LocateCommandSuggestions() {}

    public static List<String> suggest(
            CommandSender sender,
            String[] args,
            KingdomService kingdomService,
            TeleportService teleportService) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission(PERM_LOCATE)) {
                options.add("structure");
                options.add("biome");
            }
            if (sender.hasPermission(PERM_CHECKPOINT)) {
                kingdomService.getMembership(player.getUniqueId()).ifPresent(membership -> {
                    for (TeleportPlace place : teleportService.listPlaces(membership.getKingdomId())) {
                        String name = place.name();
                        if (!LocateCheckpointResolver.isReservedKeyword(name)) {
                            options.add(name);
                        }
                    }
                });
            }
            return filter(options, args[0]);
        }

        if (args.length == 2 && sender.hasPermission(PERM_LOCATE)) {
            if ("structure".equalsIgnoreCase(args[0])) {
                return filter(structureNames(), args[1]);
            }
            if ("biome".equalsIgnoreCase(args[0])) {
                return filter(biomeNames(), args[1]);
            }
        }

        return List.of();
    }

    public static String[] argsForSuggest(String remainingInput) {
        return TpCommandSuggestions.argsForSuggest(remainingInput);
    }

    private static List<String> structureNames() {
        List<String> names = new ArrayList<>();
        var structures = LocateRegistries.structures();
        for (Structure structure : structures) {
            names.add(structures.getKey(structure).getKey());
        }
        var structureTypes = LocateRegistries.structureTypes();
        for (StructureType structureType : structureTypes) {
            String key = structureType.getKey().getKey();
            if (names.stream().noneMatch(existing -> existing.equalsIgnoreCase(key))) {
                names.add(key);
            }
        }
        if (names.stream().noneMatch(name -> name.equalsIgnoreCase("village"))) {
            names.add("village");
        }
        return names.stream().sorted().toList();
    }

    private static List<String> biomeNames() {
        List<String> names = new ArrayList<>();
        for (Biome biome : LocateRegistries.biomes()) {
            names.add(biome.getKey().getKey());
        }
        return names.stream().sorted().toList();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toList());
    }
}
