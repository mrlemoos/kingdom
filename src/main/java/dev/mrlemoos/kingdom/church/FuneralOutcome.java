package dev.mrlemoos.kingdom.church;

/** What a member's funeral gave back: the ruling, and the experience returned to the deceased. */
public record FuneralOutcome(ChurchResult result, int experience) {

    public static FuneralOutcome refused(String message) {
        return new FuneralOutcome(ChurchResult.fail(message), 0);
    }
}
