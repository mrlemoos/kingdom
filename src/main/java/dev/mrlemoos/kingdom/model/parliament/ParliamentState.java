package dev.mrlemoos.kingdom.model.parliament;

import dev.mrlemoos.kingdom.economy.model.MintLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public final class ParliamentState {

    private Bill currentBill;
    private MintLocation preparedMint;
    private final List<AssentedAct> assentedActs = new ArrayList<>();
    private boolean sessionOpen = true;
    private long stateOpeningPendingSinceMcDay = -1L;
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

    /**
     * Ends the session: the bill before Parliament dies on the order paper and any prepared mint is
     * discarded. The new Parliament must re-table its business after the State Opening.
     */
    public void prorogue() {
        this.sessionOpen = false;
        this.stateOpeningPendingSinceMcDay = -1L;
        clearCurrentBill();
        clearPreparedMint();
    }

    public void openSession() {
        this.sessionOpen = true;
        this.stateOpeningPendingSinceMcDay = -1L;
    }
}
