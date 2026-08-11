package dev.mrlemoos.kingdom.model.parliament;

import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.parliament.HansardRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public final class ParliamentState {

    private Bill currentBill;
    private MintLocation preparedMint;
    private PreparedPublicWork preparedPublicWork;
    private final List<AssentedAct> assentedActs = new ArrayList<>();
    private final List<HansardRecord> hansard = new ArrayList<>();
    private boolean sessionOpen = true;
    private long stateOpeningPendingSinceMcDay = -1L;
    private long lastPremierQuestionsMcDay = -1L;
    private long confidenceCooldownUntilMcDay = -1L;
    private PendingMotionSecond pendingMotionSecond;
    private java.util.UUID speakerVillagerEntityId;

    /** The villager presiding over the Commons while no player holds the Speakership. */
    public Optional<java.util.UUID> speakerVillagerEntityId() {
        return Optional.ofNullable(speakerVillagerEntityId);
    }

    public void setSpeakerVillagerEntityId(java.util.UUID entityId) {
        this.speakerVillagerEntityId = entityId;
    }

    public void clearSpeakerVillager() {
        this.speakerVillagerEntityId = null;
    }

    public Optional<Bill> currentBill() {
        return Optional.ofNullable(currentBill);
    }

    public void setCurrentBill(Bill bill) {
        this.currentBill = bill;
    }

    public void clearCurrentBill() {
        this.currentBill = null;
    }

    public Optional<MintLocation> preparedMint() {
        return Optional.ofNullable(preparedMint);
    }

    public void setPreparedMint(MintLocation preparedMint) {
        this.preparedMint = preparedMint;
    }

    public void clearPreparedMint() {
        this.preparedMint = null;
    }

    public Optional<PreparedPublicWork> preparedPublicWork() {
        return Optional.ofNullable(preparedPublicWork);
    }

    public void setPreparedPublicWork(PreparedPublicWork preparedPublicWork) {
        this.preparedPublicWork = preparedPublicWork;
    }

    public void clearPreparedPublicWork() {
        this.preparedPublicWork = null;
    }

    public List<AssentedAct> assentedActsView() {
        return List.copyOf(assentedActs);
    }

    public void addAssentedAct(AssentedAct act) {
        assentedActs.add(act);
    }

    public void replaceAssentedActs(List<AssentedAct> loaded) {
        assentedActs.clear();
        if (loaded != null) {
            assentedActs.addAll(loaded);
        }
    }

    /** The bound record of this session so far, in the order the House decided its business. */
    public List<HansardRecord> hansardView() {
        return List.copyOf(hansard);
    }

    /** Enters a decided piece of business in Hansard. */
    public void addHansardRecord(HansardRecord record) {
        if (record != null) {
            hansard.add(record);
        }
    }

    public void replaceHansard(List<HansardRecord> loaded) {
        hansard.clear();
        if (loaded != null) {
            hansard.addAll(loaded);
        }
    }

    public boolean isSessionOpen() {
        return sessionOpen;
    }

    public void setSessionOpen(boolean sessionOpen) {
        this.sessionOpen = sessionOpen;
    }

    public OptionalLong stateOpeningPendingSinceMcDay() {
        return stateOpeningPendingSinceMcDay < 0 ? OptionalLong.empty() : OptionalLong.of(stateOpeningPendingSinceMcDay);
    }

    public void awaitStateOpening(long mcDay) {
        this.stateOpeningPendingSinceMcDay = Math.max(mcDay, 0L);
    }

    public void clearStateOpeningPending() {
        this.stateOpeningPendingSinceMcDay = -1L;
    }

    /** The in-game day the Speaker last called Questions to the Premier, if it ever has. */
    public OptionalLong lastPremierQuestionsMcDay() {
        return lastPremierQuestionsMcDay < 0 ? OptionalLong.empty() : OptionalLong.of(lastPremierQuestionsMcDay);
    }

    public void recordPremierQuestions(long mcDay) {
        this.lastPremierQuestionsMcDay = Math.max(mcDay, 0L);
    }

    public void clearPremierQuestions() {
        this.lastPremierQuestionsMcDay = -1L;
    }

    /** The motion of no confidence awaiting a seconder, if one is on the order paper. */
    public Optional<PendingMotionSecond> pendingMotionSecond() {
        return Optional.ofNullable(pendingMotionSecond);
    }

    public void setPendingMotionSecond(PendingMotionSecond pendingMotionSecond) {
        this.pendingMotionSecond = pendingMotionSecond;
    }

    public void clearPendingMotionSecond() {
        this.pendingMotionSecond = null;
    }

    /**
     * The in-game day the <b>confidence cooldown</b> runs to after a failed motion. It binds the
     * whole House, so it is kept against the kingdom rather than against any signatory.
     */
    public OptionalLong confidenceCooldownUntilMcDay() {
        return confidenceCooldownUntilMcDay < 0 ? OptionalLong.empty() : OptionalLong.of(confidenceCooldownUntilMcDay);
    }

    public void startConfidenceCooldown(long untilMcDay) {
        this.confidenceCooldownUntilMcDay = Math.max(untilMcDay, 0L);
    }

    public void clearConfidenceCooldown() {
        this.confidenceCooldownUntilMcDay = -1L;
    }

    /**
     * Ends the session: the bill before Parliament dies on the order paper, any prepared mint or
     * public work is discarded, and this Parliament's Hansard closes—bound and shelved by the caller
     * before the live record is cleared. The new Parliament must re-table its business after the
     * State Opening.
     */
    public void prorogue() {
        this.sessionOpen = false;
        this.stateOpeningPendingSinceMcDay = -1L;
        clearCurrentBill();
        clearPendingMotionSecond();
        clearPreparedMint();
        clearPreparedPublicWork();
        hansard.clear();
    }

    public void openSession() {
        this.sessionOpen = true;
        this.stateOpeningPendingSinceMcDay = -1L;
    }
}
