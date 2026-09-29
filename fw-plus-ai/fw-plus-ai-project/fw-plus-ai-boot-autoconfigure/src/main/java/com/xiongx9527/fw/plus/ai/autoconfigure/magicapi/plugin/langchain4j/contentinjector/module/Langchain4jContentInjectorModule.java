package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.MagicApiContentInjector;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.model.Langchain4jContentInjectorInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.registry.Langchain4jContentInjectorMagicDynamicRegistry;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.script.MagicScriptContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 内容注入器脚本模块：把 {@code langchain4j-content-injector} 资源暴露为 {@link ContentInjector}，
 * 供 {@code DefaultRetrievalAugmentor.builder().contentInjector(...)} 装配。
 *
 * <p>用法：{@code langchain4jContentInjector.get('/injector/assistant')} 返回 {@link ContentInjector}；
 * 也可用 {@code langchain4jContentInjector.inject('/injector/assistant', contents, chatMessage)}
 * 直接执行一次注入逻辑（便于脚本内联或排查）。</p>
 */
@MagicModule("langchain4jContentInjector")
public class Langchain4jContentInjectorModule {

	private final Langchain4jContentInjectorMagicDynamicRegistry registry;

	private final MagicResourceService magicResourceService;

	public Langchain4jContentInjectorModule(Langchain4jContentInjectorMagicDynamicRegistry registry,
			MagicResourceService magicResourceService) {
		this.registry = registry;
		this.magicResourceService = magicResourceService;
	}

	/** 按资源路径取内容注入器；路径为空时抛中文异常 */
	public ContentInjector get(String path) {
		return new MagicApiContentInjector(this, normalize(path));
	}

	/**
	 * 执行一次注入：跑路径对应的脚本，并把返回值规范成 {@link ChatMessage}。
	 *
	 * @param contents    检索结果（可为 null）
	 * @param chatMessage 原始消息；脚本返回 null 时原样返回
	 */
	public ChatMessage inject(String path, List<Content> contents, ChatMessage chatMessage) {
		Langchain4jContentInjectorInfo info = find(path);
		Object result = execute(info, contents, chatMessage, Collections.<String, Object>emptyMap(), null);
		return toChatMessage(result, chatMessage);
	}

	/**
	 * 执行资源脚本：调用方传入（调试）上下文时复用，否则新建；两份入参一并注入脚本上下文。
	 * 供注入器执行与控制台「执行测试」共用。
	 *
	 * @param params   测试参数（控制台解析的结果，可为空）
	 * @param provided 调用方提供的脚本上下文（调试用），可为 null
	 */
	public Object execute(Langchain4jContentInjectorInfo info, List<Content> contents, ChatMessage chatMessage,
			Map<String, Object> params, MagicScriptContext provided) {
		MagicScriptContext context = provided == null ? new MagicScriptContext() : provided;
		context.setScriptName(info.getPath());
		context.set("contents", contents == null ? Collections.emptyList() : contents);
		context.set("texts", textsOf(contents));
		context.set("chatMessage", chatMessage);
		context.set("query", queryOf(chatMessage));
		if (params != null) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				context.set(entry.getKey(), entry.getValue());
			}
		}
		return ScriptManager.executeScript(info.getScript(), context);
	}

	/** 按路径取资源：先查动态注册表，再回落到资源服务（控制台刚保存、注册表尚未刷新时） */
	public Langchain4jContentInjectorInfo find(String path) {
		String normalized = normalize(path);
		Langchain4jContentInjectorInfo info = registry.getMapping(normalized);
		if (info == null) {
			info = magicResourceService.file(normalized);
		}
		if (info == null) {
			throw new IllegalArgumentException("未找到内容注入器资源: " + normalized);
		}
		return info;
	}

	/** 脚本返回值规范化：ChatMessage 原样、其它转文本包成 UserMessage、null 表示不注入 */
	static ChatMessage toChatMessage(Object result, ChatMessage fallback) {
		if (result == null) {
			return fallback;
		}
		if (result instanceof ChatMessage) {
			return (ChatMessage) result;
		}
		return UserMessage.from(String.valueOf(result));
	}

	private static String normalize(String path) {
		if (path == null || path.trim().isEmpty()) {
			throw new IllegalArgumentException("内容注入器路径不能为空");
		}
		String trimmed = path.trim();
		return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
	}

	private static List<String> textsOf(List<Content> contents) {
		List<String> texts = new ArrayList<>();
		if (contents != null) {
			for (Content content : contents) {
				texts.add(content == null || content.textSegment() == null ? "" : content.textSegment().text());
			}
		}
		return texts;
	}

	/** 原始用户消息文本；非 UserMessage（或没有单条文本）时为 null */
	private static String queryOf(ChatMessage chatMessage) {
		if (!(chatMessage instanceof UserMessage)) {
			return null;
		}
		try {
			return ((UserMessage) chatMessage).singleText();
		} catch (RuntimeException e) {
			return null;
		}
	}
}
