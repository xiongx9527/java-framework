package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.TinyEngineProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineExternalCapability;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEngineAppMvcController;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEngineMaterialMvcController;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEngineModelMvcController;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEnginePageMvcController;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicInsert;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicInsertResult;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicPageResult;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicQuery;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.DynamicService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppMemberDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppMemberRoleRequest;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AppExtension;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineRequestUserScope;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
			assertThat(context).hasSingleBean(TinyEngineHistoryWebFluxController.class);
			assertThat(context).hasSingleBean(TinyEngineI18nWebFluxController.class);
			assertThat(context).hasSingleBean(TinyEngineTaskWebFluxController.class);
			assertThat(context).hasSingleBean(TinyEnginePlatformWebFluxController.class);
			assertThat(context).hasSingleBean(TinyEngineCodegenWebFluxController.class);
			assertThat(context).hasSingleBean(TinyEngineDynamicModelWebFluxController.class);
			assertThat(context).doesNotHaveBean(TinyEngineAppMvcController.class);
			assertThat(context).doesNotHaveBean(TinyEnginePageMvcController.class);
			assertThat(context).doesNotHaveBean(TinyEngineMaterialMvcController.class);
			assertThat(context).doesNotHaveBean(TinyEngineModelMvcController.class);

			Set<String> mappings = context.getBean(RequestMappingHandlerMapping.class).getHandlerMethods().keySet()
					.stream()
					.flatMap(info -> info.getPatternsCondition().getPatterns().stream())
					.map(Object::toString)
					.map(TinyEngineWebFluxApiContractTest::normalizeTemplateVariables)
					.collect(Collectors.toCollection(LinkedHashSet::new));
			assertThat(mappings).containsAll(implementedWebFluxContractPaths());
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

	@Test
	void memberRoutesDelegateToTheSameCoreFacadeOperations() {
		TinyEngineApiFacade facade = mock(TinyEngineApiFacade.class);
		AppMemberDto member = new AppMemberDto();
		member.setUserId("8");
		member.setRoleCode("app_dev");
		when(facade.appMembers(Integer.valueOf(1))).thenReturn(Collections.singletonList(member));
		when(facade.addAppMember(any(Integer.class), any(AppMemberRoleRequest.class))).thenReturn(member);
		when(facade.changeAppMember(any(Integer.class), any(String.class), any(AppMemberRoleRequest.class)))
				.thenReturn(member);
		TinyEngineReactiveExecutor executor = new TinyEngineReactiveExecutor(
				Schedulers.newSingle("tiny-engine-member-test"), Duration.ofSeconds(5));
		WebTestClient client = WebTestClient.bindToController(new TinyEngineAppWebFluxController(facade, executor))
				.build();

		client.get().uri("/app-center/api/apps/1/members").exchange().expectStatus().isOk()
				.expectBody().jsonPath("$.data[0].roleCode").isEqualTo("app_dev");
		client.post().uri("/app-center/api/apps/1/members").contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"userId\":\"8\",\"roleCode\":\"app_dev\"}").exchange()
				.expectStatus().isOk();
		client.put().uri("/app-center/api/apps/1/members/8").contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"roleCode\":\"app_admin\"}").exchange().expectStatus().isOk();
		client.delete().uri("/app-center/api/apps/1/members/8").exchange().expectStatus().isOk();

		verify(facade).appMembers(Integer.valueOf(1));
		verify(facade).addAppMember(eq(Integer.valueOf(1)), any(AppMemberRoleRequest.class));
		verify(facade).changeAppMember(eq(Integer.valueOf(1)), eq("8"), any(AppMemberRoleRequest.class));
		verify(facade).removeAppMember(Integer.valueOf(1), "8");
	}

	@Test
	void apiFacadeTimeoutReturnsServiceUnavailable() {
		TinyEngineApiFacade facade = mock(TinyEngineApiFacade.class);
		when(facade.appDetail(Integer.valueOf(1))).thenAnswer(invocation -> {
			try {
				Thread.sleep(200);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new App();
		});
		TinyEngineReactiveExecutor executor = new TinyEngineReactiveExecutor(
				Schedulers.newSingle("tiny-engine-blocking-test"), Duration.ofMillis(10));
		WebTestClient client = WebTestClient.bindToController(new TinyEngineAppWebFluxController(facade, executor))
				.build();

		client.get().uri("/app-center/api/apps/detail/1")
				.exchange()
				.expectStatus().is5xxServerError()
				.expectBody()
				.jsonPath("$.code").isEqualTo("503")
				.jsonPath("$.message").isEqualTo("TinyEngine service is busy");
	}

	@Test
	void appWriteAndPageCopyCallsRunOnTinyEngineBlockingScheduler() {
		TinyEngineApiFacade facade = mock(TinyEngineApiFacade.class);
		App app = new App();
		app.setId(Integer.valueOf(7));
		when(facade.updateApp(any(Integer.class), any(App.class))).thenAnswer(invocation -> {
			assertThat(Thread.currentThread().getName()).startsWith("tiny-engine-blocking-test");
			return app;
		});
		when(facade.createAppExtension(any(AppExtension.class))).thenAnswer(invocation -> {
			assertThat(Thread.currentThread().getName()).startsWith("tiny-engine-blocking-test");
			return new AppExtension();
		});
		Page page = new Page();
		page.setId(Integer.valueOf(9));
		when(facade.copyPage(any(Page.class))).thenAnswer(invocation -> {
			assertThat(Thread.currentThread().getName()).startsWith("tiny-engine-blocking-test");
			return page;
		});
		TinyEngineReactiveExecutor executor = new TinyEngineReactiveExecutor(
				Schedulers.newSingle("tiny-engine-blocking-test"), Duration.ofSeconds(5));
		WebTestClient client = WebTestClient.bindToController(new TinyEngineAppWebFluxController(facade, executor),
				new TinyEnginePageWebFluxController(facade, executor)).build();

		client.post().uri("/app-center/api/apps/update/7")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"updated\"}")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.data.id").isEqualTo(7);
		client.post().uri("/app-center/api/apps/extension/create")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"app\":7,\"category\":\"utils\"}")
				.exchange()
				.expectStatus().isOk();
		client.post().uri("/app-center/api/pages/copy")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"copy\"}")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.data.id").isEqualTo(9);
	}

	@Test
	void codegenWebFluxKeepsOfficialParametersAndBadSourceTemplateRequestsOutOfExecutor() {
		RecordingExternalCapability capability = new RecordingExternalCapability();
		TinyEngineReactiveExecutor executor = new TinyEngineReactiveExecutor(
				Schedulers.newSingle("tiny-engine-blocking-test"), Duration.ofSeconds(5));
		WebTestClient client = WebTestClient.bindToController(new TinyEngineCodegenWebFluxController(capability, executor))
				.build();

		client.get().uri("/app-center/api/code?platform=1&app=2&type=page&id=3&history=4")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.data.file").isEqualTo("Generated.vue");
		assertThat(capability.generatedOnThread).startsWith("tiny-engine-blocking-test");
		assertThat(capability.generated.getPlatform()).isEqualTo(Integer.valueOf(1));
		assertThat(capability.generated.getApp()).isEqualTo(Integer.valueOf(2));
		assertThat(capability.generated.getType()).isEqualTo("page");
		assertThat(capability.generated.getId()).isEqualTo(Integer.valueOf(3));
		assertThat(capability.generated.getHistory()).isEqualTo(Integer.valueOf(4));

		client.get().uri("/app-center/api/source_tpl")
				.exchange()
				.expectStatus().isBadRequest();
		client.get().uri("/app-center/api/source_tpl?platform=1&id=2")
				.exchange()
				.expectStatus().isBadRequest();
		assertThat(capability.sourceTemplateCalls).isEqualTo(0);
	}

	@Test
	void dynamicWebFluxAdaptsOfficialShapeAndKeepsBigIntegerId() {
		DynamicService dynamicService = mock(DynamicService.class);
		when(dynamicService.query(any(DynamicQuery.class))).thenAnswer(invocation -> {
			assertThat(Thread.currentThread().getName()).startsWith("tiny-engine-blocking-test");
			return new DynamicPageResult(Collections.<Map<String, Object>>emptyList(), 11, 2, 5);
		});
		when(dynamicService.insert(any(DynamicInsert.class))).thenAnswer(invocation -> {
			assertThat(Thread.currentThread().getName()).startsWith("tiny-engine-blocking-test");
			return new DynamicInsertResult(1, new BigInteger("9223372036854775808"));
		});
		TinyEngineReactiveExecutor executor = new TinyEngineReactiveExecutor(
				Schedulers.newSingle("tiny-engine-blocking-test"), Duration.ofSeconds(5));
		WebTestClient client = WebTestClient.bindToController(
				new TinyEngineDynamicModelWebFluxController(dynamicService, executor)).build();

		client.post().uri("/platform-center/api/model-data/queryApi")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"nameEn\":\"user\",\"params\":{\"name\":\"Ada\"},\"currentPage\":2,\"pageSize\":5}")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.data.total").isEqualTo(11)
				.jsonPath("$.data.pageNum").isEqualTo(2)
				.jsonPath("$.data.pages").isEqualTo(3);
		client.post().uri("/platform-center/api/model-data/insertApi")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"nameEn\":\"user\",\"params\":{\"name\":\"Ada\"}}")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.data.insert").isEqualTo(1)
				.jsonPath("$.data.id").isEqualTo("9223372036854775808");
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

		@Bean
		TinyEngineExternalCapability tinyEngineExternalCapability() {
			return mock(TinyEngineExternalCapability.class);
		}

		@Bean
		DynamicService dynamicService() {
			return mock(DynamicService.class);
		}

		@Bean
		TinyEngineUserSystemService tinyEngineUserSystemService() {
			return mock(TinyEngineUserSystemService.class);
		}

		@Bean
		TinyEngineRequestUserScope tinyEngineRequestUserScope() {
			return new TinyEngineRequestUserScope();
		}
	}

	private static final class RecordingExternalCapability implements TinyEngineExternalCapability {
		private com.xiongx9527.fw.plus.tinyengine.model.dto.SchemaCodeParam generated;
		private String generatedOnThread;
		private int sourceTemplateCalls;

		@Override
		public Object generateCode(com.xiongx9527.fw.plus.tinyengine.model.dto.SchemaCodeParam request) {
			generated = request;
			generatedOnThread = Thread.currentThread().getName();
			return Collections.<String, Object>singletonMap("file", "Generated.vue");
		}

		@Override
		public Object sourceTemplate(Map<String, Object> query) {
			sourceTemplateCalls++;
			return Collections.emptyList();
		}

		@Override
		public Object unsupported(String capability) {
			throw new IllegalStateException(capability);
		}
	}

	private static Set<String> implementedWebFluxContractPaths() throws IOException {
		JsonNode contracts = new ObjectMapper().readTree(findOfficialContractFile());
		Set<String> paths = new LinkedHashSet<String>();
		for (JsonNode contract : contracts) {
			if ("implemented".equals(contract.path("status").asText())
					&& !contract.path("currentWebFluxHandler").isNull()) {
				paths.add(normalizeTemplateVariables(contract.path("path").asText()));
			}
		}
		return paths;
	}

	private static String normalizeTemplateVariables(String path) {
		return path == null ? null : path.replaceAll("\\{[^/]+}", "{}");
	}

	private static File findOfficialContractFile() {
		File current = new File("").getAbsoluteFile();
		while (current != null) {
			File contract = new File(current, "docs/tiny-engine/official-api-contracts.json");
			if (contract.isFile()) {
				return contract;
			}
			current = current.getParentFile();
		}
		throw new IllegalStateException("Cannot find docs/tiny-engine/official-api-contracts.json");
	}
}
