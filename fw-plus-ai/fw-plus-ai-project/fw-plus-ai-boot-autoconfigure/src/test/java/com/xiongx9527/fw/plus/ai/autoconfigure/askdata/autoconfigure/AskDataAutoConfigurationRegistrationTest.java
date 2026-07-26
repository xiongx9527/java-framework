package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AskDataAutoConfigurationRegistrationTest {

    @Test
    public void shouldRegisterAskDataAutoConfigurationInSpringFactoriesOnly() throws Exception {
        String factories = read("META-INF/spring.factories");
        String autoConfiguration = "com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure.AskDataAutoConfiguration";
        assertTrue(factories.contains(autoConfiguration));
        assertFalse(existsInAutoconfigure("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"));
    }

    private boolean existsInAutoconfigure(String location) throws Exception {
        File classesDirectory = new File(AskDataAutoConfiguration.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        return new File(classesDirectory, location).isFile();
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
