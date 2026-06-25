package dev.leo.kingdom.election;

public sealed interface ElectionResult {

    String message();

    record Success(String message) implements ElectionResult {}

    record Failure(String message) implements ElectionResult {}

    static ElectionResult ok(String message) {
        return new Success(message);
    }

    static ElectionResult fail(String message) {
        return new Failure(message);
    }
}
