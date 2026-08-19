package com.xiongx9527.fw.plus.ai.tinyflow.sample;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TinyflowSampleApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "fw-plus.ai.model.enabled=false",
        "fw.plus.ai.tinyflow.enabled=true",
        "spring.autoconfigure.exclude=com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration,com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure.AskDataAutoConfiguration",
        "server.port=0"
})
public class TinyflowSampleApplicationContextTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void startsWithoutOpenAiEnvironmentAndServesHealth() throws Exception {
        mockMvc.perform(get("/api/tinyflow/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.workflowId").value("real-chat"));
    }
}
