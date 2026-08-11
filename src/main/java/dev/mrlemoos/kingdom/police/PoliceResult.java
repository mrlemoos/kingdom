package dev.mrlemoos.kingdom.police;

public sealed interface PoliceResult permits PoliceResult.Success, PoliceResult.Failure {

    record Success(String message) implements PoliceResult {}

    record Failure(String message) implements PoliceResult {}

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
