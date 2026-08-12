package dev.mrlemoos.kingdom.city;

public sealed interface CityResult permits CityResult.Success, CityResult.Failure {

    record Success(String message) implements CityResult {}

    record Failure(String message) implements CityResult {}

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
