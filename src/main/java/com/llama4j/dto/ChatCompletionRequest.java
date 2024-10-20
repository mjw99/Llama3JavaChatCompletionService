package com.llama4j.dto;

import java.util.List;

public record ChatCompletionRequest(
    List<Message> messages,
    float temperature,
    float top_p,
    int max_tokens
) {
}
