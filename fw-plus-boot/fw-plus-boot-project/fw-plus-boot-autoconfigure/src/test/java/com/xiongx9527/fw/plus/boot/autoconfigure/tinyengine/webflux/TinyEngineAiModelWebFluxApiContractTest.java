package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelImportRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelServiceDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelServiceRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppAiModelSettings;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TinyEngineAiModelWebFluxApiContractTest {

	@Test
	void aiModelRoutesDelegateToFacadeOnBlockingSchedulerAndNeverSerializeApiKey() {
		TinyEngineApiFacade facade = mock(TinyEngineApiFacade.class);
		AiModelServiceDto service = service();
		when(facade.usableAiModelServices(Integer.valueOf(2))).thenAnswer(invocation -> {
			assertThat(Thread.currentThread().getName()).startsWith("tiny-engine-ai-model-test");
			return Collections.singletonList(service);
		});
		when(facade.manageableAiModelServices(Integer.valueOf(2))).thenReturn(Collections.singletonList(service));
		when(facade.aiModelSettings(Integer.valueOf(2))).thenReturn(settings());
		when(facade.updateAiModelSettings(eq(Integer.valueOf(2)), any(AppAiModelSettings.class)))
				.thenReturn(settings());
		when(facade.createAiModelService(eq(Integer.valueOf(2)), any(AiModelServiceRequest.class))).thenReturn(service);
		when(facade.updateAiModelService(eq(Integer.valueOf(2)), eq(Long.valueOf(21)),
				any(AiModelServiceRequest.class))).thenReturn(service);
		when(facade.addAiModel(eq(Integer.valueOf(2)), eq(Long.valueOf(21)), any(AiModelRequest.class)))
				.thenReturn(service);
		when(facade.updateAiModel(eq(Integer.valueOf(2)), eq(Long.valueOf(21)), eq(Long.valueOf(34)),
				any(AiModelRequest.class))).thenReturn(service);
		when(facade.importAiModelServices(eq(Integer.valueOf(2)), any(AiModelImportRequest.class)))
				.thenReturn(Collections.singletonList(service));
		TinyEngineReactiveExecutor executor = new TinyEngineReactiveExecutor(
				Schedulers.newSingle("tiny-engine-ai-model-test"), Duration.ofSeconds(5));
		WebTestClient client = WebTestClient.bindToController(new TinyEngineAppWebFluxController(facade, executor))
				.build();

		client.get().uri("/app-center/api/apps/2/ai-model-services").exchange().expectStatus().isOk()
				.expectBody().jsonPath("$.data[0].hasApiKey").isEqualTo(true)
				.jsonPath("$.data[0].apiKey").doesNotExist();
		client.get().uri("/app-center/api/apps/2/ai-model-services/manage").exchange().expectStatus().isOk();
		client.get().uri("/app-center/api/apps/2/ai-model-services/settings").exchange().expectStatus().isOk()
				.expectBody().jsonPath("$.data.defaultModel").isEqualTo("openai::gpt-4o");
		client.put().uri("/app-center/api/apps/2/ai-model-services/settings").contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"defaultModel\":\"21::gpt-4o\",\"chatMode\":\"chat\"}")
				.exchange().expectStatus().isOk();
		client.post().uri("/app-center/api/apps/2/ai-model-services").contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"serviceKey\":\"openai\"}").exchange().expectStatus().isOk();
		client.put().uri("/app-center/api/apps/2/ai-model-services/21").contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"label\":\"OpenAI\"}").exchange().expectStatus().isOk();
		client.delete().uri("/app-center/api/apps/2/ai-model-services/21").exchange().expectStatus().isOk();
		client.post().uri("/app-center/api/apps/2/ai-model-services/21/models").contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"gpt-4o\"}").exchange().expectStatus().isOk();
		client.put().uri("/app-center/api/apps/2/ai-model-services/21/models/34").contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"label\":\"GPT-4o\"}").exchange().expectStatus().isOk();
		client.delete().uri("/app-center/api/apps/2/ai-model-services/21/models/34").exchange().expectStatus().isOk();
		client.post().uri("/app-center/api/apps/2/ai-model-services/import-local").contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"settings\":{\"services\":[]}}").exchange().expectStatus().isOk();

		verify(facade).usableAiModelServices(Integer.valueOf(2));
		verify(facade).manageableAiModelServices(Integer.valueOf(2));
		verify(facade).aiModelSettings(Integer.valueOf(2));
		verify(facade).updateAiModelSettings(eq(Integer.valueOf(2)), any(AppAiModelSettings.class));
		verify(facade).createAiModelService(eq(Integer.valueOf(2)), any(AiModelServiceRequest.class));
		verify(facade).updateAiModelService(eq(Integer.valueOf(2)), eq(Long.valueOf(21)),
				any(AiModelServiceRequest.class));
		verify(facade).deleteAiModelService(Integer.valueOf(2), Long.valueOf(21));
		verify(facade).addAiModel(eq(Integer.valueOf(2)), eq(Long.valueOf(21)), any(AiModelRequest.class));
		verify(facade).updateAiModel(eq(Integer.valueOf(2)), eq(Long.valueOf(21)), eq(Long.valueOf(34)),
				any(AiModelRequest.class));
		verify(facade).deleteAiModel(Integer.valueOf(2), Long.valueOf(21), Long.valueOf(34));
		verify(facade).importAiModelServices(eq(Integer.valueOf(2)), any(AiModelImportRequest.class));
	}

	private AiModelServiceDto service() {
		AiModelServiceDto service = new AiModelServiceDto();
		service.setId(Long.valueOf(21));
		service.setServiceKey("openai");
		service.setHasApiKey(true);
		service.setApiKeyMask("********");
		return service;
	}

	private AppAiModelSettings settings() {
		AppAiModelSettings settings = new AppAiModelSettings();
		settings.setDefaultModel("openai::gpt-4o");
		settings.setChatMode("chat");
		return settings;
	}
}
