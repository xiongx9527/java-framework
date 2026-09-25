package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service;

import com.ajaxjs.mcp.protocol.prompt.PromptArgument;
import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.ajaxjs.mcp.protocol.common.ContentText;
import com.ajaxjs.mcp.protocol.prompt.PromptMessage;
import com.ajaxjs.mcp.protocol.prompt.Role;
import com.ajaxjs.mcp.protocol.resource.ResourceItem;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.ajaxjs.mcp.server.feature.model.ServerStorePrompt;
import com.ajaxjs.mcp.server.feature.model.ServerStoreResource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptMessage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpResourceInfo;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 把 {@code mcp-resource}/{@code mcp-prompt} 资源定义注册进各实例的
 * {@code resourceStore}/{@code promptStore}，使内核 {@code resources/list}、
 * {@code resources/read}、{@code prompts/list}、{@code prompts/get} 可访问。
 *
 * <p>三态同步：新增/修改写入 store，消失的从 store 移除，内容逐字段比较决定是否替换。
 * 每项资源（提示词）各自持有 invoker 实例，内核 {@code resources/read}、{@code prompts/get}
 * 反射调用时才能取到该项自己的内容。</p>
 */
public class MagicApiMcpCapabilityRegistrar implements SmartInitializingSingleton {

	private final CapabilitySource source;

	private final Map<FeatureMgr, Set<String>> registeredResources = new LinkedHashMap<>();

	private final Map<FeatureMgr, Set<String>> registeredPrompts = new LinkedHashMap<>();

	public MagicApiMcpCapabilityRegistrar(CapabilitySource source) {
		this.source = Objects.requireNonNull(source, "source");
	}

	@Override
	public void afterSingletonsInstantiated() {
		refresh();
	}

	/**
	 * 资源/提示词快照变化后调用：三态同步全部实例。
	 */
	public void refresh() {
		for (FeatureMgr featureMgr : source.featureMgrs()) {
			registerInstance(featureMgr);
		}
	}

	/**
	 * 新增实例（{@code mcp-server}）后调用：把当前快照注册到该实例。
	 *
	 * @param featureMgr 新实例的能力管理器
	 */
	public void registerInstance(FeatureMgr featureMgr) {
		syncResources(featureMgr);
		syncPrompts(featureMgr);
	}

	/**
	 * 实例销毁（{@code mcp-server} 删除）后调用：清理该实例的 store 与增量状态。
	 *
	 * @param featureMgr 已销毁实例的能力管理器
	 */
	public void unregisterInstance(FeatureMgr featureMgr) {
		featureMgr.getResourceStore().clear();
		featureMgr.getPromptStore().clear();
		registeredResources.remove(featureMgr);
		registeredPrompts.remove(featureMgr);
	}

	private void syncResources(FeatureMgr featureMgr) {
		Map<String, ServerStoreResource> store = featureMgr.getResourceStore();
		Set<String> registered = registeredResources.get(featureMgr);
		if (registered == null) {
			registered = new LinkedHashSet<>();
			registeredResources.put(featureMgr, registered);
		}

		Map<String, ServerStoreResource> desired = new LinkedHashMap<>();
		for (MagicMcpResourceInfo info : source.resources()) {
			if (!info.isEnabled() || info.getUri() == null || info.getUri().trim().isEmpty()) {
				continue;
			}
			desired.put(info.getUri(), storeResource(info));
		}

		for (String uri : registered) {
			if (!desired.containsKey(uri)) {
				store.remove(uri);
			}
		}
		for (Map.Entry<String, ServerStoreResource> entry : desired.entrySet()) {
			ServerStoreResource existing = store.get(entry.getKey());
			if (existing == null || !sameResource(existing, entry.getValue())) {
				store.put(entry.getKey(), entry.getValue());
			}
		}
		registered.clear();
		registered.addAll(desired.keySet());
	}

	private void syncPrompts(FeatureMgr featureMgr) {
		Map<String, ServerStorePrompt> store = featureMgr.getPromptStore();
		Set<String> registered = registeredPrompts.get(featureMgr);
		if (registered == null) {
			registered = new LinkedHashSet<>();
			registeredPrompts.put(featureMgr, registered);
		}

		Map<String, ServerStorePrompt> desired = new LinkedHashMap<>();
		for (MagicMcpPromptInfo info : source.prompts()) {
			if (!info.isEnabled() || info.getName() == null || info.getName().trim().isEmpty()) {
				continue;
			}
			desired.put(info.getName(), storePrompt(info));
		}

		for (String name : registered) {
			if (!desired.containsKey(name)) {
				store.remove(name);
			}
		}
		for (Map.Entry<String, ServerStorePrompt> entry : desired.entrySet()) {
			ServerStorePrompt existing = store.get(entry.getKey());
			if (existing == null || !samePrompt(existing.getPrompt(), entry.getValue().getPrompt())) {
				store.put(entry.getKey(), entry.getValue());
			}
		}
		registered.clear();
		registered.addAll(desired.keySet());
	}

	private ServerStoreResource storeResource(MagicMcpResourceInfo info) {
		ResourceItem item = new ResourceItem();
		item.setUri(info.getUri());
		item.setName(info.getName() == null ? info.getUri() : info.getName());
		item.setMimeType(info.getMimeType());
		item.setDescription(info.getDescription());

		ServerStoreResource store = new ServerStoreResource();
		store.setResource(item);
		store.setInstance(new MagicResourceInvoker(info.getContent()));
		store.setMethod(MagicResourceInvoker.READ_METHOD);
		return store;
	}

	private ServerStorePrompt storePrompt(MagicMcpPromptInfo info) {
		PromptItem item = new PromptItem();
		item.setName(info.getName());
		item.setDescription(info.getDescription());

		List<PromptArgument> arguments = new ArrayList<>();
		if (info.getArguments() != null) {
			for (MagicMcpPromptMessage.Argument argument : info.getArguments()) {
				PromptArgument promptArgument = new PromptArgument();
				promptArgument.setName(argument.getName());
				promptArgument.setDescription(argument.getDescription());
				promptArgument.setRequired(argument.isRequired());
				arguments.add(promptArgument);
			}
		}
		item.setArguments(arguments);

		MagicPromptInvoker invoker = new MagicPromptInvoker(info);
		ServerStorePrompt store = new ServerStorePrompt();
		store.setPrompt(item);
		store.setInstance(invoker);
		store.setMethod(MagicPromptInvoker.resolveMethod(arguments.size()));
		return store;
	}

	private boolean sameResource(ServerStoreResource left, ServerStoreResource right) {
		if (!Objects.equals(JsonUtils.toJsonString(left.getResource()), JsonUtils.toJsonString(right.getResource()))) {
			return false;
		}
		return Objects.equals(readContent(left), readContent(right));
	}

	/**
	 * 资源内容存在 invoker 而非 {@link ResourceItem} 上，比较时必须一并纳入，
	 * 否则只改 content 时 store 不会替换。
	 */
	private String readContent(ServerStoreResource store) {
		try {
			return String.valueOf(store.getMethod().invoke(store.getInstance()));
		} catch (Exception e) {
			return null;
		}
	}

	private boolean samePrompt(PromptItem left, PromptItem right) {
		return Objects.equals(JsonUtils.toJsonString(left), JsonUtils.toJsonString(right));
	}

	/**
	 * 能力快照数据源：每次 refresh 取运行态最新值，避免注册表与能力注册器之间的构造期环。
	 */
	public interface CapabilitySource {

		List<FeatureMgr> featureMgrs();

		List<MagicMcpResourceInfo> resources();

		List<MagicMcpPromptInfo> prompts();
	}

	/**
	 * 资源内容提供者：内核 {@code resources/read} 反射调用 {@link #read()}。
	 */
	static final class MagicResourceInvoker {

		private static final Method READ_METHOD = resolve();

		private final String content;

		MagicResourceInvoker(String content) {
			this.content = content == null ? "" : content;
		}

		public String read() {
			return content;
		}

		private static Method resolve() {
			try {
				return MagicResourceInvoker.class.getMethod("read");
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException("resource read method not found", e);
			}
		}
	}

	/**
	 * 提示词内容提供者：内核按 {@code PromptItem.arguments} 的个数反射调用对应参数数量的
	 * {@code get(argN)}，返回值携带已完成 {@code ${name}} 插值的消息。
	 */
	static final class MagicPromptInvoker {

		private final MagicMcpPromptInfo info;

		MagicPromptInvoker(MagicMcpPromptInfo info) {
			this.info = info;
		}

		public List<PromptMessage> get() {
			return get(new Object[0]);
		}

		public List<PromptMessage> get(Object arg1) {
			return get(new Object[] { arg1 });
		}

		public List<PromptMessage> get(Object arg1, Object arg2) {
			return get(new Object[] { arg1, arg2 });
		}

		public List<PromptMessage> get(Object arg1, Object arg2, Object arg3) {
			return get(new Object[] { arg1, arg2, arg3 });
		}

		public List<PromptMessage> get(Object arg1, Object arg2, Object arg3, Object arg4) {
			return get(new Object[] { arg1, arg2, arg3, arg4 });
		}

		public List<PromptMessage> get(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
			return get(new Object[] { arg1, arg2, arg3, arg4, arg5 });
		}

		public List<PromptMessage> get(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5,
				Object arg6) {
			return get(new Object[] { arg1, arg2, arg3, arg4, arg5, arg6 });
		}

		public List<PromptMessage> get(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
				Object arg7) {
			return get(new Object[] { arg1, arg2, arg3, arg4, arg5, arg6, arg7 });
		}

		public List<PromptMessage> get(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
				Object arg7, Object arg8) {
			return get(new Object[] { arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8 });
		}

		private List<PromptMessage> get(Object[] values) {
			Map<String, Object> variables = new LinkedHashMap<>();
			if (info.getArguments() != null) {
				for (int i = 0; i < info.getArguments().size() && i < values.length; i++) {
					variables.put(info.getArguments().get(i).getName(), values[i]);
				}
			}

			List<PromptMessage> messages = new ArrayList<>();
			for (MagicMcpPromptMessage message : info.getMessages() == null
					? Collections.<MagicMcpPromptMessage>emptyList() : info.getMessages()) {
				PromptMessage promptMessage = new PromptMessage();
				promptMessage.setRole(resolveRole(message.getRole()));
				promptMessage.setContent(new ContentText(interpolate(message.getContent(), variables)));
				messages.add(promptMessage);
			}
			return messages;
		}

		private Role resolveRole(String role) {
			return "assistant".equalsIgnoreCase(role) ? Role.ASSISTANT : Role.USER;
		}

		private String interpolate(String template, Map<String, Object> variables) {
			if (template == null) {
				return "";
			}
			String result = template;
			for (Map.Entry<String, Object> entry : variables.entrySet()) {
				if (entry.getKey() == null) {
					continue;
				}
				result = result.replace("${" + entry.getKey() + "}",
						entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
			}
			return result;
		}

		static Method resolveMethod(int argumentCount) {
			if (argumentCount <= 0) {
				return method("get");
			}
			Class<?>[] types = new Class[argumentCount];
			java.util.Arrays.fill(types, Object.class);
			return method("get", types);
		}

		private static Method method(String name, Class<?>... types) {
			try {
				return MagicPromptInvoker.class.getMethod(name, types);
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException("prompt method not found: " + name, e);
			}
		}
	}
}
