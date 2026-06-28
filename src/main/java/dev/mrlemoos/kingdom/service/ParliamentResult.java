package dev.mrlemoos.kingdom.service;

public sealed interface ParliamentResult permits ParliamentResult.Success, ParliamentResult.Failure {

    record Success(String message) implements ParliamentResult {}

    record Failure(String message) implements ParliamentResult {}

    static Success ok(String message) {
        return new Success(message);
    }

    static Failure fail(String message) {
        return new Failure(message);
    }
}
