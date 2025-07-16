package com.llama4j.core;

import java.util.random.RandomGenerator;

import org.jetbrains.annotations.NotNull;

record CategoricalSampler(RandomGenerator rng) implements Sampler {

    @Override
    public int sampleToken(@NotNull FloatTensor logits) {
        // sample index from probabilities (they must sum to 1!)
        float random0to1 = rng.nextFloat(1f);
        float cdf = 0.0f;
        for (int i = 0; i < logits.size(); i++) {
            cdf += logits.getFloat(i);
            if (random0to1 < cdf) {
                return i;
            }
        }
        return logits.size() - 1; // in case of rounding errors
    }
}