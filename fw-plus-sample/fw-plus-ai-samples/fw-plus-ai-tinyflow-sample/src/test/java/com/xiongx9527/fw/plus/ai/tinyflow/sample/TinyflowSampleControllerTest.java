package com.xiongx9527.fw.plus.ai.tinyflow.sample;

import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowExecutionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.junit.jupiter.api.extension.ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
@WebMvcTest(TinyflowSampleController.class)
@ContextConfiguration(classes = TinyflowSampleController.class)
@org.springframework.test.context.TestPropertySource(properties = "sample.api.token=test-token")
public class TinyflowSampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @MockBean
    private TinyflowService tinyflowService;

    @MockBean
    private TinyflowModelProvider tinyflowModelProvider;

    @org.junit.jupiter.api.BeforeEach
    public void modelIsAvailableForSuccessfulExecutionTests() {
        when(tinyflowModelProvider.listChatModels()).thenReturn(
                Collections.singletonList(new TinyflowModelDescriptor("chat-default", "chat-default", "CHAT", "openai")));
    }

    @Test
    public void executeRequiresConfiguredSampleToken() throws Exception {
        String token = environment.getProperty("sample.api.token");
        org.junit.jupiter.api.Assertions.assertNotNull(token);
        org.junit.jupiter.api.Assertions.assertFalse(token.trim().isEmpty());

        mockMvc.perform(post("/api/tinyflow/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("TINYFLOW_UNAUTHORIZED"));
    }

    @Test
    public void healthReturnsWorkflowStatusWithoutExecutingWorkflow() throws Exception {
        mockMvc.perform(get("/api/tinyflow/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.workflowId").value("real-chat"));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void executeMergesVariablesAndMessageAndReturnsResultFields() throws Exception {
        when(tinyflowService.execute(eq("real-chat"), anyMap()))
                .thenReturn(new TinyflowExecutionResult("instance-1",
                        Collections.<String, Object>singletonMap("answer", "hello back"), false));

        mockMvc.perform(post("/api/tinyflow/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Sample-Api-Token", "test-token")
                        .content("{\"message\":\"hello\",\"variables\":{\"tenant\":\"acme\",\"message\":\"old\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instanceId").value("instance-1"))
                .andExpect(jsonPath("$.outputs.answer").value("hello back"))
                .andExpect(jsonPath("$.suspended").value(false));

        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(tinyflowService).execute(eq("real-chat"), variables.capture());
        assertEquals("acme", variables.getValue().get("tenant"));
        assertEquals("hello", variables.getValue().get("message"));
    }

    @Test
    public void executeWithoutModelConfigurationReturnsStableServiceUnavailableResponse() throws Exception {
        when(tinyflowModelProvider.listChatModels()).thenReturn(Collections.<TinyflowModelDescriptor>emptyList());

        mockMvc.perform(post("/api/tinyflow/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Sample-Api-Token", "test-token")
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("TINYFLOW_MODEL_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Tinyflow model is not configured"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void ordinaryExceptionContainingModelAndUrlTextReturnsSafeBadGatewayResponse() throws Exception {
        when(tinyflowService.execute(eq("real-chat"), anyMap()))
                .thenThrow(new RuntimeException("model https://secret.example/v1/api-key=hidden"));

        mockMvc.perform(post("/api/tinyflow/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Sample-Api-Token", "test-token")
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("TINYFLOW_EXECUTION_FAILED"))
                .andExpect(jsonPath("$.message").value("Tinyflow execution failed"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
