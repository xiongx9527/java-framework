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
    public void shouldRegisterOnlyLoadableBoot3AutoConfigurations() throws Exception {
        String factories = read("META-INF/spring.factories");
        String imports = read("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
        String autoConfiguration = "com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure.AskDataAutoConfiguration";
        List<String> autoConfigurations = Arrays.asList(imports.split("\\R"));
        List<String> expectedAutoConfigurations = Arrays.asList(
                "com.xiongx9527.fw.plus.ai.autoconfigure.BaseAutoConfiguration",
                "com.xiongx9527.fw.plus.ai.autoconfigure.model.AiModelAutoConfiguration",
                "com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration",
                "com.xiongx9527.fw.plus.ai.autoconfigure.store.embedding.AiEmbeddingStoreAutoConfiguration",
                "com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure.LangChain4jModelAskDataAutoConfiguration",
                autoConfiguration,
                "com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.LangChain4jAdapterAutoConfiguration");
        assertTrue(factories.contains(autoConfiguration));
        Set<String> uniqueAutoConfigurations = new HashSet<>(autoConfigurations);
        assertEquals(autoConfigurations.size(), uniqueAutoConfigurations.size());
        for (String configuration : autoConfigurations) {
            Class.forName(configuration);
        }
        assertEquals(expectedAutoConfigurations, autoConfigurations);
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
