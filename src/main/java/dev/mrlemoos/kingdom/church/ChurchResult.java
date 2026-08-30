package dev.mrlemoos.kingdom.church;

public sealed interface ChurchResult permits ChurchResult.Success, ChurchResult.Failure {

    record Success(String message) implements ChurchResult {}

    record Failure(String message) implements ChurchResult {}

    static Success ok(String message) {
        return new Success(message);
    }

    static Failure fail(String message) {
        return new Failure(message);
    }

    default String message() {
        return switch (this) {
            case Success success -> success.message();
            case Failure failure -> failure.message();
        };
    }
}
