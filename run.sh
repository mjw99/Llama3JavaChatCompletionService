#!/bin/zsh

# Set environment variables
# export JAVA_HOME=/Library/Java/JavaVirtualMachines/liberica-jdk-21.jdk/Contents/Home
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-23.jdk/Contents/Home

java_config=$(cat <<EOF -XX:+UseG1GC -XX:MaxGCPauseMillis=500 -XX:InitiatingHeapSizePercent=40 -XX:GCTimeRatio=7 -XX:MaxGCTimeRatio=14 EOF )

export JAVA_CONFIG=$java_config

# Run Maven and Java
mvn clean package

# Start the LLama3 server
java \
  --enable-preview \
  --add-modules jdk.incubator.vector \
  -Xmx4g \
  -jar target/llama3-server-1.0.0-SNAPSHOT.jar \
  --spring.config.location=classpath:/application.properties \
  "$@"
