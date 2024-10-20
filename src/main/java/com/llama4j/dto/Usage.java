package com.llama4j.dto;

public record Usage(
    int promptTokens,
    int completionTokens,
    int totalTokens,
    CompletionTokensDetails completionTokensDetails) {
}
