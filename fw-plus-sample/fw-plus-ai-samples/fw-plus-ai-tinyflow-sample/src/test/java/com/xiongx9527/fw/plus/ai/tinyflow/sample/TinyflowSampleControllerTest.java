package com.xiongx9527.fw.plus.ai.tinyflow.sample;

import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowExecutionResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.junit.jupiter.api.extension.ExtendWith(SpringExtension.class)
@WebMvcTest(TinyflowSampleController.class)
@ContextConfiguration(classes = TinyflowSampleController.class)
public class TinyflowSampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TinyflowService tinyflowService;

    @Test
    public void healthReturnsWorkflowStatusWithoutExecutingWorkflow() throws Exception {
        mockMvc.perform(get("/api/tinyflow/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.workflowId").value("real-chat"));

    }

    @Test
    public void executeMergesVariablesAndMessageAndReturnsResultFields() throws Exception {
        when(tinyflowService.execute(eq("real-chat"), anyMap()))
                .thenReturn(new TinyflowExecutionResult("instance-1",
                        Collections.<String, Object>singletonMap("answer", "hello back"), false));

        mockMvc.perform(post("/api/tinyflow/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\",\"variables\":{\"tenant\":\"acme\",\"message\":\"old\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instanceId").value("instance-1"))
                .andExpect(jsonPath("$.outputs.answer").value("hello back"))
                .andExpect(jsonPath("$.suspended").value(false));

        ArgumentCaptor<Map> variables = ArgumentCaptor.forClass(Map.class);
        verify(tinyflowService).execute(eq("real-chat"), variables.capture());
        assertEquals("acme", variables.getValue().get("tenant"));
        assertEquals("hello", variables.getValue().get("message"));
    }

    @Test
    public void executeWithoutModelConfigurationReturnsStableServiceUnavailableResponse() throws Exception {
        when(tinyflowService.execute(eq("real-chat"), anyMap()))
                .thenThrow(new IllegalStateException("Tinyflow model is not configured"));

        mockMvc.perform(post("/api/tinyflow/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("TINYFLOW_MODEL_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Tinyflow model is not configured"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
