package com.llama4j.web.rest;

import com.llama4j.core.ChatFormat;
import com.llama4j.core.Llama;
import com.llama4j.core.Llama3;
import com.llama4j.core.Sampler;
import com.llama4j.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.llama4j.core.Llama3.*;

@RestController
public class LlamaWrapperApplication {

    private static final Logger LOG = LoggerFactory.getLogger(LlamaWrapperApplication.class);

    private final AtomicLong requestCounter = new AtomicLong(0);

    private final Llama model;

    @Value("${llama.model.path}")
    private String modelPath;

    @Value("${llama.model.name}")
    private String modelName;

    public LlamaWrapperApplication(Llama model) {
        this.model = model;
    }

    @PostMapping("/chat/completions")
    public ResponseEntity<Object> chatCompletions(@RequestBody ChatCompletionRequest request) {
        LOG.info("Received request: {}", request);

        List<Message> messages = request.messages();
        if (messages == null || messages.isEmpty()) {
            LOG.error("Messages cannot be empty");
            return ResponseEntity.badRequest().body("Messages cannot be empty");
        }

        // Convert messages to Llama format
        List<ChatFormat.Message> llamaMessages = new ArrayList<>();
        for (Message message : messages) {
            llamaMessages.add(new ChatFormat.Message(new ChatFormat.Role(message.role()), message.content()));
            LOG.debug("Prompt {} : {}", message.role(), message.content());
        }

        Llama3.Options options =
            new Llama3.Options(
                Path.of(modelPath + File.pathSeparator +modelName),
                request.messages().size() > 1 ? request.messages().get(1).content() : null, // User Prompt
                request.messages().getFirst().content(), // System Prompt
                false, // TODO interactive should be a request parameter
                request.temperature() == 0.0f ? 0.7f : request.temperature(),
                request.top_p() == 0.0f ? 0.95f : request.top_p(),
                123, // seed,
                request.max_tokens() == 0 ? 4_000 : request.max_tokens(),
                true, // stream
                false // echo
            );

        Sampler sampler = selectSampler(model.configuration().vocabularySize, options.temperature(), options.topp(), options.seed());

        RequestResponse requestResponse = null;
        if (options.interactive()) {
            runInteractive(model, sampler, options);
        } else {
            requestResponse = runInstructOnce(model, sampler, options);
        }

        ChatFormat chatFormat = new ChatFormat(model.tokenizer());

        // Encode the conversation
        List<Integer> conversationTokens = chatFormat.encodeDialogPrompt(true, llamaMessages);

        String cleanedResponse = cleanResponse(requestResponse != null ? requestResponse.completion() : "");

        // Create the response object
        ChatCompletionResponse response = new ChatCompletionResponse(
            "chatcmpl-" + requestCounter.incrementAndGet(),
            "chat.completion",
            System.currentTimeMillis() / 1000,
            modelName,
            "fp_" + Long.toHexString(System.nanoTime()), // Generate a mock system fingerprint
            Collections.singletonList(new Choice(
                0,
                new Message("assistant", cleanedResponse),
                null, // logprobs
                "stop"
            )),
            new Usage(
                conversationTokens.size(),
                requestResponse != null ? requestResponse.totalTokens() : 0,
                conversationTokens.size() + (requestResponse != null ? requestResponse.totalTokens() : 0),
                new CompletionTokensDetails(0) // Assuming no specific reasoning tokens
            )
        );

        return ResponseEntity.ok(response);
    }

    private String cleanResponse(String response) {
        // Remove <|eot_id|> token
        String cleaned = response.replaceAll("<\\|eot_id\\|>", "").trim();

        // You might want to add more cleaning steps here if needed
        // For example, removing any other special tokens or unwanted characters

        return cleaned;
    }
}
