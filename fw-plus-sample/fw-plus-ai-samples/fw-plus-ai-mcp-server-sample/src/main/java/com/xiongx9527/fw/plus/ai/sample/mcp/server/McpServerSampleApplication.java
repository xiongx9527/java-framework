package com.xiongx9527.fw.plus.ai.sample.mcp.server;

import com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.xiongx9527.fw.plus.ai.sample.mcp.server")
public class McpServerSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerSampleApplication.class, args);
    }

    static class EchoToolProvider implements McpToolProvider {
        @Override
        public java.util.List<com.xiongx9527.fw.plus.ai.mcp.spi.McpTool> listTools(
                com.xiongx9527.fw.plus.ai.mcp.spi.McpProviderContext context,
                com.xiongx9527.fw.plus.ai.mcp.spi.McpPageRequest pageRequest) {
            return java.util.Collections.singletonList(
                    com.xiongx9527.fw.plus.ai.mcp.spi.McpTool.builder()
                            .name("sample-echo")
                            .description("样例工具：回传客户端输入的文本")
                            .inputSchema(java.util.Collections.singletonMap("type", "object"))
                            .build());
        }

        @Override
        public com.xiongx9527.fw.plus.ai.mcp.spi.McpToolResult callTool(
                com.xiongx9527.fw.plus.ai.mcp.spi.McpProviderContext context,
                com.xiongx9527.fw.plus.ai.mcp.spi.McpToolCallRequest request) {
            Object message = request.getArguments() == null ? null : request.getArguments().get("message");
            return com.xiongx9527.fw.plus.ai.mcp.spi.McpToolResult.success("sample-echo: " + message);
        }
    }

    static class GreetingResourceProvider implements McpResourceProvider {
        @Override
        public java.util.List<com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceInfo> listResources(
                com.xiongx9527.fw.plus.ai.mcp.spi.McpProviderContext context) {
            return java.util.Collections.singletonList(
                    new com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceInfo("sample://greeting", "问候样例资源"));
        }

        @Override
        public java.util.List<com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceTemplate> listTemplates(
                com.xiongx9527.fw.plus.ai.mcp.spi.McpProviderContext context) {
            return java.util.Collections.emptyList();
        }

        @Override
        public com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceContent readResource(
                com.xiongx9527.fw.plus.ai.mcp.spi.McpProviderContext context,
                com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceReadRequest request) {
            return com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceContent.text(request.getUri(), "hello from sample resource");
        }
    }

    static class GreetingPromptProvider implements McpPromptProvider {
        @Override
        public java.util.List<com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptInfo> listPrompts(
                com.xiongx9527.fw.plus.ai.mcp.spi.McpProviderContext context) {
            com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptInfo prompt = new com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptInfo(
                    "sample-greeting", "问候样例提示词");
            prompt.setArguments(java.util.Collections.singletonList(
                    new com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptArgument("name", "被问候者", true)));
            return java.util.Collections.singletonList(prompt);
        }

        @Override
        public com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptResult getPrompt(
                com.xiongx9527.fw.plus.ai.mcp.spi.McpProviderContext context,
                com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptRequest request) {
            String name = request.getArguments() == null ? null : (String) request.getArguments().get("name");
            return com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptResult.of(
                    "问候样例提示词",
                    java.util.Collections.singletonList(
                            com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptResult.McpPromptMessage.user(
                                    com.xiongx9527.fw.plus.ai.mcp.spi.McpContent.text("hello " + name))));
        }
    }

    static class SampleCompletionProvider implements McpCompletionProvider {
        @Override
        public com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionResult complete(
                com.xiongx9527.fw.plus.ai.mcp.spi.McpProviderContext context,
                com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionRequest request) {
            return com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionResult.of(
                    java.util.Collections.<String>emptyList());
        }

        @Override
        public java.util.List<com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionRef> supportedRefs() {
            return java.util.Collections.emptyList();
        }
    }

    static class SampleToolBean {
        @com.xiongx9527.fw.plus.ai.mcp.annotation.McpTool(name = "sample-bean-echo", description = "样例注解工具")
        public String beanEcho(@com.xiongx9527.fw.plus.ai.mcp.annotation.McpToolParam(name = "message", required = true) String message) {
            return "sample-bean-echo: " + message;
        }
    }
}
