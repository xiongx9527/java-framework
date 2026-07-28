package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AskDataAutoConfigurationRegistrationTest {

    @Test
    public void shouldRegisterAskDataAutoConfigurationInBothMetadataFiles() throws Exception {
        String factories = read("META-INF/spring.factories");
        String imports = read("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
        String autoConfiguration = "com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure.AskDataAutoConfiguration";
        assertTrue(factories.contains(autoConfiguration));
        assertTrue(imports.contains(autoConfiguration));
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
