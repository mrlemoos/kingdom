package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import dev.mrlemoos.kingdom.war.tribute.DebtPaymentResult;
import dev.mrlemoos.kingdom.war.tribute.TributeDesk;
import dev.mrlemoos.kingdom.war.tribute.WarDebt;
import dev.mrlemoos.kingdom.war.tribute.WarTributeService;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /kingdom tribute …}: Crown pays war debt from the treasury; operators may credit a debtor. */
public final class KingdomTributeHandler {

    private final KingdomService kingdoms;
    private final EconomyService economy;
    private final WarTributeService tribute;
    private final YamlEconomyStore economyStore;
    private final TributeDesk desk;

    public KingdomTributeHandler(
            KingdomService kingdoms,
            EconomyService economy,
            WarTributeService tribute,
            YamlEconomyStore economyStore) {
        this.kingdoms = kingdoms;
        this.economy = economy;
        this.tribute = tribute;
        this.economyStore = economyStore;
        this.desk = new TributeDesk(tribute);
    }

    public String infoLine(String kingdomId) {
        return KingdomInfoSummary.warDebtLine(tribute.totalDebtOwed(kingdomId), tribute.totalDebtOwedTo(kingdomId));
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (args.length == 0 || "status".equalsIgnoreCase(args[0])) {
            return handleStatus(sender);
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "pay" -> handlePay(sender, args);
            case "credit" -> handleCredit(sender, args);
            default -> {
                sender.sendMessage(help());
                yield true;
            }
        };
    }

    private boolean handleStatus(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players may use this command."));
            return true;
        }
        Optional<PlayerMembership> membership = kingdoms.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            sender.sendMessage(error("You must join a kingdom first."));
            return true;
        }
        String kingdomId = membership.get().getKingdomId();
        sender.sendMessage(info(KingdomInfoSummary.warDebtLine(
                tribute.totalDebtOwed(kingdomId), tribute.totalDebtOwedTo(kingdomId))));
        for (WarDebt debt : tribute.allDebts()) {
            if (debt.debtorKingdomId().equals(kingdomId)) {
                sender.sendMessage(c("&7  Owes " + formatCorona(debt.amount()) + " Corona to "
                        + display(debt.creditorKingdomId()) + "."));
            } else if (debt.creditorKingdomId().equals(kingdomId)) {
                sender.sendMessage(c("&7  Owed " + formatCorona(debt.amount()) + " Corona by "
                        + display(debt.debtorKingdomId()) + "."));
            }
        }
        return true;
    }

    private boolean handlePay(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(error(PAY_FROM_HUB));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom tribute pay <creditor> [amount]"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players may use this command."));
            return true;
        }
        Optional<PlayerMembership> membership = kingdoms.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            sender.sendMessage(error("You must join a kingdom first."));
            return true;
        }
        String creditorId = Kingdom.normaliseId(args[1]);
        if (kingdoms.getKingdom(creditorId).isEmpty()) {
            sender.sendMessage(error("Unknown kingdom."));
            return true;
        }
        OptionalDouble amount = OptionalDouble.empty();
        if (args.length > 2) {
            Double parsed = parseAmount(sender, args, 2, 0);
            if (parsed == null) {
                return true;
            }
            amount = OptionalDouble.of(parsed);
        }
        TributeDesk.Outcome outcome =
                desk.pay(membership.get().getKingdomId(), membership.get().getRank(), creditorId, amount);
        if (outcome.success()) {
            economyStore.saveFrom(economy);
        }
        sender.sendMessage(outcome.success() ? success(outcome.message()) : error(outcome.message()));
        return true;
    }

    /** Where the Crown pays war debt now; the command is the operators' escape hatch. */
    static final String PAY_FROM_HUB = "War debt is paid from the Realm Hub. "
            + "Type /kingdom, open The Treasury and click Pay War Debt.";

    private boolean handleCredit(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(error("Operators only."));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(error("Usage: /kingdom tribute credit <debtor> <creditor> [amount]"));
            return true;
        }
        String debtorId = Kingdom.normaliseId(args[1]);
        String creditorId = Kingdom.normaliseId(args[2]);
        if (kingdoms.getKingdom(debtorId).isEmpty() || kingdoms.getKingdom(creditorId).isEmpty()) {
            sender.sendMessage(error("Unknown kingdom."));
            return true;
        }
        double owed = tribute.debtOwed(debtorId, creditorId);
        if (owed <= 0) {
            sender.sendMessage(error("That realm owes that creditor no war debt."));
            return true;
        }
        Double amount = parseAmount(sender, args, 3, owed);
        if (amount == null) {
            return true;
        }
        economy.creditTreasury(debtorId, amount);
        DebtPaymentResult result = tribute.payDebt(debtorId, creditorId, amount);
        economyStore.saveFrom(economy);
        sender.sendMessage(success("Credited and paid " + formatCorona(result.paid())
                + " Corona of war debt. Remaining: " + formatCorona(result.remainingDebt()) + "."));
        return true;
    }

    private Double parseAmount(CommandSender sender, String[] args, int index, double fallback) {
        if (args.length <= index) {
            return fallback;
        }
        try {
            double amount = Double.parseDouble(args[index]);
            if (amount <= 0) {
                sender.sendMessage(error("Amount must be positive."));
                return null;
            }
            return amount;
        } catch (NumberFormatException ex) {
            sender.sendMessage(error("Amount must be a number."));
            return null;
        }
    }

    private String display(String kingdomId) {
        Optional<Kingdom> kingdom = kingdoms.getKingdom(kingdomId);
        return kingdom.isPresent() ? kingdom.get().getDisplayName() : kingdomId;
    }

    private String help() {
        return info("Tribute commands:")
                + "\n" + c("&e/kingdom tribute status") + c("&7 — war debt owed and owing")
                + "\n" + c("&e/kingdom tribute pay <creditor> [amount]")
                + c("&7 — operators; the Crown pays from The Treasury in /kingdom");
    }

    private static String formatCorona(double amount) {
        if (Math.rint(amount) == amount) {
            return String.format(Locale.UK, "%.0f", amount);
        }
        return String.format(Locale.UK, "%.2f", amount);
    }

    private static String success(String message) {
        return c("&a" + message);
    }

    private static String error(String message) {
        return c("&c" + message);
    }

    private static String info(String message) {
        return c("&b" + message);
    }
}
