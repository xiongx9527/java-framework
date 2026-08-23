#!/usr/bin/env zsh

set -eu

maven_cmd=${MAVEN_CMD:-mvn}
java_major=$($JAVA_HOME/bin/java -version 2>&1 | head -n 1)
if [[ "$java_major" != *'17.'* ]]; then
  print -u2 -- "需要 Java 17，当前为: $java_major"
  exit 1
fi

probe_root=$(mktemp -d "${TMPDIR:-/tmp}/fw-plus-spring-ai-probe.XXXXXX")
trap 'rm -rf "$probe_root"' EXIT
mkdir -p "$probe_root/src/main/java/probe"

cat > "$probe_root/src/main/java/probe/SpringAiApiProbe.java" <<'JAVA'
package probe;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;

import java.util.List;

final class SpringAiApiProbe {
    private SpringAiApiProbe() {
    }

    static void verify(ChatModel chatModel, EmbeddingModel embeddingModel,
            DashScopeChatModel dashScopeChatModel, DashScopeEmbeddingModel dashScopeEmbeddingModel) {
        ChatResponse chatResponse = chatModel.call(new Prompt("probe"));
        Generation generation = chatResponse.getResult();
        String text = generation.getOutput().getText();
        String finishReason = generation.getMetadata().getFinishReason();
        Integer chatTokens = chatResponse.getMetadata().getUsage().getPromptTokens();

        EmbeddingResponse embeddingResponse = embeddingModel.call(
                new EmbeddingRequest(List.of("probe"), null));
        List<Embedding> embeddings = embeddingResponse.getResults();
        float[] vector = embeddings.get(0).getOutput();
        EmbeddingResponseMetadata metadata = embeddingResponse.getMetadata();
        Integer embeddingTokens = metadata.getUsage().getPromptTokens();

        ChatModel chatContract = dashScopeChatModel;
        EmbeddingModel embeddingContract = dashScopeEmbeddingModel;
        if (text == null || finishReason == null || chatTokens == null || vector.length == 0
                || embeddingTokens == null || chatContract == null || embeddingContract == null) {
            throw new IllegalStateException("Spring AI compatibility probe failed");
        }
    }
}
JAVA

for boot_version in 3.5.7 3.5.8; do
  cat > "$probe_root/pom.xml" <<XML
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>${boot_version}</version>
    <relativePath/>
  </parent>
  <groupId>probe</groupId>
  <artifactId>fw-plus-spring-ai-probe</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <properties>
    <java.version>17</java.version>
    <spring-ai.version>1.1.2</spring-ai.version>
    <spring-ai-alibaba.version>1.1.2.2</spring-ai-alibaba.version>
  </properties>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-bom</artifactId>
        <version>\${spring-ai.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-bom</artifactId>
        <version>\${spring-ai-alibaba.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-extensions-bom</artifactId>
        <version>\${spring-ai-alibaba.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-model</artifactId>
    </dependency>
    <dependency>
      <groupId>com.alibaba.cloud.ai</groupId>
      <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
    </dependency>
  </dependencies>
</project>
XML

  print -- "验证 Spring Boot ${boot_version} + Spring AI 1.1.2 + Spring AI Alibaba 1.1.2.2"
  "$maven_cmd" -B -f "$probe_root/pom.xml" clean compile
  "$maven_cmd" -B -f "$probe_root/pom.xml" dependency:tree \
    '-Dincludes=org.springframework.ai:*,com.alibaba.cloud.ai:*,org.springframework.boot:*'
done

print -- "Spring AI compatibility probe passed"
