package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.election.VillagerMpEntityService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.election.ResignationSubjectKind;
import dev.mrlemoos.kingdom.resignation.ResignationLetterDelivery;
import dev.mrlemoos.kingdom.resignation.ResignationResult;
import dev.mrlemoos.kingdom.resignation.ResignationService;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.Optional;
import java.util.OptionalInt;
import org.bukkit.Bukkit;
import dev.mrlemoos.kingdom.mint.TreasuryLordTargetScan;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ResignCommand {

    private final JavaPlugin plugin;
    private final KingdomService kingdomService;
    private final ResignationService resignationService;
    private final VillagerMpEntityService villagerMpEntityService;
    private final NoblePrefixDisplay nobleDisplay;
    private final YamlKingdomStore store;
    private final ResignationLetterDelivery letterDelivery;

    public ResignCommand(
            JavaPlugin plugin,
            KingdomService kingdomService,
            ResignationService resignationService,
            VillagerMpEntityService villagerMpEntityService,
            NoblePrefixDisplay nobleDisplay,
            YamlKingdomStore store,
            ResignationLetterDelivery letterDelivery) {
        this.plugin = plugin;
        this.kingdomService = kingdomService;
        this.resignationService = resignationService;
        this.villagerMpEntityService = villagerMpEntityService;
        this.nobleDisplay = nobleDisplay;
        this.store = store;
        this.letterDelivery = letterDelivery;
    }

    public boolean handle(Player player) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            player.sendMessage(error("You must join a kingdom first."));
            return true;
        }

        String kingdomId = membership.get().getKingdomId();
        if (isTargetingVillagerSpeaker(player)) {
            player.sendMessage(error("The Speaker of the House is not an elected office."));
            return true;
        }
        OptionalInt villagerSeat = resolveTargetedVillagerSeat(player, kingdomId);
        if (villagerSeat.isPresent() && !canOfferVillagerResignation(membership.get())) {
            player.sendMessage(error("Only a seated MP, the Premier, or the Speaker may offer a villager resignation."));
            return true;
        }

        ResignationResult result = resignationService.offerResignation(
                kingdomId, player.getUniqueId(), villagerSeat);
        if (result instanceof ResignationResult.Failure failure) {
            player.sendMessage(error(failure.message()));
            return true;
        }

        store.saveFrom(kingdomService);
        player.sendMessage(success(((ResignationResult.Success) result).message()));
        notifyCrown(kingdomId);
        return true;
    }

    public boolean accept(Player player, String kingdomId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !kingdomId.equals(membership.get().getKingdomId())) {
            return false;
        }

        ResignationSubjectKind subjectKind = resignationService
                .pendingResignation(kingdomId)
                .map(p -> p.subject().kind())
                .orElse(null);
        OptionalInt villagerSeat = resignationService
                .pendingResignation(kingdomId)
                .flatMap(p -> p.subject().seatIndex())
                .map(OptionalInt::of)
                .orElse(OptionalInt.empty());

        if (subjectKind == ResignationSubjectKind.VILLAGER_MP
                || subjectKind == ResignationSubjectKind.VILLAGER_PREMIER) {
            villagerSeat.ifPresent(index -> villagerMpEntityService.releaseSeat(kingdomId, index));
        }

        ResignationResult result = resignationService.acceptResignation(kingdomId, membership.get().getRank());
        if (result instanceof ResignationResult.Failure failure) {
            player.sendMessage(error(failure.message()));
            return true;
        }

        if (subjectKind == ResignationSubjectKind.VILLAGER_MP
                || subjectKind == ResignationSubjectKind.VILLAGER_PREMIER) {
            plugin.getServer().getScheduler().runTask(plugin, () -> villagerMpEntityService.syncKingdom(kingdomId));
        }
        store.saveFrom(kingdomService);
        refreshDisplays(kingdomId);
        letterDelivery.removeLetters(player, kingdomId);
        player.sendMessage(success(((ResignationResult.Success) result).message()));
        Bukkit.broadcastMessage(c("&6" + membership.get().getRank().displayTitle(
                        membership.get().getTitleStyle() != null
                                ? membership.get().getTitleStyle()
                                : dev.mrlemoos.kingdom.model.TitleStyle.MASCULINE)
                + " has accepted a resignation in "
                + kingdomService.getKingdom(kingdomId).orElseThrow().getDisplayName()
                + "."));
        return true;
    }

    public boolean reject(Player player, String kingdomId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !kingdomId.equals(membership.get().getKingdomId())) {
            return false;
        }

        ResignationResult result = resignationService.rejectResignation(kingdomId, membership.get().getRank());
        if (result instanceof ResignationResult.Failure failure) {
            player.sendMessage(error(failure.message()));
            return true;
        }

        store.saveFrom(kingdomService);
        letterDelivery.removeLetters(player, kingdomId);
        player.sendMessage(success(((ResignationResult.Success) result).message()));
        return true;
    }

    private void refreshDisplays(String kingdomId) {
        for (PlayerMembership membership : kingdomService.getMembershipsView().values()) {
            if (!kingdomId.equals(membership.getKingdomId())) {
                continue;
            }
            Player online = Bukkit.getPlayer(membership.getPlayerId());
            if (online != null) {
                nobleDisplay.refresh(online);
            }
        }
    }

    private void notifyCrown(String kingdomId) {
        letterDelivery.deliverPendingLetter(kingdomId);
    }

    private boolean isTargetingVillagerSpeaker(Player player) {
        Optional<org.bukkit.entity.Entity> target = TreasuryLordTargetScan.targetedEntity(player, 6.0);
        return target.isPresent()
                && target.get() instanceof Villager villager
                && villagerMpEntityService.isVillagerSpeaker(villager);
    }

    private OptionalInt resolveTargetedVillagerSeat(Player player, String kingdomId) {
        Optional<org.bukkit.entity.Entity> target = TreasuryLordTargetScan.targetedEntity(player, 6.0);
        if (target.isEmpty() || !(target.get() instanceof Villager villager)) {
            return OptionalInt.empty();
        }
        return kingdomService
                .getKingdom(kingdomId)
                .map(k -> k.getElectionState().seatIndexForVillagerEntity(villager.getUniqueId()))
                .orElse(OptionalInt.empty());
    }

    private static boolean canOfferVillagerResignation(PlayerMembership membership) {
        NobleRank rank = membership.getRank();
        return rank == NobleRank.MP || rank == NobleRank.PREMIER || rank == NobleRank.SPEAKER;
    }

    private static String success(String message) {
        return c("&a" + message);
    }

    private static String error(String message) {
        return c("&c" + message);
    }
}
