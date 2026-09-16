package dev.mrlemoos.kingdom.appeal;

public sealed interface AppealResult permits AppealResult.Success, AppealResult.Failure {
    record Success(String message) implements AppealResult {}
    record Failure(String message) implements AppealResult {}
}
