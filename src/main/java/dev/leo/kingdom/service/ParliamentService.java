package dev.leo.kingdom.service;

import dev.leo.kingdom.economy.model.FiscalRates;
import dev.leo.kingdom.economy.model.MintLocation;
import dev.leo.kingdom.election.ProfessionVoteBias;
import dev.leo.kingdom.election.StableSeatUuid;
import dev.leo.kingdom.model.election.MpSeatKind;
import dev.leo.kingdom.model.Kingdom;
import dev.leo.kingdom.model.NobleRank;
import dev.leo.kingdom.model.parliament.AssentedAct;
import dev.leo.kingdom.model.parliament.Bill;
import dev.leo.kingdom.model.parliament.BillPayload;
import dev.leo.kingdom.model.parliament.BillState;
import dev.leo.kingdom.model.parliament.BillType;
import dev.leo.kingdom.model.parliament.ChamberSite;
import dev.leo.kingdom.model.parliament.ParliamentState;
import dev.leo.kingdom.model.parliament.RegistrarSite;
import dev.leo.kingdom.model.parliament.VoteChoice;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class ParliamentService {

    private final KingdomService kingdomService;
    private final java.util.function.Supplier<Long> clockMs;
    private final AtomicLong billSequence = new AtomicLong(1);
    private ProfessionVoteBias professionVoteBias = ProfessionVoteBias.defaults();

    public ParliamentService(KingdomService kingdomService) {
        this(kingdomService, System::currentTimeMillis);
    }

    ParliamentService(KingdomService kingdomService, java.util.function.Supplier<Long> clockMs) {
        this.kingdomService = kingdomService;
        this.clockMs = clockMs;
    }

    public void setProfessionVoteBias(ProfessionVoteBias professionVoteBias) {
        this.professionVoteBias = professionVoteBias != null ? professionVoteBias : ProfessionVoteBias.defaults();
    }

    public ParliamentResult setCommons(String kingdomId, ChamberSite site) {
        return setChamber(kingdomId, site, ChamberTarget.COMMONS);
    }

    public ParliamentResult setLords(String kingdomId, ChamberSite site) {
        return setChamber(kingdomId, site, ChamberTarget.LORDS);
    }

    public ParliamentResult setRegistrar(String kingdomId, RegistrarSite site) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        if (site == null) {
            return ParliamentResult.fail("Registrar site is required.");
        }
        kingdom.get().getParliamentSites().setRegistrar(site);
        return ParliamentResult.ok("Registrar site set.");
    }

    public ParliamentResult setMpSeat(String kingdomId, int seatIndex, dev.leo.kingdom.model.election.MpSeatLocation location) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        if (seatIndex < 1 || seatIndex > 8) {
            return ParliamentResult.fail("MP seat index must be 1–8.");
        }
        if (location == null) {
            return ParliamentResult.fail("MP seat location is required.");
        }
        kingdom.get().getElectionState().setSeatLocation(seatIndex, location);
        return ParliamentResult.ok("MP seat " + seatIndex + " location set.");
    }

    public ParliamentResult prepareMint(String kingdomId, NobleRank rank, MintLocation location) {
        if (rank != NobleRank.PREMIER) {
            return ParliamentResult.fail("Only the Premier may prepare a mint location.");
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        if (location == null) {
            return ParliamentResult.fail("Mint location is required.");
        }
        kingdom.get().getParliamentState().setPreparedMint(location);
        return ParliamentResult.ok("Mint location prepared for a supply bill.");
    }

    public ParliamentResult tableFiscal(
            String kingdomId, NobleRank rank, UUID proposerId, FiscalRates rates, String optionalTitle) {
        if (rank != NobleRank.PREMIER) {
            return ParliamentResult.fail("Only the Premier may table a fiscal bill.");
        }
        if (rates == null) {
            return ParliamentResult.fail("Fiscal rates are required.");
        }
        return tableBill(
                kingdomId,
                proposerId,
                BillType.FISCAL,
                optionalTitle,
                new BillPayload.Fiscal(rates));
    }

    public ParliamentResult tableBudget(String kingdomId, NobleRank rank, UUID proposerId, double amount, String optionalTitle) {
        if (rank != NobleRank.PREMIER) {
            return ParliamentResult.fail("Only the Premier may table a budget bill.");
        }
        if (amount < 0) {
            return ParliamentResult.fail("Budget amount cannot be negative.");
        }
        return tableBill(
                kingdomId,
                proposerId,
                BillType.BUDGET,
                optionalTitle,
                new BillPayload.Budget(amount));
    }

    public ParliamentResult tableSpendMint(
            String kingdomId, NobleRank rank, UUID proposerId, double cost, String optionalTitle) {
        if (rank != NobleRank.PREMIER) {
            return ParliamentResult.fail("Only the Premier may table a mint supply bill.");
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        Optional<MintLocation> prepared = kingdom.get().getParliamentState().preparedMint();
        if (prepared.isEmpty()) {
            return ParliamentResult.fail("No mint location prepared. Use /kingdom parliament prepare mint at a lectern.");
        }
        if (cost < 0) {
            return ParliamentResult.fail("Mint cost cannot be negative.");
        }
        return tableBill(
                kingdomId,
                proposerId,
                BillType.SPEND_MINT,
                optionalTitle,
                new BillPayload.SpendMint(prepared.get(), cost));
    }

    public ParliamentResult tableSpendStipend(
            String kingdomId,
            NobleRank rank,
            UUID proposerId,
            UUID recipientId,
            double amount,
            String reason,
            String optionalTitle) {
        if (rank != NobleRank.PREMIER) {
            return ParliamentResult.fail("Only the Premier may table a supply bill.");
        }
        if (recipientId == null) {
            return ParliamentResult.fail("Recipient is required.");
        }
        if (amount <= 0) {
            return ParliamentResult.fail("Spend amount must be positive.");
        }
        String normalisedReason = reason != null && !reason.isBlank() ? reason.trim() : null;
        return tableBill(
                kingdomId,
                proposerId,
                BillType.SPEND_STIPEND,
                optionalTitle,
                new BillPayload.SpendStipend(recipientId, amount, normalisedReason));
    }

    public ParliamentResult openDivision(String kingdomId, NobleRank rank) {
        if (rank != NobleRank.SPEAKER) {
            return ParliamentResult.fail("Only the Speaker may open a division.");
        }
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty()) {
            return ParliamentResult.fail("No bill is before the House.");
        }
        if (bill.get().state() != BillState.TABLED) {
            return ParliamentResult.fail("A division is not ready to open.");
        }
        bill.get().setState(BillState.DIVISION_OPEN);
        return ParliamentResult.ok("Division open. MPs may vote.");
    }

    public ParliamentResult castVote(String kingdomId, NobleRank rank, UUID voterId, VoteChoice choice) {
        if (rank != NobleRank.MP) {
            return ParliamentResult.fail("Only Members of Parliament may vote in a division.");
        }
        if (choice == null) {
            return ParliamentResult.fail("Vote choice is required.");
        }
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty() || bill.get().state() != BillState.DIVISION_OPEN) {
            return ParliamentResult.fail("No division is open.");
        }
        bill.get().recordVote(voterId, choice);
        return ParliamentResult.ok("Vote recorded.");
    }

    public ParliamentResult castSpeakerVote(String kingdomId, NobleRank rank, VoteChoice choice) {
        if (rank != NobleRank.SPEAKER) {
            return ParliamentResult.fail("Only the Speaker may cast a casting vote.");
        }
        if (choice != VoteChoice.AYE && choice != VoteChoice.NAY) {
            return ParliamentResult.fail("Casting vote must be aye or nay.");
        }
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty() || bill.get().state() != BillState.DIVISION_OPEN) {
            return ParliamentResult.fail("No division is open.");
        }
        VoteTally tally = VoteTally.from(bill.get().votesView());
        if (tally.aye() != tally.nay()) {
            return ParliamentResult.fail("Casting vote is only required when aye and nay are tied.");
        }
        bill.get().setSpeakerCastingVote(choice);
        return ParliamentResult.ok("Casting vote recorded.");
    }

    public ParliamentResult closeDivision(String kingdomId, NobleRank rank) {
        if (rank != NobleRank.SPEAKER) {
            return ParliamentResult.fail("Only the Speaker may close a division.");
        }
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty() || bill.get().state() != BillState.DIVISION_OPEN) {
            return ParliamentResult.fail("No division is open.");
        }

        castVillagerMpVotes(kingdomId, bill.get());

        VoteTally tally = VoteTally.from(bill.get().votesView());
        int aye = tally.aye();
        int nay = tally.nay();

        if (aye == nay) {
            Optional<VoteChoice> casting = bill.get().speakerCastingVote();
            if (casting.isEmpty()) {
                return ParliamentResult.fail("Division is tied. The Speaker must cast a casting vote.");
            }
            if (casting.get() == VoteChoice.AYE) {
                aye++;
            } else {
                nay++;
            }
        }

        if (aye > nay) {
            bill.get().setState(BillState.AWAITING_ASSENT);
            return ParliamentResult.ok("Bill passed the Commons and awaits royal assent.");
        }

        bill.get().setState(BillState.FAILED);
        clearBill(kingdomId);
        return ParliamentResult.ok("Bill failed the division.");
    }

    public ParliamentResult assent(String kingdomId, NobleRank rank) {
        if (rank != NobleRank.KING && rank != NobleRank.QUEEN) {
            return ParliamentResult.fail("Only the King or Queen may grant royal assent.");
        }
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty() || bill.get().state() != BillState.AWAITING_ASSENT) {
            return ParliamentResult.fail("No bill awaits royal assent.");
        }
        bill.get().setState(BillState.ASSENTED);
        return ParliamentResult.ok("Royal assent granted.");
    }

    public ParliamentResult reject(String kingdomId, NobleRank rank) {
        if (rank != NobleRank.KING && rank != NobleRank.QUEEN) {
            return ParliamentResult.fail("Only the King or Queen may withhold assent.");
        }
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty() || bill.get().state() != BillState.AWAITING_ASSENT) {
            return ParliamentResult.fail("No bill awaits royal assent.");
        }
        bill.get().setState(BillState.REJECTED);
        clearBill(kingdomId);
        return ParliamentResult.ok("Royal assent withheld. Bill rejected.");
    }

    public Optional<Bill> currentBill(String kingdomId) {
        return kingdomService.getKingdom(kingdomId).map(k -> k.getParliamentState().currentBill()).orElse(Optional.empty());
    }

    public boolean isDivisionTied(String kingdomId) {
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty() || bill.get().state() != BillState.DIVISION_OPEN) {
            return false;
        }
        VoteTally tally = VoteTally.from(bill.get().votesView());
        return tally.aye() == tally.nay();
    }

    public boolean canCloseDivision(String kingdomId) {
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty() || bill.get().state() != BillState.DIVISION_OPEN) {
            return false;
        }
        if (isDivisionTied(kingdomId)) {
            return bill.get().speakerCastingVote().isPresent();
        }
        return true;
    }

    public Optional<AssentedActDraft> draftForAssentedBill(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return Optional.empty();
        }
        Optional<Bill> bill = kingdom.get().getParliamentState().currentBill();
        if (bill.isEmpty() || bill.get().state() != BillState.ASSENTED) {
            return Optional.empty();
        }

        Bill enacted = bill.get();
        return Optional.of(new AssentedActDraft(
                enacted.kingdomId(),
                enacted.id(),
                enacted.title(),
                enacted.type(),
                clockMs.get(),
                buildBookPages(enacted),
                enacted.votesView(),
                enacted.speakerCastingVote().orElse(null),
                enacted.payload()));
    }

    public void clearAssentedBill(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> {
            kingdom.getParliamentState().clearCurrentBill();
            kingdom.getParliamentState().clearPreparedMint();
        });
    }

    public Optional<AssentedActDraft> consumeAssentedBill(String kingdomId) {
        Optional<AssentedActDraft> draft = draftForAssentedBill(kingdomId);
        draft.ifPresent(ignored -> clearAssentedBill(kingdomId));
        return draft;
    }

    public void commitArchivedAct(String kingdomId, AssentedActDraft draft, RegistrarSite shelf, int slot) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> {
            AssentedAct act = new AssentedAct(
                    draft.billId(),
                    draft.title(),
                    draft.type(),
                    draft.assentedAtMs(),
                    draft.bookPages(),
                    draft.divisionVotes(),
                    draft.speakerCastingVote(),
                    shelf.worldName(),
                    shelf.blockX(),
                    shelf.blockY(),
                    shelf.blockZ(),
                    slot);
            kingdom.getParliamentState().addAssentedAct(act);
        });
    }

    public void clearPreparedMintAfterTable(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(k -> k.getParliamentState().clearPreparedMint());
    }

    private void castVillagerMpVotes(String kingdomId, Bill bill) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> {
            kingdom.getElectionState().seatsView().values().stream()
                    .filter(seat -> seat.kind() == MpSeatKind.VILLAGER)
                    .filter(seat -> seat.profession().isPresent())
                    .forEach(seat -> {
                        VoteChoice choice = professionVoteBias.resolve(
                                bill.type(), seat.profession().orElseThrow());
                        bill.recordVote(StableSeatUuid.forSeat(kingdomId, seat.index()), choice);
                    });
        });
    }

    private ParliamentResult tableBill(
            String kingdomId, UUID proposerId, BillType type, String optionalTitle, BillPayload payload) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        ParliamentState state = kingdom.get().getParliamentState();
        if (state.currentBill().isPresent()) {
            return ParliamentResult.fail("A bill is already before Parliament.");
        }

        long tabledAt = clockMs.get();
        String title = BillTitles.resolve(type, kingdomId, tabledAt, optionalTitle);
        Bill bill = new Bill(
                nextBillId(kingdomId),
                kingdomId,
                type,
                title,
                BillState.TABLED,
                proposerId,
                payload,
                tabledAt);
        state.setCurrentBill(bill);

        if (type == BillType.SPEND_MINT) {
            state.clearPreparedMint();
        }

        return ParliamentResult.ok("Bill tabled: " + title);
    }

    private ParliamentResult setChamber(String kingdomId, ChamberSite site, ChamberTarget target) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        if (site == null) {
            return ParliamentResult.fail("Chamber site is required.");
        }
        switch (target) {
            case COMMONS -> kingdom.get().getParliamentSites().setCommons(site);
            case LORDS -> kingdom.get().getParliamentSites().setLords(site);
        }
        return ParliamentResult.ok(target.label + " site set.");
    }

    private void clearBill(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(k -> k.getParliamentState().clearCurrentBill());
    }

    private String nextBillId(String kingdomId) {
        return kingdomId + "-" + billSequence.getAndIncrement();
    }

    private List<String> buildBookPages(Bill bill) {
        List<String> pages = new ArrayList<>();
        pages.add(bill.title());
        pages.add("Type: " + bill.type().name().toLowerCase(Locale.ROOT).replace('_', ' '));
        pages.add(describePayload(bill.payload()));
        pages.add("Division:");
        for (Map.Entry<UUID, VoteChoice> entry : bill.votesView().entrySet()) {
            pages.add(entry.getKey() + ": " + entry.getValue().name().toLowerCase(Locale.ROOT));
        }
        bill.speakerCastingVote()
                .ifPresent(choice -> pages.add("Speaker casting vote: " + choice.name().toLowerCase(Locale.ROOT)));
        return pages;
    }

    private static String describePayload(BillPayload payload) {
        return switch (payload) {
            case BillPayload.Fiscal fiscal -> String.format(
                    Locale.UK,
                    "Base tax %.1f%%, foreign %.1f%%, transfer %.1f%%, cross %.1f%%",
                    fiscal.rates().baseRate() * 100,
                    fiscal.rates().foreignSurcharge() * 100,
                    fiscal.rates().transferFee() * 100,
                    fiscal.rates().crossKingdomTransferFee() * 100);
            case BillPayload.Budget budget -> String.format(Locale.UK, "Budget cap %.2f Corona", budget.amount());
            case BillPayload.SpendMint mint -> String.format(
                    Locale.UK,
                    "Mint at %s %d %d %d for %.2f Corona",
                    mint.mintLocation().worldName(),
                    mint.mintLocation().x(),
                    mint.mintLocation().y(),
                    mint.mintLocation().z(),
                    mint.cost());
            case BillPayload.SpendStipend stipend -> {
                String reason = stipend.reason() != null ? " — " + stipend.reason() : "";
                yield String.format(
                        Locale.UK,
                        "Stipend %.2f Corona to %s%s",
                        stipend.amount(),
                        stipend.recipientId(),
                        reason);
            }
        };
    }

    private enum ChamberTarget {
        COMMONS("House of Commons"),
        LORDS("House of Lords");

        private final String label;

        ChamberTarget(String label) {
            this.label = label;
        }
    }

    public record AssentedActDraft(
            String kingdomId,
            String billId,
            String title,
            BillType type,
            long assentedAtMs,
            List<String> bookPages,
            Map<UUID, VoteChoice> divisionVotes,
            VoteChoice speakerCastingVote,
            BillPayload payload) {}
}
