package com.llama4j.config;

import com.llama4j.core.Llama;
import com.llama4j.core.ModelLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Path;

@Configuration
public class ModelConfig {
    private static final Logger LOG = LoggerFactory.getLogger(ModelConfig.class);

    @Value("${llama.model.path}")
    private String modelPath;

    @Value("${llama.model.name}")
    private String modelName;

    @Bean
    public Llama loadModel() {
        Llama model;
        try {
            LOG.debug("Using model path: {}", modelPath);
            LOG.debug("Using model filename: {}", modelName);

            String fullyQualifiedModel = modelPath + File.separatorChar + modelName;

            // TODO Max tokens should be part of request parameters
            model = ModelLoader.loadModel(Path.of(fullyQualifiedModel), 4000, true);
            LOG.info("Model loaded successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Llama model", e);
        }
        return model;
    }
}
