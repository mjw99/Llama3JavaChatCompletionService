package com.llama4j.core;

@FunctionalInterface
public interface Sampler {
    int sampleToken(FloatTensor logits);

    Sampler ARGMAX = FloatTensor::argmax;
}
