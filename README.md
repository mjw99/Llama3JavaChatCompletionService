# Llama3.java with OpenAI Chat Completion REST API

This project provides a REST API wrapper for the Llama3.java model. 
The wrapper is compliant with the OpenAI API specification for chat completions.

## On Apple Silicon (M1/M2/M3)

Make sure to download an ARM compliant SDK, for example from https://bell-sw.com/pages/downloads/#jdk-21-lts 

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

## Run 

Run the Llama3.java REST wrapper:

```bash
java --add-modules jdk.incubator.vector --enable-preview -jar target/OpenAIRestWrapper-0.0.1.jar
```

## Test Local LLM

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
