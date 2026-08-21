package com.xiongx9527.fw.plus.ai.autoconfigure.store.embedding;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;

public class AiEmbeddingStoreAutoConfigurationMetadataTest {

    @Test
    public void shouldRegisterAutoConfigurationInSpringFactories() throws Exception {
        String factories = read("META-INF/spring.factories");

        String autoConfiguration = "com.xiongx9527.fw.plus.ai.autoconfigure.store.embedding.AiEmbeddingStoreAutoConfiguration";
        assertTrue(factories.contains(autoConfiguration));
        assertTrue(factories.contains("com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.LangChain4jAdapterAutoConfiguration"));

        String imports = read("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
        assertTrue(imports.contains("com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.LangChain4jAdapterAutoConfiguration"));
    }

    private String read(String location) throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(location);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append('\n');
            }
            return result.toString();
        } finally {
            inputStream.close();
        }
    }
}
