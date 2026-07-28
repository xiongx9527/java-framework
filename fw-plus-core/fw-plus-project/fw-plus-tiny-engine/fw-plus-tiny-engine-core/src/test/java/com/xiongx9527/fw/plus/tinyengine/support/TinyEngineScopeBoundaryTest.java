package com.xiongx9527.fw.plus.tinyengine.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TinyEngineScopeBoundaryTest {

	private static final List<String> OUT_OF_SCOPE_MARKERS = Arrays.asList(
		"Controller",
		"OpenAPI",
		"Swagger",
		"ServletFilter",
		"SpringBootApplication",
		"Docker",
		"LangChain4j",
		"VectorStorage",
		"RAG",
		"jakarta.",
		"com.tinyengine.it"
	);

	@Test
	void shouldNotContainOutOfScopeTypesOrDependencies() throws IOException, URISyntaxException {
		Path moduleDirectory = moduleDirectory();
		assertTrue(Files.isDirectory(moduleDirectory), "应能定位 TinyEngine core 模块目录");

		assertDirectoryDoesNotContainOutOfScopeMarkers(moduleDirectory.resolve("src/main/java"));
		assertDirectoryDoesNotContainOutOfScopeMarkers(moduleDirectory.resolve("src/main/resources"));
		assertDirectoryDoesNotContainOutOfScopeMarkers(moduleDirectory.resolve("target/classes"));
	}

	private Path moduleDirectory() throws URISyntaxException {
		Path testClassesDirectory = Paths.get(TinyEngineScopeBoundaryTest.class.getProtectionDomain()
			.getCodeSource().getLocation().toURI());
		return testClassesDirectory.getParent().getParent();
	}

	private void assertDirectoryDoesNotContainOutOfScopeMarkers(Path directory) throws IOException {
		assertTrue(Files.isDirectory(directory), "应存在待检查目录: " + directory);
		try (Stream<Path> paths = Files.walk(directory)) {
			Iterator<Path> iterator = paths.filter(Files::isRegularFile).iterator();
			while (iterator.hasNext()) {
				Path file = iterator.next();
				String content = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
				for (String marker : OUT_OF_SCOPE_MARKERS) {
					assertFalse(file.toString().contains(marker) || content.contains(marker),
						"TinyEngine core 不应包含范围外标识 " + marker + ": " + file);
				}
			}
		}
	}
}
