package dev.mrlemoos.kingdom.model.war;

/**
 * Minimal military duty record for a mobilised standing roster member. Hardened service marks
 * permanent-core mobilisation as distinct from any future levy/conscription duty.
 */
public record OnDutyState(MoraleTier moraleTier, boolean hardenedService) {

    public OnDutyState {
        if (moraleTier == null) {
            throw new IllegalArgumentException("moraleTier must not be null");
        }
    }

    public static OnDutyState steadfastHardened() {
        return new OnDutyState(MoraleTier.STEADFAST, true);
    }
}
