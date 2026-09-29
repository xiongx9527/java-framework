package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.model.Langchain4jContentInjectorInfo;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.content.Content;
import org.junit.jupiter.api.Test;
import org.ssssssss.script.MagicScriptContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 内容注入器：脚本返回值规范化（String → UserMessage、ChatMessage 原样、null 不注入）
 * 与脚本上下文注入（contents / texts / chatMessage / query + 测试参数）。
 */
class MagicApiContentInjectorTest {

    private final Langchain4jContentInjectorModule module =
            new Langchain4jContentInjectorModule(null, null);

    @Test
    void shouldWrapScriptTextIntoUserMessage() {
        UserMessage original = UserMessage.from("杭州天气怎么样");

        ChatMessage injected = Langchain4jContentInjectorModule.toChatMessage("注入后的文本", original);

        assertThat(injected).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) injected).singleText()).isEqualTo("注入后的文本");
    }

    @Test
    void shouldKeepChatMessageReturnedByScript() {
        ChatMessage returned = AiMessage.from("脚本直接返回的消息");

        assertThat(Langchain4jContentInjectorModule.toChatMessage(returned, UserMessage.from("原始")))
                .isSameAs(returned);
    }

    @Test
    void shouldKeepOriginalMessageWhenScriptReturnsNull() {
        UserMessage original = UserMessage.from("原始消息");

        assertThat(Langchain4jContentInjectorModule.toChatMessage(null, original)).isSameAs(original);
    }

    @Test
    void shouldInjectContentsAndQueryIntoScriptContext() {
        Langchain4jContentInjectorInfo info =
                info("return query + '|' + texts.size() + '|' + contents.get(0).textSegment().text();");
        ChatMessage message = UserMessage.from("杭州天气怎么样");

        Object result = module.execute(info, contents("西湖很适合傍晚散步", "灵隐寺清晨人少"),
                message, Collections.<String, Object>emptyMap(), null);

        assertThat(result).isEqualTo("杭州天气怎么样|2|西湖很适合傍晚散步");
    }

    @Test
    void shouldInjectExtraParamsIntoProvidedContext() {
        Langchain4jContentInjectorInfo info = info("return prefix + query;");
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("prefix", "【知识库】");

        Object result = module.execute(info, null, UserMessage.from("问题"),
                params, new MagicScriptContext());

        assertThat(result).isEqualTo("【知识库】问题");
    }

    private static Langchain4jContentInjectorInfo info(String script) {
        Langchain4jContentInjectorInfo info = new Langchain4jContentInjectorInfo();
        info.setPath("/injector/assistant");
        info.setScript(script);
        return info;
    }

    private static java.util.List<Content> contents(String... texts) {
        java.util.List<Content> contents = new java.util.ArrayList<>();
        for (String text : texts) {
            contents.add(Content.from(text));
        }
        return contents;
    }
}
