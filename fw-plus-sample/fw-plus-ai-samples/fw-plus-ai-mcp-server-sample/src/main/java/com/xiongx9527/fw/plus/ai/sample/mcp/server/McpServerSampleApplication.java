package com.xiongx9527.fw.plus.ai.sample.mcp.server;

import com.ajaxjs.mcp.server.feature.annotation.McpService;
import com.ajaxjs.mcp.server.feature.annotation.Prompt;
import com.ajaxjs.mcp.server.feature.annotation.Resource;
import com.ajaxjs.mcp.server.feature.annotation.Tool;
import com.ajaxjs.mcp.server.feature.annotation.ToolArg;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.xiongx9527.fw.plus.ai.sample.mcp.server")
public class McpServerSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerSampleApplication.class, args);
    }

    @McpService
    public static class EchoToolProvider {
        @Tool(value = "sample-echo", description = "回传客户端输入的文本")
        public String echo(@ToolArg("message") String message) {
            return "sample-echo: " + message;
        }
    }

    @McpService
    public static class GreetingResourceProvider {
        @Resource(uri = "sample://greeting", description = "问候样例资源")
        public String greeting() {
            return "hello from sample resource";
        }
    }

    @McpService
    public static class GreetingPromptProvider {
        @Prompt(value = "sample-greeting", description = "生成问候提示词")
        public String greetingPrompt(@ToolArg("name") String name) {
            return "你好，" + name;
        }
    }

    @McpService
    public static class SampleToolBean {
        @Tool(value = "sample-bean-echo", description = "样例注解工具")
        public String beanEcho(@ToolArg(value = "message", required = true) String message) {
            return "sample-bean-echo: " + message;
        }
    }
}
