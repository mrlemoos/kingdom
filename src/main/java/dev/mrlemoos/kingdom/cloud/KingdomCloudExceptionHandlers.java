package dev.mrlemoos.kingdom.cloud;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.command.UnknownOrderRefusal;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.exception.NoSuchCommandException;
import org.incendo.cloud.exception.handling.ExceptionContext;

/**
 * The second door onto the realm's "no such order" refusal.
 *
 * <p>{@code KingdomCommand.execute} refuses a subcommand it has no case for, but a player at the
 * keyboard never reaches it: Cloud parses {@code /kingdom <sub>} as literals, so an unparseable
 * literal is thrown out by Cloud's own syntax handler long before {@code execute} runs. Without
 * these handlers a mistyped order gets Cloud's raw "Invalid command syntax" instead of the realm's
 * voice.
 *
 * <p>{@code InvalidCommandSenderException} is deliberately not handled: no command in this plugin is
 * registered against a narrowed sender type — every one takes {@code CommandSender} and settles the
 * console-versus-player question itself — so a handler for it could never fire.
 */
public final class KingdomCloudExceptionHandlers {

    private KingdomCloudExceptionHandlers() {
    }

    /** Registers the realm's wording over Cloud's defaults for a mistyped or unknown order. */
    public static void register(CommandManager<CommandSender> manager) {
        manager.exceptionController()
                .registerHandler(
                        InvalidSyntaxException.class,
                        ctx -> refuse(ctx, rawInputOf(ctx), ctx.exception().correctSyntax()))
                .registerHandler(
                        NoSuchCommandException.class,
                        ctx -> refuse(ctx, ctx.exception().suppliedCommand(), ""));
    }

    private static void refuse(
            ExceptionContext<CommandSender, ? extends Throwable> ctx, String rawInput, String correctSyntax) {
        CommandSender sender = ctx.context().sender();
        boolean first = true;
        for (String line : UnknownOrderRefusal.lines(rawInput, correctSyntax)) {
            sender.sendMessage(c((first ? "&c" : "&7") + line));
            first = false;
        }
    }

    private static String rawInputOf(ExceptionContext<CommandSender, ? extends Throwable> ctx) {
        try {
            return ctx.context().rawInput().input();
        } catch (RuntimeException ignored) {
            // The raw input is bookkeeping Cloud may not have laid down yet; the refusal reads fine
            // without naming the order.
            return "";
        }
    }
}
