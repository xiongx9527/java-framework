package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.TinyEngineProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEngineAppMvcController;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEngineMaterialMvcController;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEngineModelMvcController;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEnginePageMvcController;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TinyEngineWebFluxApiContractTest {

	private final ReactiveWebApplicationContextRunner reactiveRunner = new ReactiveWebApplicationContextRunner()
			.withUserConfiguration(WebFluxApiConfiguration.class)
			.withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
					WebFluxAutoConfiguration.class, TinyEngineReactiveWebConfiguration.class))
			.withPropertyValues("fw.plus.tiny-engine.web.enabled=true");

	@Test
	void reactiveContextRegistersApiControllersAndNoMvcControllers() {
		reactiveRunner.run(context -> {
			assertThat(context).hasSingleBean(TinyEngineAppWebFluxController.class);
			assertThat(context).hasSingleBean(TinyEnginePageWebFluxController.class);
			assertThat(context).hasSingleBean(TinyEngineMaterialWebFluxController.class);
			assertThat(context).hasSingleBean(TinyEngineModelWebFluxController.class);
			assertThat(context).doesNotHaveBean(TinyEngineAppMvcController.class);
			assertThat(context).doesNotHaveBean(TinyEnginePageMvcController.class);
			assertThat(context).doesNotHaveBean(TinyEngineMaterialMvcController.class);
			assertThat(context).doesNotHaveBean(TinyEngineModelMvcController.class);

			Set<String> mappings = context.getBean(RequestMappingHandlerMapping.class).getHandlerMethods().keySet()
					.stream()
					.flatMap(info -> info.getPatternsCondition().getPatterns().stream())
					.map(Object::toString)
					.collect(Collectors.toCollection(LinkedHashSet::new));
			assertThat(mappings).contains(
					"/app-center/api/apps/page",
					"/app-center/api/apps/create",
					"/app-center/api/apps/detail/{id}",
					"/app-center/v1/api/apps/schema/{id}",
					"/app-center/api/pages/create",
					"/app-center/api/pages/update/{id}",
					"/app-center/api/pages/list/{appId}",
					"/app-center/api/pages/detail/{id}",
					"/material-center/api/blocks",
					"/material-center/api/block",
					"/material-center/api/block/detail/{id}",
					"/material-center/api/block/create",
					"/material-center/api/block/update/{id}",
					"/material-center/api/block/delete/{id}",
					"/material-center/api/block/deploy",
					"/material-center/api/block/tags",
					"/material-center/api/block/notgroup/{groupId}",
					"/material-center/api/block-groups",
					"/material-center/api/block-categories",
					"/material-center/api/business-category/list",
					"/material-center/api/model/list",
					"/material-center/api/model/detail/{id}",
					"/material-center/api/model/create",
					"/material-center/api/model/update/{id}",
					"/material-center/api/model/delete/{id}",
					"/app-center/api/sources/list/{appId}",
					"/app-center/api/sources/detail/{id}",
					"/app-center/api/sources/create",
					"/app-center/api/sources/update/{id}",
					"/app-center/api/sources/delete/{id}",
					"/app-center/api/app-template/list",
					"/app-center/api/app-template/create",
					"/material-center/api/resource-group/list",
					"/material-center/api/resource/list",
					"/app-center/api/i18n/entries",
					"/app-center/api/apps/{appId}/i18n/entries/update");
		});
	}

	@Test
	void apiFacadeCallsRunOnTinyEngineBlockingScheduler() {
		TinyEngineApiFacade facade = mock(TinyEngineApiFacade.class);
		App app = new App();
		app.setId(Integer.valueOf(1));
		app.setName("reactive-app");
		when(facade.appDetail(Integer.valueOf(1))).thenAnswer(invocation -> {
			assertThat(Thread.currentThread().getName()).startsWith("tiny-engine-blocking-test");
			return app;
		});
		when(facade.createApp(any(App.class))).thenReturn(app);
		TinyEngineReactiveExecutor executor = new TinyEngineReactiveExecutor(
				Schedulers.newSingle("tiny-engine-blocking-test"), Duration.ofSeconds(5));
		WebTestClient client = WebTestClient.bindToController(new TinyEngineAppWebFluxController(facade, executor))
				.build();

		client.get().uri("/app-center/api/apps/detail/1")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.data.id").isEqualTo(1)
				.jsonPath("$.data.name").isEqualTo("reactive-app");
		verify(facade).appDetail(Integer.valueOf(1));

		client.post().uri("/app-center/api/apps/create")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"reactive-app\"}")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.data.id").isEqualTo(1);
		verify(facade).createApp(any(App.class));
	}

	@Configuration
	static class WebFluxApiConfiguration {

		@Bean
		TinyEngineApiFacade tinyEngineApiFacade() {
			return mock(TinyEngineApiFacade.class);
		}

		@Bean
		TinyEngineProperties tinyEngineProperties() {
			return new TinyEngineProperties();
		}
	}
}
