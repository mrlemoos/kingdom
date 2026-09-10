package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.muster.MusterService;
import dev.mrlemoos.kingdom.war.muster.gui.MusterGui;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class MusterGuiListener implements Listener {
    private final WarService wars;
    private final MusterService musters;
    private final YamlKingdomStore store;
    private final KingdomService kingdoms;

    public MusterGuiListener(WarService wars, MusterService musters, YamlKingdomStore store, KingdomService kingdoms) {
        this.wars = Objects.requireNonNull(wars); this.musters = Objects.requireNonNull(musters);
        this.store = Objects.requireNonNull(store); this.kingdoms = Objects.requireNonNull(kingdoms);
    }

    public void open(Player player) {
        kingdoms.getMembership(player.getUniqueId()).flatMap(member -> wars.activeWarFor(member.getKingdomId()))
                .filter(war -> musters.isEligible(war.id(), player.getUniqueId()))
                .ifPresentOrElse(war -> player.openInventory(MusterGui.create(war.id()).getInventory()),
                        () -> player.sendMessage(c("&cNo muster awaits your answer.")));
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MusterGui gui)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getView().getTopInventory()) return;
        WarResult result = switch (event.getSlot()) {
            case MusterGui.ANSWER_SLOT -> musters.answer(gui.warId(), player.getUniqueId());
            case MusterGui.REFUSE_SLOT -> musters.refuse(gui.warId(), player.getUniqueId());
            default -> null;
        };
        if (result == null) return;
        String message = result instanceof WarResult.Success success ? success.message() : ((WarResult.Failure) result).message();
        player.sendMessage(c((result instanceof WarResult.Success ? "&a" : "&c") + message));
        if (result instanceof WarResult.Success) store.saveFrom(kingdoms);
        player.closeInventory();
    }
}
