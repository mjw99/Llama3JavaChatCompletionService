package com.llama4j.config;

import com.llama4j.core.Llama;
import com.llama4j.core.ModelLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.nio.file.Path;

@TestConfiguration
public class ModelConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelConfigTest.class);

    @Value("${llama.model.path}")
    private String modelPath;

    @Value("${llama.model.name}")
    private String modelName;

    @Bean
    Llama model() {
        Llama model;
        try {
            String fullyQualifiedModel = modelPath + File.separatorChar + modelName;
            LOGGER.info("Model path: {}", modelPath);
            model = ModelLoader.loadModel(Path.of(fullyQualifiedModel), 4000, true);
            LOGGER.info("Model loaded successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Llama model", e);
        }
        return model;
    }
}
