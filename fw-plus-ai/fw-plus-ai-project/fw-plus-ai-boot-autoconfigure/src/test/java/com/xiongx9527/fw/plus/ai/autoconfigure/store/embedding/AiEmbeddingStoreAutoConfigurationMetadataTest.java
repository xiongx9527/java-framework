package com.xiongx9527.fw.plus.ai.autoconfigure.store.embedding;

import org.junit.Test;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AiEmbeddingStoreAutoConfigurationMetadataTest {

    @Test
    public void shouldRegisterAutoConfigurationInSpringFactories() throws Exception {
        String factories = read("META-INF/spring.factories");

        String autoConfiguration = "com.xiongx9527.fw.plus.ai.autoconfigure.store.embedding.AiEmbeddingStoreAutoConfiguration";
        assertTrue(factories.contains(autoConfiguration));
        assertFalse(factories.contains("com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAutoConfiguration"));
        assertFalse(factories.contains("com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.EmbeddingStoreAutoConfiguration"));
        assertFalse(existsInSourceResources("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"));
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

    private boolean existsInSourceResources(String location) throws Exception {
        File classesDirectory = new File(AiEmbeddingStoreAutoConfiguration.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        File moduleDirectory = classesDirectory.getParentFile().getParentFile();
        return new File(new File(moduleDirectory, "src/main/resources"), location).isFile();
    }
}
