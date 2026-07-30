package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AskDataAutoConfigurationRegistrationTest {

    @Test
    public void shouldRegisterAskDataAutoConfigurationInBothMetadataFiles() throws Exception {
        String factories = read("META-INF/spring.factories");
        String imports = read("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
        String autoConfiguration = "com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure.AskDataAutoConfiguration";
        List<String> autoConfigurations = Arrays.asList(imports.split("\\R"));
        List<String> expectedAutoConfigurations = Arrays.asList(
                "com.xiongx9527.fw.plus.ai.autoconfigure.BaseAutoConfiguration",
                "com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAutoConfiguration",
                "com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model.LangChain4jModelAutoConfiguration",
                "com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration",
                "com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.EmbeddingStoreAutoConfiguration",
                "com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure.LangChain4jModelAskDataAutoConfiguration",
                autoConfiguration,
                "com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.LangChain4jAdapterAutoConfiguration");
        assertTrue(factories.contains(autoConfiguration));
        assertTrue(autoConfigurations.containsAll(expectedAutoConfigurations));
        Set<String> uniqueAutoConfigurations = new HashSet<>(autoConfigurations);
        assertEquals(autoConfigurations.size(), uniqueAutoConfigurations.size());
    }

    private String read(String path) throws Exception {
        Scanner scanner = new Scanner(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8.name());
        try {
            return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        } finally {
            scanner.close();
        }
    }
}
