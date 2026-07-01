package dev.mrlemoos.kingdom.whitelist;

public sealed interface WhitelistResult permits WhitelistResult.Success, WhitelistResult.Failure {

    record Success(String message) implements WhitelistResult {}

    record Failure(String message) implements WhitelistResult {}

    static Success ok(String message) {
        return new Success(message);
    }

    static Failure fail(String message) {
        return new Failure(message);
    }
}
