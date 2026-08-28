package com.xiongx9527.fw.plus.ai.autoconfigure;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AutoConfigurationMetadataConsistencyTest {

	private static final String ENABLE_AUTO_CONFIGURATION =
			"org.springframework.boot.autoconfigure.EnableAutoConfiguration";
	private static final String EXCLUDED_AUTO_CONFIGURATION =
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAutoConfiguration";
	private static final String SPRING_FACTORIES = "src/main/resources/META-INF/spring.factories";
	private static final String AUTO_CONFIGURATION_IMPORTS =
			"src/main/resources/META-INF/spring/"
					+ "org.springframework.boot.autoconfigure.AutoConfiguration.imports";
	private static final List<String> AUTO_CONFIGURATION_MODULES = Arrays.asList(
			"fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure",
			"fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-autoconfigure",
			"fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure");

	@Test
	public void shouldKeepAutoConfigurationMetadataConsistentAcrossModules() throws Exception {
		File repositoryRoot = findRepositoryRoot();
		for (String modulePath : AUTO_CONFIGURATION_MODULES) {
			File moduleDirectory = new File(repositoryRoot, modulePath);
			List<String> factories = factoryAutoConfigurations(moduleDirectory);
			List<String> imports = importedAutoConfigurations(moduleDirectory);

			assertEquals(withoutExcluded(factories), withoutExcluded(imports),
					modulePath + " auto-configuration metadata should declare the same entries");
		}
	}

	private List<String> factoryAutoConfigurations(File moduleDirectory) throws Exception {
		Properties properties = new Properties();
		properties.load(new StringReader(read(new File(moduleDirectory, SPRING_FACTORIES))));
		String value = properties.getProperty(ENABLE_AUTO_CONFIGURATION);
		assertTrue(value != null, moduleDirectory + " spring.factories must declare EnableAutoConfiguration");
		return split(value, ",");
	}

	private List<String> importedAutoConfigurations(File moduleDirectory) throws Exception {
		List<String> imports = new ArrayList<String>();
		for (String line : read(new File(moduleDirectory, AUTO_CONFIGURATION_IMPORTS)).split("\\R")) {
			String value = line.trim();
			if (!value.isEmpty() && !value.startsWith("#")) {
				imports.add(value);
			}
		}
		return imports;
	}

	private List<String> split(String value, String delimiter) {
		List<String> values = new ArrayList<String>();
		for (String item : value.split(delimiter)) {
			String trimmed = item.trim();
			if (!trimmed.isEmpty()) {
				values.add(trimmed);
			}
		}
		return values;
	}

	private Set<String> withoutExcluded(List<String> values) {
		Set<String> filtered = new HashSet<String>(values);
		filtered.remove(EXCLUDED_AUTO_CONFIGURATION);
		return filtered;
	}

	private String read(File file) throws Exception {
		assertTrue(file.isFile(), "Metadata file should exist: " + file.getAbsolutePath());
		return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
	}

	private File findRepositoryRoot() {
		File currentDirectory = new File(System.getProperty("user.dir"));
		while (currentDirectory != null) {
			if (new File(currentDirectory, AUTO_CONFIGURATION_MODULES.get(0)).isDirectory()) {
				return currentDirectory;
			}
			currentDirectory = currentDirectory.getParentFile();
		}
		throw new IllegalStateException("Cannot locate fw-plus repository root");
	}
}
