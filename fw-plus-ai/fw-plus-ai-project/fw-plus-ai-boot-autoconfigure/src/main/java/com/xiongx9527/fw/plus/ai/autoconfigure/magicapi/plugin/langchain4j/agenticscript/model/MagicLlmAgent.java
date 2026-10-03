package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicChatModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;

import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * agentic LLM 节点：把 {@code type=llm} 的脚本资源包装为编排里的 {@link UntypedAgent} 节点——
 * {@code script} 正文是 user message 模板（{@code {{var}}} 按调用入参插值），invoke 即一次
 * 对话补全（systemMessage + 模板渲染后的 user message），返回模型文本。
 *
 * <p>模型经 {@link MagicDynamicChatModel} 按 {@code modelKey} 实时解析（资源热更新即时生效，
 * 解析失败回落容器 ChatModel bean）。与 {@link MagicScriptAgent} 平级，由模块按 type 分发。</p>
 */
public class MagicLlmAgent implements MagicAgenticNode {

	/** 模板插值：{@code {{var}}}，变量名允许两侧空白 */
	private static final Pattern TEMPLATE_VAR = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*}}");

	private final Langchain4jAgenticScriptInfo info;

	private final String script;

	private final Langchain4jChatModelModule chatModelModule;

	public MagicLlmAgent(Langchain4jAgenticScriptInfo info, Langchain4jChatModelModule chatModelModule) {
		this.info = info;
		this.script = info.getScript();
		this.chatModelModule = chatModelModule;
	}

	public String getPath() {
		return info.getPath();
	}

	public String getName() {
		return info.getName() == null || info.getName().trim().isEmpty() ? info.getPath() : info.getName();
	}

	public String getDescription() {
		return info.getDescription();
	}

	public java.util.List<String> getInputKeys() {
		return info.getInputKeys();
	}

	public String getOutputKey() {
		return info.getOutputKey();
	}

	public String getType() {
		return info.getType();
	}

	@Override
	public AgenticScope getAgenticScope(Object memoryId) {
		return null;
	}

	@Override
	public boolean evictAgenticScope(Object memoryId) {
		return false;
	}

	@Override
	public Object invoke(Map<String, Object> input) {
		if (chatModelModule == null) {
			throw new IllegalStateException(String.format(
					"LLM 节点 %s 需要装配 langchain4j-chat 插件（langchain4jChatModel 资源），"
							+ "当前应用未引入该插件", info.getPath()));
		}
		String userMessage = interpolate(script, input);
		return chat(resolveModel(), userMessage);
	}

	/** 模型解析：经 {@link MagicDynamicChatModel} 按 modelKey 实时解析（热更新安全，失败回落容器 bean）。 */
	protected ChatModel resolveModel() {
		return new MagicDynamicChatModel(chatModelModule, info.getModelKey());
	}

	/** 一次对话补全：systemMessage 可选，返回模型文本。 */
	private Object chat(ChatModel model, String userMessage) {
		String system = info.getSystemMessage();
		ChatResponse response = system == null || system.trim().isEmpty()
				? model.chat(UserMessage.from(userMessage))
				: model.chat(SystemMessage.from(system), UserMessage.from(userMessage));
		return response == null || response.aiMessage() == null ? null : response.aiMessage().text();
	}

	/**
	 * 模板插值：{@code {{var}}} 按入参替换，值转字符串；入参缺失或为 null 时置空串
	 * （与脚本节点声明的输入变量缺参补 null 语义一致）。
	 */
	private String interpolate(String template, Map<String, Object> input) {
		if (template == null || template.isEmpty()) {
			return "";
		}
		if (input == null || input.isEmpty()) {
			return replaceAllWithEmpty(template);
		}
		Matcher matcher = TEMPLATE_VAR.matcher(template);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			String name = matcher.group(1);
			Object value = input.get(name);
			matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	/** 无入参时全部占位符置空串 */
	private String replaceAllWithEmpty(String template) {
		return TEMPLATE_VAR.matcher(template).replaceAll("");
	}

	@Override
	public ResultWithAgenticScope<String> invokeWithAgenticScope(Map<String, Object> input) {
		Object result = invoke(input);
		Map<String, Object> state = new java.util.HashMap<>();
		if (input != null) {
			state.putAll(input);
		}
		if (info.getOutputKey() != null) {
			state.put(info.getOutputKey(), result);
		}
		return new ResultWithAgenticScope<>(new MapAgenticScope(state), result == null ? "" : String.valueOf(result));
	}

	/** 基于 Map 的最小 AgenticScope：LLM 节点无真实共享作用域，按调用入参/输出构造只读视图。 */
	private static final class MapAgenticScope implements AgenticScope {

		private final Map<String, Object> state;

		private MapAgenticScope(Map<String, Object> state) {
			this.state = state == null ? new java.util.HashMap<>() : state;
		}

		@Override
		public Object memoryId() {
			return "agentic-llm:" + System.identityHashCode(this);
		}

		@Override
		public void writeState(String key, Object value) {
			state.put(key, value);
		}

		@Override
		public <T> void writeState(Class<? extends dev.langchain4j.agentic.declarative.TypedKey<T>> keyClass, T value) {
			throw new UnsupportedOperationException("LLM 节点不支持 TypedKey 写入");
		}

		@Override
		public void writeStateIfAbsent(String key, Object value) {
			state.putIfAbsent(key, value);
		}

		@Override
		public <T> void writeStateIfAbsent(Class<? extends dev.langchain4j.agentic.declarative.TypedKey<T>> keyClass, T value) {
			throw new UnsupportedOperationException("LLM 节点不支持 TypedKey 写入");
		}

		@Override
		public void writeStates(Map<String, Object> values) {
			state.putAll(values);
		}

		@Override
		public boolean hasState(String key) {
			return state.containsKey(key);
		}

		@Override
		public boolean hasState(Class<? extends dev.langchain4j.agentic.declarative.TypedKey<?>> keyClass) {
			return false;
		}

		@Override
		public Object readState(String key) {
			return state.get(key);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T readState(String key, T defaultValue) {
			Object value = state.get(key);
			return value == null ? defaultValue : (T) value;
		}

		@Override
		public <T> T readState(Class<? extends dev.langchain4j.agentic.declarative.TypedKey<T>> keyClass) {
			return null;
		}

		@Override
		public Map<String, Object> state() {
			return state;
		}

		@Override
		public String contextAsConversation(String... excludeKeys) {
			return "";
		}

		@Override
		public String contextAsConversation(Object... excludeKeys) {
			return "";
		}

		@Override
		public java.util.List<dev.langchain4j.agentic.scope.AgentInvocation> agentInvocations() {
			return java.util.Collections.emptyList();
		}

		@Override
		public java.util.List<dev.langchain4j.agentic.scope.AgentInvocation> agentInvocations(String agentName) {
			return java.util.Collections.emptyList();
		}

		@Override
		public java.util.List<dev.langchain4j.agentic.scope.AgentInvocation> agentInvocations(Class<?> agentInterface) {
			return java.util.Collections.emptyList();
		}

		@Override
		public boolean completePendingResponse(String pendingResponseId, Object response) {
			return false;
		}

		@Override
		public boolean completePendingResponse(Object response) {
			return false;
		}

		@Override
		public java.util.Set<String> pendingResponseIds() {
			return java.util.Collections.emptySet();
		}

		@Override
		public void writeExecutionContext(String key, Object value) {
			state.put(key, value);
		}

		@Override
		public void writeExecutionContext(Class<?> keyClass, Object value) {
			state.put(keyClass.getName(), value);
		}

		@Override
		public Object executionContext(String key) {
			return state.get(key);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T executionContextAs(String key, Class<T> type) {
			return (T) state.get(key);
		}

		@Override
		public <T> T executionContextAs(Class<T> keyClass) {
			return (T) state.get(keyClass.getName());
		}
	}
}
