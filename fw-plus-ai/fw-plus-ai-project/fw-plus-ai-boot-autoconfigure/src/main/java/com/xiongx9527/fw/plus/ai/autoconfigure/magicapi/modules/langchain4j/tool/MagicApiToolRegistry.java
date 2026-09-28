package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.tool;

import com.alibaba.fastjson2.JSON;
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
 * Magic-API → LangChain4j 工具快照注册表。
 *
 * <p>订阅 Magic-API 的 api/function 元数据（经 {@link MagicResourceService}），按
 * {@link Langchain4jToolProperties} 过滤生成已过滤工具快照：exclude-groups / exclude-paths
 * 最高优先，其次 include-groups（Ant 匹配 group path）、include-paths，api 另有 methods
 * 白名单与 require-opt-in（与 MCP 共用 {@code mcpTool} 选项）。工具名采用 MCP 风格原样名：
 * api 为 {@code "POST /path"}，function 为函数名，同名冲突时 function 优先。</p>
 *
 * <p>快照带缓存，只在收到 Magic-API 资源变更事件（{@link #invalidateSnapshot()}）后重建；
 * 故意不用 TTL——那会让「刚保存的内容在 N 秒内读不到」。</p>
 */
public class MagicApiToolRegistry {

	private static final String FUNCTION_TYPE = MagicApiTool.TYPE_FUNCTION;

	private static final String API_TYPE = MagicApiTool.TYPE_API;

	private static final String DEFAULT_OPT_IN_OPTION = "mcpTool";

	private final MagicResourceService magicResourceService;

	private final Langchain4jToolProperties properties;

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	private volatile List<MagicApiTool> snapshotCache;

	/** 快照重建与失效的互斥锁 */
	private final Object snapshotLock = new Object();

	public MagicApiToolRegistry(MagicResourceService magicResourceService, Langchain4jToolProperties properties) {
		this.magicResourceService = Objects.requireNonNull(magicResourceService, "magicResourceService");
		this.properties = Objects.requireNonNull(properties, "properties");
	}

	public MagicResourceService getMagicResourceService() {
		return magicResourceService;
	}

	/**
	 * @return 当前已过滤工具快照（不可变；缓存有效时直接返回缓存）
	 */
	public List<MagicApiTool> snapshot() {
		List<MagicApiTool> cached = snapshotCache;
		if (cached != null) {
			return cached;
		}
		// 与 invalidateSnapshot 互斥：避免「读到 null → 重建」期间被置空后把旧快照回写，
		// 导致刚保存的资源在下一次事件前不可见。
		synchronized (snapshotLock) {
			if (snapshotCache == null) {
				snapshotCache = buildSnapshot();
			}
			return snapshotCache;
		}
	}

	/**
	 * 资源变更后调用：使快照失效，下次读取重建。
	 */
	public void invalidateSnapshot() {
		synchronized (snapshotLock) {
			snapshotCache = null;
		}
	}

	/**
	 * 本地执行 api 工具时按 id 定位 Magic-API api 资源。
	 *
	 * @return 找不到时返回 {@code null}
	 */
	public ApiInfo api(String apiId) {
		if (apiId == null) {
			return null;
		}
		for (ApiInfo info : magicResourceService.<ApiInfo>files(API_TYPE)) {
			if (apiId.equals(info.getId())) {
				return info;
			}
		}
		return null;
	}

	private List<MagicApiTool> buildSnapshot() {
		List<MagicApiTool> result = new ArrayList<>();
		// 工具名 -> 在 result 中的下标；function 先登记，api 不与已登记 function 抢名。
		Map<String, Integer> indexByName = new LinkedHashMap<>();
		Langchain4jToolProperties.Scope functions = properties.getTools().getFunctions();
		if (functions.isEnabled()) {
			for (MagicEntity entity : magicResourceService.<MagicEntity>files(FUNCTION_TYPE)) {
				if (matchesFunction(functions, entity)) {
					addByName(result, indexByName, toFunctionTool(functions, entity));
				}
			}
		}
		Langchain4jToolProperties.Api api = properties.getTools().getApi();
		if (api.isEnabled()) {
			for (ApiInfo info : magicResourceService.<ApiInfo>files(API_TYPE)) {
				if (matchesApi(api, info)) {
					MagicApiTool tool = toApiTool(api, info);
					if (!indexByName.containsKey(tool.getName())) {
						addByName(result, indexByName, tool);
					}
				}
			}
		}
		return Collections.unmodifiableList(result);
	}

	private void addByName(List<MagicApiTool> result, Map<String, Integer> indexByName, MagicApiTool tool) {
		Integer index = indexByName.get(tool.getName());
		if (index == null) {
			indexByName.put(tool.getName(), result.size());
			result.add(tool);
		} else {
			result.set(index, tool);
		}
	}

	private boolean matchesFunction(Langchain4jToolProperties.Scope scope, MagicEntity entity) {
		if (entity == null) {
			return false;
		}
		return matchesScope(scope, entity.getGroupId(),
				entity instanceof PathMagicEntity ? ((PathMagicEntity) entity).getPath() : null);
	}

	private boolean matchesApi(Langchain4jToolProperties.Api scope, ApiInfo info) {
		if (info == null) {
			return false;
		}
		return matchesScope(scope, info.getGroupId(), info.getPath())
				&& matchesMethod(scope, info)
				&& (!scope.isRequireOptIn() || isOptedIn(scope, info));
	}

	private boolean matchesScope(Langchain4jToolProperties.Scope scope, String groupId, String path) {
		String normalizedPath = normalizePath(path);
		String groupPath = normalizePath(magicResourceService.getGroupPath(groupId));
		if (matchesPathAny(scope.getExcludePaths(), normalizedPath) || matchesGroupAny(scope.getExcludeGroups(), groupPath)) {
			return false;
		}
		return matchesIncludePaths(scope, normalizedPath) && matchesIncludeGroups(scope, groupPath);
	}

	private boolean matchesIncludeGroups(Langchain4jToolProperties.Scope scope, String groupPath) {
		List<String> includeGroups = scope.getIncludeGroups();
		return includeGroups == null || includeGroups.isEmpty() || matchesGroupAny(includeGroups, groupPath);
	}

	private boolean matchesIncludePaths(Langchain4jToolProperties.Scope scope, String path) {
		List<String> includePaths = scope.getIncludePaths();
		return includePaths == null || includePaths.isEmpty() || matchesPathAny(includePaths, path);
	}

	private boolean matchesGroupAny(List<String> patterns, String groupPath) {
		if (patterns == null || patterns.isEmpty() || StringUtils.isBlank(groupPath)) {
			return false;
		}
		for (String pattern : patterns) {
			if (matchesGroupPattern(pattern, groupPath)) {
				return true;
			}
		}
		return false;
	}

	private boolean matchesPathAny(List<String> patterns, String path) {
		if (patterns == null || patterns.isEmpty() || StringUtils.isBlank(path)) {
			return false;
		}
		for (String pattern : patterns) {
			if (matchesPathPattern(pattern, path)) {
				return true;
			}
		}
		return false;
	}

	private boolean matchesMethod(Langchain4jToolProperties.Api scope, ApiInfo info) {
		List<String> methods = scope.getMethods();
		if (methods == null || methods.isEmpty()) {
			return true;
		}
		String method = upper(info.getMethod());
		if (StringUtils.isBlank(method)) {
			// 未声明 HTTP 方法的 api 无法命中白名单，按不曝光处理（避免解引用 null）
			return false;
		}
		for (String configured : methods) {
			if (method.equals(upper(configured))) {
				return true;
			}
		}
		return false;
	}

	private boolean isOptedIn(Langchain4jToolProperties.Api scope, ApiInfo info) {
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
		return StringUtils.isNotBlank(value) && Boolean.parseBoolean(value);
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

	private boolean matchesGroupPattern(String pattern, String groupPath) {
		if (StringUtils.isBlank(pattern) || StringUtils.isBlank(groupPath)) {
			return false;
		}
		String normalized = normalizePath(pattern);
		// "/评估" 这类不带通配符的写法同时覆盖组本身与其子组，减少用户写 "/评估/**" 的负担。
		return pathMatcher.match(normalized, groupPath)
				|| pathMatcher.match(normalized.endsWith("/**") ? normalized : normalized + "/**", groupPath);
	}

	private boolean matchesPathPattern(String pattern, String path) {
		if (StringUtils.isBlank(pattern)) {
			return false;
		}
		String normalized = normalizePath(pattern);
		return pathMatcher.match(normalized, path) || normalized.equals(path);
	}

	private MagicApiTool toFunctionTool(Langchain4jToolProperties.Scope scope, MagicEntity entity) {
		String path = entity instanceof PathMagicEntity ? ((PathMagicEntity) entity).getPath() : null;
		return new MagicApiTool(entity.getId(), entity.getName(), entity.getName(), FUNCTION_TYPE, null, path,
				entity.getGroupId(), magicResourceService.getGroupPath(entity.getGroupId()),
				inputSchema(scope, entity));
	}

	private MagicApiTool toApiTool(Langchain4jToolProperties.Api scope, ApiInfo info) {
		return new MagicApiTool(info.getId(), toolName(info), description(info), API_TYPE, upper(info.getMethod()),
				normalizePath(info.getPath()), info.getGroupId(), magicResourceService.getGroupPath(info.getGroupId()),
				inputSchema(scope, info));
	}

	private String toolName(ApiInfo info) {
		return upper(info.getMethod()) + " " + normalizePath(info.getPath());
	}

	private String description(ApiInfo info) {
		return StringUtils.defaultIfBlank(info.getDescription(), info.getName());
	}

	/**
	 * 解析入参 schema：优先取元数据属性（Map 直接采用，String 按 JSON 解析且 {@code type} 为
	 * {@code object} 才采用），缺失或无效时回退 {@code {"type":"object"}}。
	 */
	private Map<String, Object> inputSchema(Langchain4jToolProperties.Scope scope, MagicEntity entity) {
		Object value = entity.getAttribute(StringUtils.defaultString(scope.getInputSchemaAttribute()));
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

	private String upper(String value) {
		return StringUtils.upperCase(value, Locale.ENGLISH);
	}

	private String normalizePath(String path) {
		if (StringUtils.isBlank(path)) {
			return "/";
		}
		return path.startsWith("/") ? path : "/" + path;
	}
}
