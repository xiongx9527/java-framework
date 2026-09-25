package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service;

import com.ajaxjs.mcp.protocol.prompt.PromptMessage;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptMessage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpResourceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MagicApiMcpCapabilityRegistrarTest {

    private FeatureMgr featureMgr;

    private List<MagicMcpResourceInfo> resources;

    private List<MagicMcpPromptInfo> prompts;

    private MagicApiMcpCapabilityRegistrar registrar;

    @BeforeEach
    void setUp() {
        featureMgr = new FeatureMgr();
        resources = new ArrayList<>();
        prompts = new ArrayList<>();
        registrar = registrar();
    }

    @Test
    void shouldRegisterResourcesAndPromptsIntoStore() {
        resources.add(resource("mcp://readme", "readme", "正文"));
        prompts.add(prompt("summarize", "text"));

        registrar.refresh();

        assertThat(featureMgr.getResourceStore()).containsOnlyKeys("mcp://readme");
        assertThat(featureMgr.getResourceStore()).containsKey("mcp://readme");
        assertThat(featureMgr.getPromptStore()).containsOnlyKeys("summarize");
    }

    @Test
    void shouldReadResourceContentThroughInvoker() throws Exception {
        resources.add(resource("mcp://readme", "readme", "正文"));

        registrar.refresh();

        Object invoker = featureMgr.getResourceStore().get("mcp://readme").getInstance();
        String content = (String) featureMgr.getResourceStore().get("mcp://readme").getMethod()
                .invoke(invoker);
        assertThat(content).isEqualTo("正文");
    }

    @Test
    void shouldRemoveCapabilityFromStoreWhenResourceDisappears() {
        resources.add(resource("mcp://readme", "readme", "正文"));
        prompts.add(prompt("summarize", "text"));
        registrar.refresh();

        resources.clear();
        prompts.clear();
        registrar.refresh();

        assertThat(featureMgr.getResourceStore()).doesNotContainKey("mcp://readme");
        assertThat(featureMgr.getPromptStore()).doesNotContainKey("summarize");
    }

    @Test
    void shouldUpdateStoreItemWhenContentChanges() throws Exception {
        resources.add(resource("mcp://readme", "readme", "正文"));
        registrar.refresh();

        resources.clear();
        resources.add(resource("mcp://readme", "readme", "正文 v2"));
        registrar.refresh();

        Object invoker = featureMgr.getResourceStore().get("mcp://readme").getInstance();
        String content = (String) featureMgr.getResourceStore().get("mcp://readme").getMethod()
                .invoke(invoker);
        assertThat(content).isEqualTo("正文 v2");
    }

    @Test
    void shouldSkipDisabledCapabilities() {
        MagicMcpResourceInfo resource = resource("mcp://readme", "readme", "正文");
        resource.setEnabled(false);
        resources.add(resource);
        MagicMcpPromptInfo prompt = prompt("summarize", "text");
        prompt.setEnabled(false);
        prompts.add(prompt);

        registrar.refresh();

        assertThat(featureMgr.getResourceStore()).isEmpty();
        assertThat(featureMgr.getPromptStore()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldInterpolatePromptArguments() throws Exception {
        prompts.add(prompt("summarize", "text"));

        registrar.refresh();

        Object invoker = featureMgr.getPromptStore().get("summarize").getInstance();
        List<PromptMessage> messages = (List<PromptMessage>) featureMgr.getPromptStore()
                .get("summarize").getMethod().invoke(invoker, "Spring");

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getRole().name()).isEqualTo("USER");
        assertThat(messages.get(0).getContent().toString()).contains("Spring");
    }

    @Test
    void shouldRegisterIndependentStatePerInstance() {
        resources.add(resource("mcp://readme", "readme", "正文"));
        registrar.refresh();
        FeatureMgr another = new FeatureMgr();

        registrar.registerInstance(another);

        assertThat(another.getResourceStore()).containsKey("mcp://readme");
        resources.clear();
        registrar.refresh();
        assertThat(another.getResourceStore()).containsKey("mcp://readme");
        assertThat(featureMgr.getResourceStore()).doesNotContainKey("mcp://readme");
    }

    private MagicMcpResourceInfo resource(String uri, String name, String content) {
        MagicMcpResourceInfo info = new MagicMcpResourceInfo();
        info.setId("resource-" + uri);
        info.setUri(uri);
        info.setName(name);
        info.setMimeType("text/plain");
        info.setContent(content);
        return info;
    }

    private MagicMcpPromptInfo prompt(String name, String... argumentNames) {
        MagicMcpPromptInfo info = new MagicMcpPromptInfo();
        info.setId("prompt-" + name);
        info.setName(name);
        info.setDescription("总结内容");
        List<MagicMcpPromptMessage.Argument> arguments = new ArrayList<>();
        for (String argumentName : Arrays.asList(argumentNames)) {
            MagicMcpPromptMessage.Argument argument = new MagicMcpPromptMessage.Argument();
            argument.setName(argumentName);
            argument.setRequired(true);
            arguments.add(argument);
        }
        info.setArguments(arguments);
        MagicMcpPromptMessage message = new MagicMcpPromptMessage("user", "请总结：${" + argumentNames[0] + "}");
        info.setMessages(Collections.singletonList(message));
        return info;
    }

    private MagicApiMcpCapabilityRegistrar registrar() {
        return new MagicApiMcpCapabilityRegistrar(new MagicApiMcpCapabilityRegistrar.CapabilitySource() {
            @Override
            public List<FeatureMgr> featureMgrs() {
                return Collections.singletonList(featureMgr);
            }

            @Override
            public List<MagicMcpResourceInfo> resources() {
                return resources;
            }

            @Override
            public List<MagicMcpPromptInfo> prompts() {
                return prompts;
            }
        });
    }
}
