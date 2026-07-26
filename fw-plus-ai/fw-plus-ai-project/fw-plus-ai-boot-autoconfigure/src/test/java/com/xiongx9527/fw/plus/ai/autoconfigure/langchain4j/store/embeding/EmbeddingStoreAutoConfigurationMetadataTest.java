package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding;

import org.junit.Test;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmbeddingStoreAutoConfigurationMetadataTest {

    @Test
    public void shouldRegisterAutoConfigurationInSpringFactoriesOnly() throws Exception {
        String factories = read("META-INF/spring.factories");

        assertTrue(factories.contains("com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.EmbeddingStoreAutoConfiguration"));
        assertFalse(existsInStarter("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"));
        assertFalse(existsInStarter("META-INF/spring/org.springframework.boot.autoconfig.AutoConfiguration.imports"));
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

    private boolean existsInStarter(String location) throws Exception {
        File classesDirectory = new File(EmbeddingStoreAutoConfiguration.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        return new File(classesDirectory, location).isFile();
    }
}
