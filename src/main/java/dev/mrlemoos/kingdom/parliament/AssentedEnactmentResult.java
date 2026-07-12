package dev.mrlemoos.kingdom.parliament;

public sealed interface AssentedEnactmentResult
        permits AssentedEnactmentResult.Success, AssentedEnactmentResult.Failure {

    record Success(String message) implements AssentedEnactmentResult {}

    record Failure(String message) implements AssentedEnactmentResult {}

    static Success ok(String message) {
        return new Success(message);
    }

    static Failure fail(String message) {
        return new Failure(message);
    }
}
