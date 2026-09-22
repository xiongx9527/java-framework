package com.xiongx9527.fw.plus.ai.sample.mcp.server;

import com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleMcpConfiguration {

    @Bean
    public McpToolProvider sampleToolProvider() {
        return new McpServerSampleApplication.EchoToolProvider();
    }

    @Bean
    public McpResourceProvider sampleResourceProvider() {
        return new McpServerSampleApplication.GreetingResourceProvider();
    }

    @Bean
    public McpPromptProvider samplePromptProvider() {
        return new McpServerSampleApplication.GreetingPromptProvider();
    }

    @Bean
    public McpCompletionProvider sampleCompletionProvider() {
        return new McpServerSampleApplication.SampleCompletionProvider();
    }

    @Bean
    public McpServerSampleApplication.SampleToolBean sampleToolBean() {
        return new McpServerSampleApplication.SampleToolBean();
    }
}
