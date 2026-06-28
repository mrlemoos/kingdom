package dev.mrlemoos.kingdom.resignation;

public sealed interface ResignationResult {

    record Success(String message) implements ResignationResult {}

    record Failure(String message) implements ResignationResult {}
}
