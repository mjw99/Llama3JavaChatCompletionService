package com.llama4j.dto;

public record Choice(
    int index,
    Message message,
    Object logprobs,
    String finishReason
) {}
