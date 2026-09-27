package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service;

import com.ajaxjs.mcp.protocol.prompt.PromptMessage;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptMessage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpResourceInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.spi.MagicApiMcpCapabilityProvider;
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

    @Test
    void shouldReplacePromptWhenOnlyMessageContentChanges() throws Exception {
        prompts.add(prompt("summarize", "text"));
        registrar.refresh();
        Object before = featureMgr.getPromptStore().get("summarize").getInstance();

        prompts.clear();
        MagicMcpPromptInfo changed = prompt("summarize", "text");
        changed.setMessages(Collections.singletonList(new MagicMcpPromptMessage("user", "请总结（v2）：${text}")));
        prompts.add(changed);
        registrar.refresh();

        Object after = featureMgr.getPromptStore().get("summarize").getInstance();
        assertThat(after).isNotSameAs(before);
        assertThat(promptContent(featureMgr)).contains("v2");
    }

    @Test
    void shouldKeepPromptWhenMessageContentUnchanged() {
        prompts.add(prompt("summarize", "text"));
        registrar.refresh();
        Object before = featureMgr.getPromptStore().get("summarize").getInstance();

        prompts.clear();
        prompts.add(prompt("summarize", "text"));
        registrar.refresh();

        assertThat(featureMgr.getPromptStore().get("summarize").getInstance()).isSameAs(before);
    }

    @Test
    void shouldClearTemplateAndCompletionStateWhenInstanceUnregisters() throws Exception {
        MagicMcpResourceInfo info = resource("mcp://readme", "readme", "正文");
        info.setTemplate("mcp://docs/{path}");
        info.setCompletions(Collections.singletonMap("path", Arrays.asList("a", "b")));
        resources.add(info);

        registrar.refresh();
        assertThat(featureMgr.getResourceTemplateStore()).containsKey("mcp://docs/{path}");
        assertThat(featureMgr.getCompletionStore()).containsKey("resource:mcp://docs/{path}:path");

        registrar.unregisterInstance(featureMgr);
        assertThat(featureMgr.getResourceTemplateStore()).isEmpty();
        assertThat(featureMgr.getCompletionStore()).isEmpty();
        assertThat(incrementalStateKeys(registrar)).isEmpty();

        // 重新注册同一实例时增量状态重建，不残留上一个生命周期的登记项
        registrar.registerInstance(featureMgr);
        assertThat(featureMgr.getResourceTemplateStore()).containsKey("mcp://docs/{path}");
        assertThat(featureMgr.getCompletionStore()).containsKey("resource:mcp://docs/{path}:path");
    }

    private String promptContent(FeatureMgr mgr) throws Exception {
        Object invoker = mgr.getPromptStore().get("summarize").getInstance();
        @SuppressWarnings("unchecked")
        List<PromptMessage> messages = (List<PromptMessage>) mgr.getPromptStore().get("summarize")
                .getMethod().invoke(invoker, "text");
        return String.valueOf(messages.get(0).getContent());
    }

    /**
     * registrar 四张增量表的 key 集合；用于断言实例注销后增量状态被清空。
     */
    private java.util.Set<FeatureMgr> incrementalStateKeys(MagicApiMcpCapabilityRegistrar target)
            throws Exception {
        java.util.Set<FeatureMgr> keys = new java.util.LinkedHashSet<>();
        for (String field : new String[] { "registeredResources", "registeredPrompts", "registeredTemplates",
                "registeredCompletions" }) {
            java.lang.reflect.Field declared = MagicApiMcpCapabilityRegistrar.class.getDeclaredField(field);
            declared.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<FeatureMgr, ?> table = (java.util.Map<FeatureMgr, ?>) declared.get(target);
            keys.addAll(table.keySet());
        }
        return keys;
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
        return registrar(Collections.<MagicApiMcpCapabilityProvider>emptyList());
    }

    private MagicApiMcpCapabilityRegistrar registrar(
            final java.util.List<MagicApiMcpCapabilityProvider> capabilityProviders) {
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
        }, capabilityProviders);
    }

    @Test
    void shouldRegisterResourceTemplateAndMatchUri() throws Exception {
        MagicMcpResourceInfo info = resource("mcp://readme", "readme", "正文");
        info.setTemplate("mcp://docs/{path}");
        resources.add(info);

        registrar.refresh();

        assertThat(featureMgr.getResourceTemplateStore()).containsKey("mcp://docs/{path}");
        java.util.regex.Pattern pattern = featureMgr.getResourceTemplateStore()
                .get("mcp://docs/{path}").getUriPattern();
        assertThat(pattern.matcher("mcp://docs/readme").matches()).isTrue();
        assertThat(pattern.matcher("mcp://docs/a/b").matches()).isFalse();
        assertThat(featureMgr.getResourceTemplateStore().get("mcp://docs/{path}").getTemplateVariableNames())
                .containsExactly("path");
    }

    @Test
    void shouldRemoveResourceTemplateWhenTemplateCleared() {
        MagicMcpResourceInfo info = resource("mcp://readme", "readme", "正文");
        info.setTemplate("mcp://docs/{path}");
        resources.add(info);
        registrar.refresh();

        info.setTemplate(null);
        registrar.refresh();

        assertThat(featureMgr.getResourceTemplateStore()).isEmpty();
    }

    @Test
    void shouldRegisterCompletionsForPromptArguments() throws Exception {
        prompts.add(prompt("summarize", "text"));
        prompts.get(0).getCompletions().put("text", Arrays.asList("alpha", "beta"));

        registrar.refresh();

        assertThat(featureMgr.getCompletionStore()).containsKey("prompt:summarize:text");
        Object invoker = featureMgr.getCompletionStore().get("prompt:summarize:text").getInstance();
        Object result = featureMgr.getCompletionStore().get("prompt:summarize:text").getMethod()
                .invoke(invoker, "al");
        assertThat(result.toString()).contains("alpha").doesNotContain("beta");
    }

    @Test
    void shouldRegisterCompletionsForResourceTemplateVariables() {
        MagicMcpResourceInfo info = resource("mcp://readme", "readme", "正文");
        info.setTemplate("mcp://docs/{path}");
        info.getCompletions().put("path", Arrays.asList("readme", "guide"));
        resources.add(info);

        registrar.refresh();

        // 模板名与 uriTemplate 两种 ref 形态都应命中
        assertThat(featureMgr.getCompletionStore()).containsKeys("resource:readme:path",
                "resource:mcp://docs/{path}:path");
    }

    @Test
    void shouldCompileTemplatePatternForMultipleVariables() {
        MagicMcpResourceInfo info = resource("mcp://readme", "readme", "正文");
        info.setTemplate("mcp://{group}/{name}");
        resources.add(info);

        registrar.refresh();

        java.util.regex.Pattern pattern = featureMgr.getResourceTemplateStore()
                .get("mcp://{group}/{name}").getUriPattern();
        assertThat(pattern.matcher("mcp://docs/readme").matches()).isTrue();
    }

    @Test
    void shouldMergeProviderCapabilitiesWithDynamicOnes() {
        resources.add(resource("mcp://from-storage", "storage", "正文"));
        prompts.add(prompt("fromStorage", "text"));

        MagicApiMcpCapabilityProvider provider = new MagicApiMcpCapabilityProvider() {
            @Override
            public List<MagicMcpResourceInfo> mcpResources() {
                MagicMcpResourceInfo info = new MagicMcpResourceInfo();
                info.setUri("mcp://from-provider");
                info.setName("provider");
                info.setContent("来自 provider");
                return Collections.singletonList(info);
            }

            @Override
            public List<MagicMcpPromptInfo> mcpPrompts() {
                MagicMcpPromptInfo info = new MagicMcpPromptInfo();
                info.setId("prompt-provider");
                info.setName("fromProvider");
                return Collections.singletonList(info);
            }

            @Override
            public List<MagicApiMcpCapabilityProvider.CompletionEntry> mcpCompletions() {
                return Collections.singletonList(
                        new MagicApiMcpCapabilityProvider.CompletionEntry("prompt", "fromProvider", "text",
                                Arrays.asList("x", "y")));
            }
        };

        MagicApiMcpCapabilityRegistrar withProvider = registrar(Collections.singletonList(provider));
        withProvider.refresh();

        assertThat(featureMgr.getResourceStore()).containsKeys("mcp://from-storage", "mcp://from-provider");
        assertThat(featureMgr.getPromptStore()).containsKeys("fromStorage", "fromProvider");
        assertThat(featureMgr.getCompletionStore()).containsKey("prompt:fromProvider:text");
    }

    @Test
    void shouldLetProviderWinOnSameKey() throws Exception {
        resources.add(resource("mcp://shared", "storage", "storage 版"));
        MagicApiMcpCapabilityProvider provider = new MagicApiMcpCapabilityProvider() {
            @Override
            public List<MagicMcpResourceInfo> mcpResources() {
                return Collections.singletonList(resource("mcp://shared", "provider", "provider 版"));
            }
        };

        registrar(Collections.singletonList(provider)).refresh();

        Object invoker = featureMgr.getResourceStore().get("mcp://shared").getInstance();
        String content = (String) featureMgr.getResourceStore().get("mcp://shared").getMethod()
                .invoke(invoker);
        assertThat(content).isEqualTo("provider 版");
    }
}
