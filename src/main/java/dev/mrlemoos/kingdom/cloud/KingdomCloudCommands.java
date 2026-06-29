package dev.mrlemoos.kingdom.cloud;

import dev.mrlemoos.kingdom.command.CommandArgTokenizer;
import dev.mrlemoos.kingdom.command.CoronaCommand;
import dev.mrlemoos.kingdom.command.KingdomCommand;
import dev.mrlemoos.kingdom.command.LocateCommand;
import dev.mrlemoos.kingdom.command.ResignCommand;
import dev.mrlemoos.kingdom.command.TpCommand;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.TeleportService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

public final class KingdomCloudCommands {

    private KingdomCloudCommands() {}

    public static void register(
            LegacyPaperCommandManager<CommandSender> manager,
            KingdomCommand kingdomCommand,
            CoronaCommand coronaCommand,
            TpCommand tpCommand,
            LocateCommand locateCommand,
            ResignCommand resignCommand,
            KingdomService kingdomService,
            TeleportService teleportService) {
        registerKingdomCommands(manager, kingdomCommand, kingdomService);
        registerTpCommands(manager, tpCommand, kingdomService, teleportService);
        registerLocateCommands(manager, locateCommand, kingdomService, teleportService);
        new AnnotationParser<CommandSender>(manager, CommandSender.class).parse(new CloudCoronaCommands(coronaCommand));
        new AnnotationParser<CommandSender>(manager, CommandSender.class).parse(new CloudResignCommands(resignCommand));
    }

    private static void registerKingdomCommands(
            LegacyPaperCommandManager<CommandSender> manager,
            KingdomCommand kingdomCommand,
            KingdomService kingdomService) {
        SuggestionProvider<CommandSender> kingdomIds = CloudSuggestionProviders.kingdomIds(kingdomService);
        SuggestionProvider<CommandSender> onlinePlayers = CloudSuggestionProviders.onlinePlayerNames();

        manager.command(manager.commandBuilder("kingdom", "kdm")
                .handler(ctx -> kingdomCommand.execute(ctx.sender(), new String[0])));

        manager.command(manager.commandBuilder("kingdom", "kdm")
                .literal("join")
                .required("kingdom", StringParser.stringParser(), kingdomIds)
                .handler(ctx -> kingdomCommand.execute(ctx.sender(), new String[] {
                    "join", ctx.get("kingdom")
                })));

        manager.command(manager.commandBuilder("kingdom", "kdm")
                .literal("list")
                .handler(ctx -> kingdomCommand.execute(ctx.sender(), new String[] {"list"})));

        manager.command(manager.commandBuilder("kingdom", "kdm")
                .literal("info")
                .handler(ctx -> kingdomCommand.execute(ctx.sender(), new String[] {"info"})));

        manager.command(manager.commandBuilder("kingdom", "kdm")
                .literal("info")
                .required("target", StringParser.stringParser())
                .handler(ctx -> kingdomCommand.execute(ctx.sender(), new String[] {
                    "info", ctx.get("target")
                })));

        manager.command(manager.commandBuilder("kingdom", "kdm")
                .literal("create")
                .required("id", StringParser.stringParser())
                .optional("display", StringParser.greedyStringParser())
                .handler(ctx -> kingdomCommand.execute(
                        ctx.sender(),
                        withOptionalGreedy(new String[] {"create", ctx.get("id")}, ctx, "display"))));

        manager.command(manager.commandBuilder("kingdom", "kdm")
                .literal("move")
                .required("player", StringParser.stringParser(), onlinePlayers)
                .required("kingdom", StringParser.stringParser(), kingdomIds)
                .handler(ctx -> kingdomCommand.execute(ctx.sender(), new String[] {
                    "move", ctx.get("player"), ctx.get("kingdom")
                })));

        manager.command(manager.commandBuilder("kingdom", "kdm")
                .literal("title")
                .required("player", StringParser.stringParser(), onlinePlayers)
                .required("rank", StringParser.stringParser())
                .optional("style", StringParser.stringParser())
                .handler(ctx -> kingdomCommand.execute(
                        ctx.sender(),
                        withOptional(
                                new String[] {"title", ctx.get("player"), ctx.get("rank")},
                                ctx,
                                "style"))));

        manager.command(manager.commandBuilder("kingdom", "kdm")
                .literal("setregion")
                .required("kingdom", StringParser.stringParser(), kingdomIds)
                .required("region", StringParser.stringParser())
                .handler(ctx -> kingdomCommand.execute(ctx.sender(), new String[] {
                    "setregion", ctx.get("kingdom"), ctx.get("region")
                })));

        manager.command(manager.commandBuilder("kingdom", "kdm")
                .literal("setworld")
                .required("kingdom", StringParser.stringParser(), kingdomIds)
                .required("world", StringParser.stringParser())
                .handler(ctx -> kingdomCommand.execute(ctx.sender(), new String[] {
                    "setworld", ctx.get("kingdom"), ctx.get("world")
                })));

        registerGreedySubcommand(manager, kingdomCommand, "fiscal");
        registerGreedySubcommand(manager, kingdomCommand, "budget");
        registerGreedySubcommand(manager, kingdomCommand, "mint");
        registerGreedySubcommand(manager, kingdomCommand, "treasury");
        registerGreedySubcommand(manager, kingdomCommand, "parliament");
        registerGreedySubcommand(manager, kingdomCommand, "election");
    }

    private static void registerGreedySubcommand(
            LegacyPaperCommandManager<CommandSender> manager,
            KingdomCommand kingdomCommand,
            String subcommand) {
        manager.command(manager.commandBuilder("kingdom", "kdm")
                .literal(subcommand)
                .handler(ctx -> kingdomCommand.execute(ctx.sender(), new String[] {subcommand})));

        manager.command(manager.commandBuilder("kingdom", "kdm")
                .literal(subcommand)
                .required("args", StringParser.greedyStringParser())
                .handler(ctx -> kingdomCommand.execute(
                        ctx.sender(),
                        prepend(subcommand, CommandArgTokenizer.tokenize(ctx.get("args"))))));
    }

    private static void registerLocateCommands(
            LegacyPaperCommandManager<CommandSender> manager,
            LocateCommand locateCommand,
            KingdomService kingdomService,
            TeleportService teleportService) {
        SuggestionProvider<CommandSender> locateArgs =
                CloudSuggestionProviders.locateArgs(kingdomService, teleportService);

        manager.command(manager.commandBuilder("locate")
                .handler(ctx -> locateCommand.execute(ctx.sender(), new String[0])));

        manager.command(manager.commandBuilder("locate")
                .required("args", StringParser.greedyStringParser(), locateArgs)
                .handler(ctx -> locateCommand.execute(
                        ctx.sender(), CommandArgTokenizer.tokenize(ctx.get("args")))));
    }

    private static void registerTpCommands(
            LegacyPaperCommandManager<CommandSender> manager,
            TpCommand tpCommand,
            KingdomService kingdomService,
            TeleportService teleportService) {
        SuggestionProvider<CommandSender> tpArgs =
                CloudSuggestionProviders.tpArgs(kingdomService, teleportService);

        manager.command(manager.commandBuilder("tp", "teleport")
                .handler(ctx -> tpCommand.execute(ctx.sender(), new String[0])));

        manager.command(manager.commandBuilder("tp", "teleport")
                .required("args", StringParser.greedyStringParser(), tpArgs)
                .handler(ctx -> tpCommand.execute(
                        ctx.sender(), CommandArgTokenizer.tokenize(ctx.get("args")))));
    }

    private static String[] prepend(String head, String[] tail) {
        String[] args = new String[tail.length + 1];
        args[0] = head;
        System.arraycopy(tail, 0, args, 1, tail.length);
        return args;
    }

    private static String[] withOptional(String[] base, CommandContext<CommandSender> ctx, String key) {
        return ctx.<String>optional(key)
                .map(value -> {
                    String[] args = new String[base.length + 1];
                    System.arraycopy(base, 0, args, 0, base.length);
                    args[base.length] = value;
                    return args;
                })
                .orElse(base);
    }

    private static String[] withOptionalGreedy(String[] base, CommandContext<CommandSender> ctx, String key) {
        return ctx.<String>optional(key)
                .map(greedy -> {
                    String[] tail = CommandArgTokenizer.tokenize(greedy);
                    String[] args = new String[base.length + tail.length];
                    System.arraycopy(base, 0, args, 0, base.length);
                    System.arraycopy(tail, 0, args, base.length, tail.length);
                    return args;
                })
                .orElse(base);
    }

    public static final class CloudCoronaCommands {

        private final CoronaCommand coronaCommand;

        CloudCoronaCommands(CoronaCommand coronaCommand) {
            this.coronaCommand = coronaCommand;
        }

        @Command("corona|cr")
        @CommandDescription("Show your Corona wallet balance")
        public void balance(Player player) {
            coronaCommand.execute(player, new String[] {"balance"});
        }

        @Command("corona|cr pay <target> <amount>")
        @CommandDescription("Pay another player Corona")
        public void pay(Player player, @Argument("target") String target, @Argument("amount") double amount) {
            coronaCommand.execute(player, new String[] {
                "pay", target, Double.toString(amount)
            });
        }

        @Command("corona|cr deposit")
        @CommandDescription("Deposit Corona nuggets at a kingdom mint")
        public void deposit(Player player) {
            coronaCommand.execute(player, new String[] {"deposit"});
        }
    }

    public static final class CloudResignCommands {

        private final ResignCommand resignCommand;

        CloudResignCommands(ResignCommand resignCommand) {
            this.resignCommand = resignCommand;
        }

        @Command("resign")
        @CommandDescription("Offer your MP or Premier seat for royal approval")
        public void resign(Player player) {
            resignCommand.handle(player);
        }
    }
}
