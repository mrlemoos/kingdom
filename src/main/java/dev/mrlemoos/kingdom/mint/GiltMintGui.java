package dev.mrlemoos.kingdom.mint;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.economy.model.GiltIssue;
import dev.mrlemoos.kingdom.economy.model.KingdomEconomy;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Public mint ledger; purchases use the visitor's own wallet, including foreign visitors. */
public final class GiltMintGui implements InventoryHolder {
    public static final int SLOT_DEBT = 4;
    public static final int SLOT_BUY = 20;
    public static final int SLOT_OPEN = 24;
    public static final int SLOT_PREVIOUS = 36;
    public static final int SLOT_NEXT = 44;
    private final String kingdomId;
    private final int page;
    private Inventory inventory;

    private GiltMintGui(String kingdomId, int page) { this.kingdomId = kingdomId; this.page = page; }
    public String kingdomId() { return kingdomId; }
    public int page() { return page; }

    public static GiltMintGui create(String kingdomId, UUID visitor, EconomyService economy, boolean crown) {
        return create(kingdomId, visitor, economy, crown, 0, 0.0);
    }

    public static GiltMintGui create(String kingdomId, UUID visitor, EconomyService economy, boolean crown, int page) {
        return create(kingdomId, visitor, economy, crown, page, 0.0);
    }

    public static GiltMintGui create(String kingdomId, UUID visitor, EconomyService economy,
            boolean crown, int page, double warDebt) {
        GiltMintGui gui = new GiltMintGui(kingdomId, Math.max(0, page));
        gui.inventory = Bukkit.createInventory(gui, 45, component("&6Kingdom Mint: Gilts"));
        KingdomEconomy account = economy.kingdomEconomies().get(kingdomId);
        if (account == null) return gui;
        gui.inventory.setItem(SLOT_DEBT, new ItemBuilder(Material.BOOK)
                .displayAs(c("&6Public debt ledger"))
                .lore(c("&7Treasury: " + money(economy.getTreasuryBalance(kingdomId))))
                .lore(c("&7Overdraft: " + money(economy.getOverdraft(kingdomId))))
                .lore(c("&7Gilt principal: " + money(economy.getGiltPrincipal(kingdomId))))
                .lore(c("&7National debt: " + money(economy.getNationalDebt(kingdomId))))
                .lore(c("&7War debt (separate): " + money(warDebt)))
                .lore(c("&7Previous-day change: " + money(account.lastDebtChange())))
                .lore(c("&7Overdraft rate: " + account.activeRates().overdraftInterest()))
                .lore(c("&7Ground-rent rate: " + account.activeRates().groundRent()))
                .lore(c("&7Default: " + (account.giltDefault() ? "yes" : "no")))
                .build());
        GiltIssue open = account.giltIssues().stream().filter(GiltIssue::isOpen).findFirst().orElse(null);
        if (open != null) {
            gui.inventory.setItem(SLOT_BUY, new ItemBuilder(Material.EMERALD)
                    .displayAs(c("&aBuy gilts"))
                    .lore(c("&7Remaining face: " + money(open.remainingFace())))
                    .lore(c("&7Coupon: " + open.terms().couponRate() + " each realm day"))
                    .lore(c("&7Maturity: " + open.terms().maturityDays() + " days after close"))
                    .lore(c("&7Sale closes on day " + (open.openedDay() + 7)))
                    .lore(c("&7Your wallet: " + money(economy.getWalletBalance(visitor))))
                    .lore(c("&7Click, then type a whole Corona amount in chat"))
                    .build());
        } else if (crown && account.giltAuthority() != null) {
            gui.inventory.setItem(SLOT_OPEN, new ItemBuilder(Material.WRITTEN_BOOK)
                    .displayAs(c("&6Open authorised gilt sale"))
                    .lore(c("&7Face: " + account.giltAuthority().faceValue()))
                    .lore(c("&7Coupon: " + account.giltAuthority().couponRate()))
                    .lore(c("&7Maturity: " + account.giltAuthority().maturityDays() + " days"))
                    .lore(c("&7Authority lapses after day " + (account.giltAuthorisedDay() + 7)))
                    .lore(c("&cUnsold face becomes your personal gilt without payment."))
                    .lore(c("&7Click to review and confirm"))
                    .build());
        }
        List<GiltIssue> holdings = account.giltIssues().stream()
                .filter(issue -> issue.holding(visitor) > 0.0 || issue.paid(visitor) > 0.0)
                .toList();
        int offset = gui.page * 15;
        int slot = 28;
        for (GiltIssue issue : holdings.stream().skip(offset).limit(15).toList()) {
            if (slot == SLOT_PREVIOUS) slot++;
            gui.inventory.setItem(slot++, new ItemBuilder(Material.PAPER)
                    .displayAs(c("&eYour gilt holding"))
                    .lore(c("&7Principal: " + money(issue.holding(visitor))))
                    .lore(c("&7Overdue: " + money(issue.duePrincipal().getOrDefault(visitor, 0.0))))
                    .lore(c("&7Payments received: " + money(issue.paid(visitor))))
                    .lore(c("&7Next coupon: " + (issue.isOpen() ? "after sale closes" : "next realm day")))
                    .lore(c("&7Maturity day: " + (issue.isOpen() ? "after sale closes" : issue.maturityDay())))
                    .build());
        }
        if (gui.page > 0) gui.inventory.setItem(SLOT_PREVIOUS, new ItemBuilder(Material.ARROW)
                .displayAs(c("&ePrevious holdings")).build());
        if (offset + 15 < holdings.size()) gui.inventory.setItem(SLOT_NEXT, new ItemBuilder(Material.ARROW)
                .displayAs(c("&eNext holdings")).build());
        return gui;
    }

    private static String money(double amount) {
        return BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString() + " Corona";
    }
    @Override public Inventory getInventory() { return inventory; }
}
