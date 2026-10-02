package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

import java.util.HashMap;
import java.util.Map;

/**
 * agentic 脚本节点：把一段 magic-script 资源包装为编排里的 {@link UntypedAgent} 节点——
 * invoke 时按声明的输入变量注入脚本上下文并求值，返回值即节点输出。
 *
 * <p>脚本求值走框架既有 {@link ScriptManager} 机制（与 system-message 插件同模式）。</p>
 */
public class MagicScriptAgent implements UntypedAgent {

	private final Langchain4jAgenticScriptInfo info;

	private final String script;

	public MagicScriptAgent(Langchain4jAgenticScriptInfo info) {
		this.info = info;
		this.script = info.getScript();
	}

	public String getPath() {
		return info.getPath();
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
		MagicScriptContext context = new MagicScriptContext();
		context.setScriptName(info.getName() == null ? info.getPath() : info.getName());
		Map<String, Object> injected = new HashMap<>();
		if (input != null) {
			for (Map.Entry<String, Object> entry : input.entrySet()) {
				context.set(entry.getKey(), entry.getValue());
				injected.put(entry.getKey(), entry.getValue());
			}
		}
		if (info.getInputKeys() != null) {
			for (String key : info.getInputKeys()) {
				if (key != null && !injected.containsKey(key)) {
					context.set(key, null);
				}
			}
		}
		return ScriptManager.executeScript(script, context);
	}

	@Override
	public ResultWithAgenticScope<String> invokeWithAgenticScope(Map<String, Object> input) {
		Object result = invoke(input);
		Map<String, Object> state = new HashMap<>();
		if (input != null) {
			state.putAll(input);
		}
		if (info.getOutputKey() != null) {
			state.put(info.getOutputKey(), result);
		}
		return new ResultWithAgenticScope<>(new MapAgenticScope(state), result == null ? "" : String.valueOf(result));
	}

	/** 基于 Map 的最小 AgenticScope：脚本节点无真实共享作用域，按调用入参/输出构造只读视图。 */
	private static final class MapAgenticScope implements AgenticScope {

		private final Map<String, Object> state;

		private MapAgenticScope(Map<String, Object> state) {
			this.state = state == null ? new HashMap<>() : state;
		}

		@Override
		public Object memoryId() {
			return "agentic-script:" + System.identityHashCode(this);
		}

		@Override
		public void writeState(String key, Object value) {
			state.put(key, value);
		}

		@Override
		public <T> void writeState(Class<? extends dev.langchain4j.agentic.declarative.TypedKey<T>> keyClass, T value) {
			throw new UnsupportedOperationException("脚本节点不支持 TypedKey 写入");
		}

		@Override
		public void writeStateIfAbsent(String key, Object value) {
			state.putIfAbsent(key, value);
		}

		@Override
		public <T> void writeStateIfAbsent(Class<? extends dev.langchain4j.agentic.declarative.TypedKey<T>> keyClass, T value) {
			throw new UnsupportedOperationException("脚本节点不支持 TypedKey 写入");
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
