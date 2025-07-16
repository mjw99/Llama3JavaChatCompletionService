package com.llama4j.core;

import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

interface Timer extends AutoCloseable {

    @Override
    void close(); // no Exception

    @Contract(value = "_ -> new", pure = true)
    static @NotNull Timer log(String label) {
        return log(label, TimeUnit.MILLISECONDS);
    }

    @Contract(value = "_, _ -> new", pure = true)
    static @NotNull Timer log(String label, TimeUnit timeUnit) {
        return new Timer() {
            final long startNanos = System.nanoTime();

            @Override
            public void close() {
                long elapsedNanos = System.nanoTime() - startNanos;
                System.out.printf("%s: %s %s%n",
                    label,
                    timeUnit.convert(elapsedNanos, TimeUnit.NANOSECONDS),
                    timeUnit.toChronoUnit().name().toLowerCase());
            }
        };
    }
}