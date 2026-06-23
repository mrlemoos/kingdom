package dev.leo.kingdom.model.parliament;

import dev.leo.kingdom.economy.model.MintLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ParliamentState {

    private Bill currentBill;
    private MintLocation preparedMint;
    private final List<AssentedAct> assentedActs = new ArrayList<>();

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
}
