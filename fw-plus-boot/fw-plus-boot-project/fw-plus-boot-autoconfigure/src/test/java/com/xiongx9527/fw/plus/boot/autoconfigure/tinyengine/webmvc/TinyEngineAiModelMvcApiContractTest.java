package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.DefaultTinyEngineApiFacade;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelImportRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelServiceDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelServiceRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppAiModelSettings;
import com.xiongx9527.fw.plus.tinyengine.service.AppAiModelConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TinyEngineAiModelMvcApiContractTest {

	private TinyEngineApiFacade facade;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		facade = mock(TinyEngineApiFacade.class);
		mvc = MockMvcBuilders.standaloneSetup(new TinyEngineAppMvcController(facade)).build();
	}

	@Test
	void aiModelRoutesDelegateToFacadeAndNeverSerializeApiKey() throws Exception {
		AiModelServiceDto service = service();
		when(facade.usableAiModelServices(Integer.valueOf(2))).thenReturn(Collections.singletonList(service));
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

		mvc.perform(get("/app-center/api/apps/2/ai-model-services"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].hasApiKey").value(true))
				.andExpect(jsonPath("$.data[0].apiKey").doesNotExist());
		mvc.perform(get("/app-center/api/apps/2/ai-model-services/manage"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].apiKey").doesNotExist());
		mvc.perform(get("/app-center/api/apps/2/ai-model-services/settings"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.defaultModel").value("openai::gpt-4o"));
		mvc.perform(put("/app-center/api/apps/2/ai-model-services/settings")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"defaultModel\":\"21::gpt-4o\",\"chatMode\":\"chat\"}"))
				.andExpect(status().isOk());
		mvc.perform(post("/app-center/api/apps/2/ai-model-services").contentType(MediaType.APPLICATION_JSON)
					.content("{\"serviceKey\":\"openai\",\"apiKey\":\"client-supplied\"}"))
				.andExpect(status().isOk());
		mvc.perform(put("/app-center/api/apps/2/ai-model-services/21").contentType(MediaType.APPLICATION_JSON)
					.content("{\"label\":\"OpenAI\"}"))
				.andExpect(status().isOk());
		mvc.perform(delete("/app-center/api/apps/2/ai-model-services/21"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value(true));
		mvc.perform(post("/app-center/api/apps/2/ai-model-services/21/models")
					.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"gpt-4o\"}"))
				.andExpect(status().isOk());
		mvc.perform(put("/app-center/api/apps/2/ai-model-services/21/models/34")
					.contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"GPT-4o\"}"))
				.andExpect(status().isOk());
		mvc.perform(delete("/app-center/api/apps/2/ai-model-services/21/models/34"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value(true));
		mvc.perform(post("/app-center/api/apps/2/ai-model-services/import-local")
					.contentType(MediaType.APPLICATION_JSON).content("{\"settings\":{\"services\":[]}}"))
				.andExpect(status().isOk());

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

	@Test
	void facadeDelegatesEveryAiModelOperationToCoreConfigService() {
		AppAiModelConfigService configService = mock(AppAiModelConfigService.class);
		DefaultTinyEngineApiFacade coreFacade = new DefaultTinyEngineApiFacade(null, null, null, null, null,
				null, null, null, null, null, null, null, null, null, null, null, null, configService);
		AiModelServiceRequest serviceRequest = new AiModelServiceRequest();
		AiModelRequest modelRequest = new AiModelRequest();
		AiModelImportRequest importRequest = new AiModelImportRequest();

		coreFacade.usableAiModelServices(Integer.valueOf(2));
		coreFacade.manageableAiModelServices(Integer.valueOf(2));
		coreFacade.aiModelSettings(Integer.valueOf(2));
		coreFacade.updateAiModelSettings(Integer.valueOf(2), new AppAiModelSettings());
		coreFacade.createAiModelService(Integer.valueOf(2), serviceRequest);
		coreFacade.updateAiModelService(Integer.valueOf(2), Long.valueOf(21), serviceRequest);
		coreFacade.deleteAiModelService(Integer.valueOf(2), Long.valueOf(21));
		coreFacade.addAiModel(Integer.valueOf(2), Long.valueOf(21), modelRequest);
		coreFacade.updateAiModel(Integer.valueOf(2), Long.valueOf(21), Long.valueOf(34), modelRequest);
		coreFacade.deleteAiModel(Integer.valueOf(2), Long.valueOf(21), Long.valueOf(34));
		coreFacade.importAiModelServices(Integer.valueOf(2), importRequest);

		verify(configService).listUsable(Integer.valueOf(2));
		verify(configService).listManageable(Integer.valueOf(2));
		verify(configService).getSettings(Integer.valueOf(2));
		verify(configService).updateSettings(eq(Integer.valueOf(2)), any(AppAiModelSettings.class));
		verify(configService).createService(Integer.valueOf(2), serviceRequest);
		verify(configService).updateService(Integer.valueOf(2), Long.valueOf(21), serviceRequest);
		verify(configService).deleteService(Integer.valueOf(2), Long.valueOf(21));
		verify(configService).addModel(Integer.valueOf(2), Long.valueOf(21), modelRequest);
		verify(configService).updateModel(Integer.valueOf(2), Long.valueOf(21), Long.valueOf(34), modelRequest);
		verify(configService).deleteModel(Integer.valueOf(2), Long.valueOf(21), Long.valueOf(34));
		verify(configService).importLocal(Integer.valueOf(2), importRequest);
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
