# Llama3.java Inference with OpenAI Chat Completion REST API ☕️

This project provides a REST API wrapper for the Llama3.java model. 
The wrapper is compliant with the OpenAI API specification for chat completions.

## ToDo 

- [X] SpringBoot wrapper around Llama3.java
- [ ] TornadoVM enabled version
- [ ] GraalVM native version
- [ ] Create Java Flame graph to see where performance issue's are located (matmul 🔥)
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

## Test using DevoxxGenie 

Select "Jlama (Experimental)" or "Exo (Experimental)" which both use the OpenAI Chat Completion.

Example with file attachment in prompt context:

![DemoLlama3java](https://github.com/user-attachments/assets/19b9194e-77f8-4c18-9224-8dcd266d14b0)

## Credits

This is just a simple Spring Boot OpenAI REST wrapper around the amazing [Llama3.java project](https://github.com/mukel/llama3.java) from Alfonso² Peterssen! 
Thanks Alfonso for leading the way! 💪🏻 ☕️
