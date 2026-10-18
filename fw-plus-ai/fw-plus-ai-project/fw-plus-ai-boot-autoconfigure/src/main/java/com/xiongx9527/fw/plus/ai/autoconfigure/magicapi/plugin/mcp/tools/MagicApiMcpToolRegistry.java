package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.spi.MagicApiMcpCapabilityProvider;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Magic-API → MCP 工具快照注册表。
 *
 * <p>订阅 Magic-API 的 api/function 元数据（经 {@link MagicResourceService}），按
 * {@link MagicApiMcpProperties} 过滤语义生成已过滤工具快照：exclude-paths 最高优先，
 * 其次 include-groups（Ant 匹配 group path）、include-paths、methods（仅 API）、
 * require-opt-in（opt-in 显式声明）。快照为不可变列表。</p>
 */
public class MagicApiMcpToolRegistry {

	private static final String FUNCTION_TYPE = "function";

	private static final String API_TYPE = "api";

	private static final String DEFAULT_OPT_IN_OPTION = "mcpTool";

	private static final String INPUT_SCHEMA_ATTRIBUTE = "mcpInputSchema";

	private final MagicResourceService magicResourceService;

	/**
	 * 本注册表生效的工具过滤配置：插件顶层配置或某个 server 实例的 {@code tools} 段。
	 */
	private final MagicApiMcpProperties.Tools tools;

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	private volatile List<MagicApiMcpCapabilityProvider> providers =
			Collections.<MagicApiMcpCapabilityProvider>emptyList();

	/**
	 * 工具快照缓存：只在收到 Magic-API 资源变更事件后才失效，避免每次 refresh/每次新增实例
	 * 都全量重扫。故意不用 TTL——那会让「刚保存的内容在 N 秒内读不到」。
	 */
	private volatile List<MagicApiMcpTool> snapshotCache;

	public MagicApiMcpToolRegistry(MagicResourceService magicResourceService, MagicApiMcpProperties properties) {
		this(magicResourceService, properties, properties == null ? null : properties.getTools());
	}

	public MagicApiMcpToolRegistry(MagicResourceService magicResourceService, MagicApiMcpProperties properties,
			MagicApiMcpProperties.Tools tools) {
		this.magicResourceService = Objects.requireNonNull(magicResourceService, "magicResourceService");
		MagicApiMcpProperties.Tools effective = tools != null ? tools
				: properties == null ? null : properties.getTools();
		this.tools = effective == null ? new MagicApiMcpProperties.Tools() : effective;
	}

	public MagicResourceService getMagicResourceService() {
		return magicResourceService;
	}

	/**
	 * 注入自定义能力提供者；其工具与资源定义派生的工具合并进快照（同名时 provider 优先）。
	 *
	 * @param providers 提供者集合
	 */
	public void setCapabilityProviders(List<MagicApiMcpCapabilityProvider> providers) {
		this.providers = providers == null
				? Collections.<MagicApiMcpCapabilityProvider>emptyList() : new ArrayList<>(providers);
	}

	/**
	 * 当前已过滤工具快照：缓存有效直接返回，否则重建。
	 *
	 * @return 工具快照
	 */
	public List<MagicApiMcpTool> snapshot() {
		List<MagicApiMcpTool> cached = snapshotCache;
		if (cached != null) {
			return cached;
		}
		List<MagicApiMcpTool> result = buildSnapshot();
		snapshotCache = result;
		return result;
	}

	/**
	 * Magic-API 资源变更后调用：使快照失效，下次读取重建。
	 */
	public void invalidateSnapshot() {
		snapshotCache = null;
	}

	private List<MagicApiMcpTool> buildSnapshot() {
		List<MagicApiMcpTool> result = new ArrayList<>();
		// 工具名 -> 在 result 中的下标；同名冲突时 provider 优先（替换），与
		// MagicApiMcpCapabilityRegistrar 对 resources/prompts 的 putIfAbsent 语义一致。
		Map<String, Integer> indexByName = new LinkedHashMap<>();
		MagicApiMcpProperties.Scope functions = tools.getFunctions();
		if (isScopeEnabled(functions)) {
			for (MagicEntity entity : magicResourceService.<MagicEntity>files(FUNCTION_TYPE)) {
				if (matchesFunction(functions, entity)) {
					addByName(result, indexByName, toFunctionTool(entity));
				}
			}
		}
		MagicApiMcpProperties.Api api = tools.getApi();
		if (isScopeEnabled(api)) {
			for (ApiInfo info : magicResourceService.<ApiInfo>files(API_TYPE)) {
				if (matchesApi(api, info)) {
					MagicApiMcpTool tool = toApiTool(api, info);
					// 动态能力内部保持旧优先级：函数先到先得，api 不与已登记函数抢名；
					// provider 优先由下面统一覆盖实现。
					if (!indexByName.containsKey(tool.getName())) {
						addByName(result, indexByName, tool);
					}
				}
			}
		}
		for (MagicApiMcpCapabilityProvider provider : providers) {
			for (MagicApiMcpTool tool : provider.mcpTools()) {
				if (tool != null && tool.getName() != null) {
					addByName(result, indexByName, tool);
				}
			}
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * 按工具名登记：首次出现追加到尾部，同名再次出现时后注册者覆盖先注册者
	 * （动态能力先登记，故 provider 同名工具会替换掉被抢占的动态工具）。
	 */
	private void addByName(List<MagicApiMcpTool> result, Map<String, Integer> indexByName, MagicApiMcpTool tool) {
		Integer index = indexByName.get(tool.getName());
		if (index == null) {
			indexByName.put(tool.getName(), result.size());
			result.add(tool);
		} else {
			result.set(index, tool);
		}
	}

	private boolean isScopeEnabled(MagicApiMcpProperties.Scope scope) {
		return scope != null && scope.isEnabled();
	}

	private boolean matchesFunction(MagicApiMcpProperties.Scope scope, MagicEntity entity) {
		if (entity == null) {
			return false;
		}
		String path = entity instanceof PathMagicEntity ? normalizePath(((PathMagicEntity) entity).getPath()) : "";
		if (matchesAny(scope.getExcludePaths(), path)) {
			return false;
		}
		return matchesIncludeGroup(scope, entity.getGroupId()) && matchesIncludePaths(scope, path);
	}

	private boolean matchesApi(MagicApiMcpProperties.Api scope, ApiInfo info) {
		if (info == null) {
			return false;
		}
		String path = normalizePath(info.getPath());
		if (matchesAny(scope.getExcludePaths(), path)) {
			return false;
		}
		return matchesIncludeGroup(scope, info.getGroupId())
				&& matchesIncludePaths(scope, path)
				&& matchesMethod(scope, info)
				&& (!scope.isRequireOptIn() || isOptedIn(scope, info));
	}

	private boolean matchesIncludeGroup(MagicApiMcpProperties.Scope scope, String groupId) {
		List<String> includeGroups = scope.getIncludeGroups();
		if (includeGroups == null || includeGroups.isEmpty()) {
			return true;
		}
		String groupPath = normalizePath(magicResourceService.getGroupPath(groupId));
		for (String pattern : includeGroups) {
			if (matchesPattern(pattern, groupPath)) {
				return true;
			}
		}
		return false;
	}

	private boolean matchesIncludePaths(MagicApiMcpProperties.Scope scope, String path) {
		List<String> includePaths = scope.getIncludePaths();
		if (includePaths == null || includePaths.isEmpty()) {
			return true;
		}
		return matchesAny(includePaths, path);
	}

	private boolean matchesMethod(MagicApiMcpProperties.Api scope, ApiInfo info) {
		List<String> methods = scope.getMethods();
		if (methods == null || methods.isEmpty()) {
			return true;
		}
		String method = StringUtils.upperCase(info.getMethod(), Locale.ENGLISH);
		for (String configured : methods) {
			if (method.equals(StringUtils.upperCase(configured, Locale.ENGLISH))) {
				return true;
			}
		}
		return false;
	}

	private boolean isOptedIn(MagicApiMcpProperties.Api scope, ApiInfo info) {
		String optionName = StringUtils.defaultIfBlank(scope.getOptInOptionName(), DEFAULT_OPT_IN_OPTION);
		Object attribute = info.getAttribute(optionName);
		if (attribute != null) {
			return Boolean.parseBoolean(String.valueOf(attribute));
		}
		Map<String, String> options = safeOptions(info);
		if (options != null && options.containsKey(optionName)) {
			return Boolean.parseBoolean(String.valueOf(options.get(optionName)));
		}
		String value = safeOptionValue(info, optionName);
		if (StringUtils.isNotBlank(value)) {
			return Boolean.parseBoolean(value);
		}
		// JVM 参数兜底（参考 MagicAPIConfigUtils 模式）
		String jvmValue = safeJvmOptionValue(info, optionName);
		return StringUtils.isNotBlank(jvmValue) && Boolean.parseBoolean(jvmValue);
	}

	/**
	 * 从 JVM 参数读取 MCP 工具 opt-in 配置。
	 * key 格式：{@code magic.api.mcp.api.<api-id>.<optionName>}。
	 */
	private String safeJvmOptionValue(ApiInfo info, String optionName) {
		try {
			String id = info.getId();
			if (StringUtils.isBlank(id)) {
				return null;
			}
			return SpringUtils.getAppConfig("magic.api.mcp.api." + id + "." + optionName, null);
		} catch (Exception ignored) {
			return null;
		}
	}

	private Map<String, String> safeOptions(ApiInfo info) {
		try {
			return info.options();
		} catch (Exception ignored) {
			return null;
		}
	}

	private String safeOptionValue(ApiInfo info, String optionName) {
		try {
			return info.getOptionValue(optionName);
		} catch (Exception ignored) {
			return null;
		}
	}

	private boolean matchesAny(List<String> patterns, String path) {
		if (patterns == null || patterns.isEmpty() || StringUtils.isBlank(path)) {
			return false;
		}
		for (String pattern : patterns) {
			if (matchesPattern(pattern, path)) {
				return true;
			}
		}
		return false;
	}

	private boolean matchesPattern(String pattern, String path) {
		if (StringUtils.isBlank(pattern)) {
			return false;
		}
		String normalized = normalizePath(pattern);
		return pathMatcher.match(normalized, path) || normalized.equals(path);
	}

	private String normalizePath(String path) {
		if (StringUtils.isBlank(path)) {
			return "/";
		}
		return path.startsWith("/") ? path : "/" + path;
	}

	private MagicApiMcpTool toFunctionTool(MagicEntity entity) {
		String path = entity instanceof PathMagicEntity ? normalizePath(((PathMagicEntity) entity).getPath()) : null;
		return new MagicApiMcpTool(entity.getId(), entity.getName(), entity.getName(), MagicApiMcpTool.TYPE_FUNCTION,
				null, path, entity.getGroupId(), magicResourceService.getGroupPath(entity.getGroupId()), false,
				inputSchema(entity));
	}

	private MagicApiMcpTool toApiTool(MagicApiMcpProperties.Api scope, ApiInfo info) {
		boolean optIn = isOptedIn(scope, info);
		return new MagicApiMcpTool(info.getId(), toolName(info), StringUtils.defaultIfBlank(info.getDescription(), info.getName()),
				MagicApiMcpTool.TYPE_API, StringUtils.upperCase(info.getMethod(), Locale.ENGLISH), normalizePath(info.getPath()),
				info.getGroupId(), magicResourceService.getGroupPath(info.getGroupId()), optIn, inputSchema(info));
	}

	private String toolName(ApiInfo info) {
		return StringUtils.upperCase(info.getMethod(), Locale.ENGLISH) + " " + normalizePath(info.getPath());
	}

	/**
	 * 解析工具 inputSchema：优先取 {@code mcpInputSchema} 元数据（Map 直接采用，String 按 JSON
	 * 解析且 {@code type} 为 {@code object} 才采用），缺失或无效时回退 {@code {"type":"object"}}。
	 */
	private Map<String, Object> inputSchema(MagicEntity entity) {
		Object value = entity.getAttribute(INPUT_SCHEMA_ATTRIBUTE);
		if (value instanceof Map) {
			return new LinkedHashMap<>((Map<String, Object>) value);
		}
		if (value instanceof String && StringUtils.isNotBlank((String) value)) {
			try {
				Map<String, Object> schema = JSON.parseObject((String) value, Map.class);
				if (schema != null && "object".equals(schema.get("type"))) {
					return schema;
				}
			} catch (Exception ignored) {
				return objectSchema();
			}
		}
		return objectSchema();
	}

	private Map<String, Object> objectSchema() {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		return schema;
	}
}
