package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.police.TrialJuryRuntime;
import dev.mrlemoos.kingdom.police.gui.TrialJuryBallotAction;
import dev.mrlemoos.kingdom.police.gui.TrialJuryBallotGui;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Handles secret trial-jury ballot clicks. */
public final class TrialJuryGuiListener implements Listener {

    private final TrialJuryRuntime runtime;

    public TrialJuryGuiListener(TrialJuryRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TrialJuryBallotGui ballot)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        TrialJuryBallotAction action = ballot.actionForSlot(event.getSlot());
        if (action == null) {
            return;
        }
        boolean guilty = action == TrialJuryBallotAction.GUILTY;
        runtime.castFromGui(player, ballot.kingdomId(), ballot.accusedId(), guilty);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TrialJuryBallotGui) {
            event.setCancelled(true);
        }
    }
}
