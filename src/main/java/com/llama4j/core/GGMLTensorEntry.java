package com.llama4j.core;

import java.lang.foreign.MemorySegment;

record GGMLTensorEntry(MemorySegment mappedFile,
                       String name, GGMLType ggmlType, int[] shape,
                       MemorySegment memorySegment) {
}