package dev.mrlemoos.kingdom.war;

public sealed interface WarResult permits WarResult.Success, WarResult.Failure {

    record Success(String message) implements WarResult {}

    record Failure(String message) implements WarResult {}

    static Success ok(String message) {
        return new Success(message);
    }

    static Failure fail(String message) {
        return new Failure(message);
    }
}
