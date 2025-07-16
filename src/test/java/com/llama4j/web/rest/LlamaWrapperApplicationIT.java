package com.llama4j.web.rest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.llama4j.config.ModelConfigTest;
import com.llama4j.dto.ChatCompletionRequest;
import com.llama4j.dto.ChatCompletionResponse;
import com.llama4j.dto.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
    ModelConfigTest.class,
})
@ActiveProfiles("test")
public class LlamaWrapperApplicationIT {

    protected MockMvc mvc;

    @Autowired
    WebApplicationContext context;

    protected ObjectMapper objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    public void tellJokeChatCompletions() throws Exception {
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(new Message("user", "Tell me a joke"));
        messages.add(new Message("system", "You are a comedian"));

        ChatCompletionRequest chatCompletionRequest =
            new ChatCompletionRequest(messages, 0.9f, 1.0f, 4000);

        MvcResult result = mvc.perform(
                post("http://localhost:8080/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(chatCompletionRequest)))
            .andExpect(status().isOk())
            .andDo(print())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        ChatCompletionResponse chatCompletionResponse = objectMapper.readValue(responseContent, ChatCompletionResponse.class);

        System.out.println("------------------------------------");
        System.out.println("Response: " + chatCompletionResponse.choices().getFirst().message().content());
        System.out.println("------------------------------------");

        assertThat(chatCompletionResponse.id()).isNotNull();

        assertThat(chatCompletionResponse.choices().getFirst().message().role()).isEqualTo("assistant");

        assertThat(chatCompletionResponse.model()).isEqualTo("Meta-Llama-3.2-1b-instruct-Q8_0.gguf");
        assertThat(chatCompletionResponse.usage()).isNotNull();
        assertThat(chatCompletionResponse.created()).isBetween(0L, System.currentTimeMillis());
        assertThat(chatCompletionResponse.object()).isEqualTo("chat.completion");
    }

    @Test
    public void reviewCodeChatCompletion() throws Exception {
        ArrayList<Message> messages = new ArrayList<>();

        messages.add(new Message("system", "You are a Java expert that knows everything about Open LLM interfaces"));
        messages.add(new Message("user", """
            <UserPrompt>How can this code be improved?</UserPrompt>
            <context>
            Context:\s

            <FileContents>
            File: LlamaWrapperApplication.java
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

            @SpringBootApplication
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
                    String cleaned = response.replaceAll("<\\\\|eot_id\\\\|>", "").trim();

                    // You might want to add more cleaning steps here if needed
                    // For example, removing any other special tokens or unwanted characters

                    return cleaned;
                }
            }
            </FileContents>
            </context>
            """));

        ChatCompletionRequest chatCompletionRequest =
            new ChatCompletionRequest(messages, 0.9f, 1.0f, 4000);

        MvcResult result = mvc.perform(
                post("http://localhost:8080/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(chatCompletionRequest)))
            .andExpect(status().isOk())
            .andDo(print())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        ChatCompletionResponse chatCompletionResponse = objectMapper.readValue(responseContent, ChatCompletionResponse.class);

        System.out.println("------------------------------------");
        System.out.println("Response: " + chatCompletionResponse.choices().getFirst().message().content());
        System.out.println("------------------------------------");

        assertThat(chatCompletionResponse.id()).isNotNull();

        assertThat(chatCompletionResponse.choices().getFirst().message().role()).isEqualTo("assistant");

        assertThat(chatCompletionResponse.model()).isEqualTo("Meta-Llama-3.2-1b-instruct-Q8_0.gguf");
        assertThat(chatCompletionResponse.usage()).isNotNull();
        assertThat(chatCompletionResponse.created()).isBetween(0L, System.currentTimeMillis());
        assertThat(chatCompletionResponse.object()).isEqualTo("chat.completion");
    }

    public String asJsonString(final Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
