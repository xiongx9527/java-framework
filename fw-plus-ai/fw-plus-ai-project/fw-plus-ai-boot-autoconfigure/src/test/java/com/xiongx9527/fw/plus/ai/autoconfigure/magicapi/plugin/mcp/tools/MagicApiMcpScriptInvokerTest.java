package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools;

import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.ApiInfo;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MagicApiMcpScriptInvokerTest {

    private final MagicApiMcpScriptInvoker scriptInvoker = new MagicApiMcpScriptInvoker();

    @Test
    void shouldExposeArgumentsAsVariableForApiScript() {
        ApiInfo info = new ApiInfo();
        info.setName("echo");
        info.setPath("/echo");
        info.setMethod("POST");
        info.setScript("return args.value;");

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("value", "hi");

        assertThat(scriptInvoker.invoke(info, arguments)).isEqualTo("hi");
    }

    @Test
    void shouldFailWhenApiScriptThrows() {
        ApiInfo info = new ApiInfo();
        info.setName("boom");
        info.setPath("/boom");
        info.setMethod("GET");
        info.setScript("throw new IllegalStateException('boom');");

        assertThatThrownBy(() -> scriptInvoker.invoke(info, new HashMap<>()))
                .hasMessageContaining("boom");
    }
}
