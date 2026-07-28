package com.xiongx9527.fw.plus.boot.magicapi;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
		classes = MagicapiSampleApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("all-plugins")
class MagicApiAllPluginsApplicationContextTest {

	private static final Path RESOURCE_DIRECTORY = createResourceDirectory();

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private ConfigurableEnvironment environment;

	@Autowired
	private TestRestTemplate restTemplate;

	@DynamicPropertySource
	static void resourceProperties(DynamicPropertyRegistry registry) {
		registry.add("magic-api.resource.location", () -> RESOURCE_DIRECTORY.toString());
	}

	@Test
	void startsRealSampleWithoutApolloOrExternalResources() {
		assertThat(applicationContext.getBeansOfType(MagicResourceService.class)).hasSize(1);
		assertThat(applicationContext.containsBean("apolloConfigChangeAdapter")).isFalse();
		assertThat(environment.getProperty("magic-api.resource.location"))
				.isEqualTo(RESOURCE_DIRECTORY.toString());
		assertThat(StreamSupport.stream(environment.getPropertySources().spliterator(), false)
				.map(PropertySource::getName))
				.noneMatch(name -> name.toLowerCase(Locale.ROOT).contains("apollo"));
	}

	@Test
	void exposesHealthAndMagicEditor() {
		ResponseEntity<String> health = restTemplate.getForEntity("/actuator/health", String.class);
		assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(health.getBody()).contains("\"status\":\"UP\"");

		ResponseEntity<String> editor = restTemplate.getForEntity("/magic-api/web/index.html", String.class);
		assertThat(editor.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(editor.getBody()).contains("<title>magic-api</title>");
	}

	private static Path createResourceDirectory() {
		try {
			return Files.createTempDirectory("fw-plus-magicapi-all-plugins-");
		} catch (IOException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}

	@AfterAll
	static void deleteResourceDirectory() throws IOException {
		List<Path> resources;
		try (Stream<Path> paths = Files.walk(RESOURCE_DIRECTORY)) {
			resources = paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
		}
		try {
			assertThat(resources)
					.filteredOn(path -> !path.equals(RESOURCE_DIRECTORY))
					.allMatch(Files::isDirectory);
		} finally {
			for (Path resource : resources) {
				Files.deleteIfExists(resource);
			}
		}
	}
}
