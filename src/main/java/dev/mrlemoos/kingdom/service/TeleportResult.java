package dev.mrlemoos.kingdom.service;

public sealed interface TeleportResult permits TeleportResult.Success, TeleportResult.Failure {

    record Success(String message) implements TeleportResult {}

    record Failure(String message) implements TeleportResult {}

    static Success ok(String message) {
        return new Success(message);
    }

    static Failure fail(String message) {
        return new Failure(message);
    }
}
