package dev.mrlemoos.kingdom.treaty;

public sealed interface TreatyResult permits TreatyResult.Success, TreatyResult.Failure {

    record Success(String message) implements TreatyResult {}

    record Failure(String message) implements TreatyResult {}

    static Success ok(String message) {
        return new Success(message);
    }

    static Failure fail(String message) {
        return new Failure(message);
    }
}
