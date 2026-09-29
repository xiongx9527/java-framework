package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicToolInvoker;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageJsonCodec;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Magic-API 函数托管的 {@link ChatMemoryStore}：把 get/update/delete 三个动作委托给
 * 三个 magic-api 函数执行，插件只做「消息对象 ↔ JSON 字符串」的序列化。
 *
 * <p>函数契约：</p>
 * <ul>
 * <li>{@code getFunction(memoryId)} → 返回消息 JSON 数组字符串（或直接返回 {@code List<ChatMessage>}）；</li>
 * <li>{@code updateFunction(memoryId, messages)} → {@code messages} 为消息 JSON 数组字符串；</li>
 * <li>{@code deleteFunction(memoryId)} → 删除消息。</li>
 * </ul>
 */
public class MagicApiChatMemoryStore implements ChatMemoryStore {

	private static final String MEMORY_ID = "memoryId";

	private static final String MESSAGES = "messages";

	private final MagicToolInvoker invoker;

	private final ChatMessageJsonCodec codec;

	private final String getFunction;

	private final String updateFunction;

	private final String deleteFunction;

	public MagicApiChatMemoryStore(MagicToolInvoker invoker, ChatMessageJsonCodec codec,
			String getFunction, String updateFunction, String deleteFunction) {
		this.invoker = invoker;
		this.codec = codec;
		this.getFunction = getFunction;
		this.updateFunction = updateFunction;
		this.deleteFunction = deleteFunction;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ChatMessage> getMessages(Object memoryId) {
		Object result = invoker.invokeFunction(getFunction, Collections.singletonMap(MEMORY_ID, memoryId));
		if (result == null) {
			return Collections.emptyList();
		}
		if (result instanceof List) {
			return (List<ChatMessage>) result;
		}
		if (result instanceof String) {
			return codec.messagesFromJson((String) result);
		}
		throw new IllegalStateException("getFunction 返回值类型不支持: " + result.getClass().getName());
	}

	@Override
	public void updateMessages(Object memoryId, List<ChatMessage> messages) {
		Map<String, Object> args = new HashMap<>();
		args.put(MEMORY_ID, memoryId);
		args.put(MESSAGES, codec.messagesToJson(messages));
		invoker.invokeFunction(updateFunction, args);
	}

	@Override
	public void deleteMessages(Object memoryId) {
		invoker.invokeFunction(deleteFunction, Collections.singletonMap(MEMORY_ID, memoryId));
	}
}
