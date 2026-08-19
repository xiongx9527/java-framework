package com.xiongx9527.fw.plus.ai.tinyflow.sample;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowExecutionResult;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflow;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/tinyflow", produces = MediaType.APPLICATION_JSON_VALUE)
public class TinyflowSampleController {

    private static final String WORKFLOW_ID = "real-chat";
    private static final String WORKFLOW_PATH = "samples/real-chat";
    private static final String WORKFLOW_RESOURCE = "classpath:workflows/real-chat.json";

    private final TinyflowService tinyflowService;
    private final ResourceLoader resourceLoader;

    public TinyflowSampleController(TinyflowService tinyflowService, ResourceLoader resourceLoader) {
        this.tinyflowService = tinyflowService;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void registerWorkflow() {
        Resource resource = resourceLoader.getResource(WORKFLOW_RESOURCE);
        String workflowJson = readWorkflow(resource);
        TinyflowWorkflow workflow = new TinyflowWorkflow(WORKFLOW_ID, WORKFLOW_PATH,
                "Real OpenAI chat", workflowJson);
        tinyflowService.validate(workflow);
        tinyflowService.register(workflow);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new LinkedHashMap<String, String>();
        response.put("status", "UP");
        response.put("workflowId", WORKFLOW_ID);
        return response;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, path = "/execute")
    public TinyflowExecutionResult execute(@RequestBody ExecuteRequest request) {
        Map<String, Object> variables = request == null || request.getVariables() == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(request.getVariables());
        if (request != null && request.getMessage() != null) {
            variables.put("message", request.getMessage());
        }
        return tinyflowService.execute(WORKFLOW_ID, variables);
    }

    private String readWorkflow(Resource resource) {
        if (resource == null || !resource.exists()) {
            throw new IllegalStateException("Tinyflow workflow resource is missing: " + WORKFLOW_RESOURCE);
        }
        try (InputStream input = resource.getInputStream()) {
            byte[] bytes = readAllBytes(input);
            String json = new String(bytes, StandardCharsets.UTF_8);
            if (json.trim().length() == 0) {
                throw new IllegalStateException("Tinyflow workflow resource is blank: " + WORKFLOW_RESOURCE);
            }
            return json;
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read Tinyflow workflow resource: " + WORKFLOW_RESOURCE, error);
        }
    }

    private byte[] readAllBytes(InputStream input) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExecuteRequest {
        private String message;
        private Map<String, Object> variables = Collections.emptyMap();

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }

        public void setVariables(Map<String, Object> variables) {
            this.variables = variables;
        }
    }
}
