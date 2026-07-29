package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmbeddingStoreAutoConfigurationMetadataTest {

    @Test
    public void shouldRegisterLoadableAutoConfigurationsInBothMetadataFiles() throws Exception {
        String factories = read("META-INF/spring.factories");
        String imports = read("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");

        String autoConfiguration = "com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.EmbeddingStoreAutoConfiguration";
        String rootAutoConfiguration = "com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAutoConfiguration";
        assertTrue(factories.contains(autoConfiguration));
        assertTrue(factories.contains(rootAutoConfiguration));
        assertTrue(imports.contains(autoConfiguration));
        assertTrue(imports.contains(rootAutoConfiguration));
        assertSpringFactoriesClassesAreLoadable();
        assertImportCandidatesAreLoadable(imports);
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

    private void assertSpringFactoriesClassesAreLoadable() throws Exception {
        Properties factories = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("META-INF/spring.factories");
        assertNotNull(inputStream);
        try {
            factories.load(inputStream);
        } finally {
            inputStream.close();
        }
        String classNames = factories.getProperty("org.springframework.boot.autoconfigure.EnableAutoConfiguration");
        assertNotNull(classNames);
        assertClassNamesAreLoadable(classNames.split(","));
    }

    private void assertImportCandidatesAreLoadable(String imports) throws Exception {
        assertClassNamesAreLoadable(imports.split("\\r?\\n"));
    }

    private void assertClassNamesAreLoadable(String[] classNames) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        for (String className : classNames) {
            String candidate = className.trim();
            if (!candidate.isEmpty()) {
                Class.forName(candidate, false, classLoader);
            }
        }
    }
}
