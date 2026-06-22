package dev.leo.kingdom.economy.service;

public sealed interface EconomyResult permits EconomyResult.Success, EconomyResult.Failure {

    record Success(String message) implements EconomyResult {}

    record Failure(String message) implements EconomyResult {}

    static Success ok(String message) {
        return new Success(message);
    }

    static Failure fail(String message) {
        return new Failure(message);
    }
}
