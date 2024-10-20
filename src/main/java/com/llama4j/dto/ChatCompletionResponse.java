package com.llama4j.dto;

import java.util.List;

public record ChatCompletionResponse(
    String id,
    String object,
    long created,
    String model,
    String systemFingerprint,
    List<Choice> choices,
    Usage usage
) {}
