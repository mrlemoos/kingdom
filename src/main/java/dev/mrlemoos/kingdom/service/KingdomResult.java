package dev.mrlemoos.kingdom.service;

public sealed interface KingdomResult permits KingdomResult.Success, KingdomResult.Failure {

    record Success(String message) implements KingdomResult {}

    record Failure(String message) implements KingdomResult {}

    static Success ok(String message) {
        return new Success(message);
    }

    static Failure fail(String message) {
        return new Failure(message);
    }
}
