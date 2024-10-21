# Llama3.java Inference with OpenAI Chat Completion REST API ☕️

This project provides a REST API wrapper for the amazing [Llama3.java project](https://github.com/mukel/llama3.java) from Alfonso² Peterssen. 
The wrapper is compliant with the OpenAI API specification for chat completions.

## ToDo 

- [X] SpringBoot wrapper around Llama3.java
- [X] Create Java Flame graph to see where performance issue's are located (matmul 🔥)
- [ ] Optional: Quarkus wrapper around Llama3.java
- [ ] TornadoVM enabled version
- [ ] GraalVM native version
- [ ] LLM Sharding (Layers, Attn Head)
- [ ] BitNets support 
- [ ] Ternary Models support

## On Apple Silicon (M1/M2/M3)

Make sure to download an ARM compliant SDK, for example from https://bell-sw.com/pages/downloads/#jdk-21-lts 

https://github.com/user-attachments/assets/6fecb9c1-6c84-4a01-a63b-272e75009618

## Setup

Set the JAVA_HOME environment variable to the ARM SDK path. For example:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/liberica-jdk-21.jdk/Contents/Home
```

IMPORTANT: Do not use SDKMan because this will fall back to the x86 version of the SDK.

## Build 

```bash
mvn clean package
```

## Download LLM

Download a GGUF model from the Hugging Face model hub and place it in the 'models' directory. 
For example:

```bash
mkdir models 
cd models
curl https://huggingface.co/hugging-quants/Llama-3.2-1B-Instruct-Q8_0-GGUF/blob/main/llama-3.2-1b-instruct-q8_0.gguf
```

Update the 'llama.model.name' variable in the application.properties file if you use a different model.

```application.properties
spring.application.name=Llama3.java Server
server.servlet.context-path=/

llama.model.path=models
llama.model.name=Meta-Llama-3.2-1b-instruct-Q8_0.gguf

logging.level.com.llama4j=INFO

server.address=localhost
server.port=8080
```

## Run 

Start the Spring Boot app which holds the Llama3.java REST wrapper as follows:

```bash
java --add-modules jdk.incubator.vector --enable-preview -jar target/OpenAIRestWrapper-0.0.1.jar
```

## Test using Curl

```bash
curl -X POST http://localhost:8080/chat/completions \
-H "Content-Type: application/json" \
-d '{
  "messages": [
    {
      "role": "system",
      "content": "You are a comedian."
    },
    {
      "role": "user",
      "content": "Tell me a joke."
    }
  ],
  "temperature": 0.7,
  "top_p": 0.95,
  "max_tokens": 100
}'
```

Response

```json
{
   "id":"chatcmpl-1",
   "object":"chat.completion",
   "created":1729447400,
   "model":"Meta-Llama-3.2-1b-instruct-Q8_0.gguf",
   "systemFingerprint":"fp_178ce5010c913",
   "choices":[
      {
         "index":0,
         "message":{
            "role":"assistant",
            "content":"A man walked into a library and asked the librarian, \"Do you have any books on Pavlov's dogs and Schrödinger's cat?\" The librarian replied, \"It rings a bell, but I'm not sure if it's here or not.\""
         },
         "logprobs":null,
         "finishReason":"stop"
      }
   ],
   "usage":{
      "promptTokens":25,
      "completionTokens":53,
      "totalTokens":78,
      "completionTokensDetails":{
         "reasoningTokens":0
      }
   }
```

## Test using DevoxxGenie 

Select "Jlama (Experimental)" or "Exo (Experimental)" which both use the OpenAI Chat Completion.

Example with file attachment in prompt context:

![Demo2](https://github.com/user-attachments/assets/cbd8af2e-d3bd-4d9a-bdf5-0c2bc033915f)

## Baseline Performance Stats

Running on Apple M1 Max with 64Gb (LPDDR5) of RAM (32 number of Cores).  

![CallTree](https://github.com/user-attachments/assets/75e739e2-44b9-4e2b-a077-63021cb9ea39)

### Key Findings by ChatGPT

This profiling trace shows a CPU-heavy Java application, likely dealing with machine learning or vectorized computation. Here's a breakdown of key components:

1. **Heavy CPU Usage (`java.util.concurrent.ForkJoinWorkerThread.run`) at 86%**
   - This is likely a thread-pool executor used for parallelizing tasks. It suggests significant multi-threaded execution, possibly parallelized matrix operations or tensor calculations.

2. **`com.llama4j.core.FloatTensor$$Lambda.accept` (61.5%)**
   - This method involves processing a tensor's float data. Lambda expressions in Java are anonymous functions, often used for concise representations of callbacks or functional programming.

3. **`jdk.incubator.vector.FloatVector.reduceLanes` (49.5%)**
   - Indicates vectorized computation involving float vectors. This uses the Vector API from the JDK's incubator module, designed to perform operations on wide vectors leveraging CPU SIMD (Single Instruction, Multiple Data) capabilities.

4. **`com.llama4j.core.ArrayFloatTensor.getFloatVector` (7.7%)**
   - Suggests fetching float vectors from an array-based tensor representation. This could be another bottleneck related to memory access when performing operations.

5. **`com.llama4j.web.rest.LlamaWrapperApplication.chatCompletions` (13.9%)**
   - Indicates that part of the time is spent processing chat completion requests, suggesting this application is likely an LLM interface or chatbot.

6. **`com.llama4j.core.FloatTensor.matmul` (12.0%)**
   - This is the matrix multiplication function, which contributes to the linear algebra operations in the application, likely forming the backbone of the model's computations.

### Potential Bottlenecks and Optimization Ideas

- **ForkJoinWorkerThread**: If this thread is consuming 86% of the CPU, there might be room to optimize the parallelization strategy. Investigate if there’s overhead or contention between threads.
- **Vectorization**: The use of `jdk.incubator.vector.FloatVector` shows a good attempt at leveraging vectorized operations, but it may need tuning based on the target CPU’s vector width (e.g., AVX-512 support).
- **Memory Access**: The significant time spent in `ArrayFloatTensor.getFloatVector` could indicate a memory bandwidth bottleneck. This might benefit from optimizing data locality or using more efficient memory layouts (like row-major or column-major order).

---

### Key Findings by Claude

1. **FloatTensor Operations: 61.5% of execution time**

com.llama4j.core.FloatTensor$$Lambda$0x0000000080143b048.accept(int)
Likely the core model inference bottleneck

2. **Vector Operations: 49.5% of execution time**

jdk.incubator.vector.Float128Vector.reduceLanes(VectorOperators$Associative)
Part of the FloatTensor operations

3. **Array Processing: 7.7% of execution time**

com.llama4j.core.ArrayFloatTensor.getFloatVector(VectorSpecies, int)

4. **HTTP Request Handling: 13.9% of execution time**

com.llama4j.web.rest.LlamaWrapperApplication.chatCompletions(ChatCompletionRequest)

#### Recommendations

1. **Optimize Tensor Operations:**

Profile the FloatTensor class to identify specific bottlenecks
Consider using more efficient linear algebra libraries or GPU acceleration
Optimize memory access patterns and data structures

2. **Improve Vector Processing:**

Investigate the use of more efficient vector operations or libraries
Consider using SIMD instructions if not already implemented

3. **Enhance Array Processing:**

Optimize the getFloatVector method in ArrayFloatTensor
Consider using more efficient data structures or access patterns

4. **Optimize HTTP Request Handling:**

Profile the chatCompletions method to identify specific bottlenecks
Consider implementing caching mechanisms or request batching
Optimize data serialization/deserialization if applicable

#### General Optimizations:

- Implement multi-threading for parallel processing where applicable
- Optimize memory usage and garbage collection
- Consider using a more performant JVM or JIT compiler

---

## Credits

This is just a simple Spring Boot OpenAI REST wrapper around the amazing [Llama3.java project](https://github.com/mukel/llama3.java) from Alfonso² Peterssen! 

Thanks Alfonso for leading the way! 💪🏻 ☕️
