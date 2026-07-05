package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.tools;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class McpToolScopeMatcher {

	private final MagicAPIMcpProperties properties;

	private final Function<String, String> groupPathResolver;

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	public McpToolScopeMatcher(MagicAPIMcpProperties properties, MagicResourceService magicResourceService) {
		this(properties, magicResourceService::getGroupPath);
	}

	public McpToolScopeMatcher(MagicAPIMcpProperties properties, Function<String, String> groupPathResolver) {
		this.properties = properties == null ? new MagicAPIMcpProperties() : properties;
		this.groupPathResolver = groupPathResolver;
	}

	public boolean matches(ApiInfo info) {
		MagicAPIMcpProperties.Api scope = properties.getTools().getApi();
		if (info == null || !scope.isEnabled()) {
			return false;
		}
		String path = normalizePath(info.getPath());
		if (matchesAny(scope.getExcludePaths(), path)) {
			return false;
		}
		if (!matchesIncludes(scope, info, path)) {
			return false;
		}
		if (!matchesMethod(scope, info)) {
			return false;
		}
		return !scope.isRequireOptIn() || isOptedIn(scope, info);
	}

	public boolean matchesFunction(MagicEntity entity) {
		MagicAPIMcpProperties.Scope scope = properties.getTools().getFunctions();
		if (entity == null || !scope.isEnabled()) {
			return false;
		}
		String path = entity instanceof PathMagicEntity ? normalizePath(((PathMagicEntity) entity).getPath()) : "";
		if (StringUtils.isNotBlank(path) && matchesAny(scope.getExcludePaths(), path)) {
			return false;
		}
		return matchesGroup(scope.getIncludeGroups(), entity.getGroupId())
				&& (scope.getIncludePaths().isEmpty() || StringUtils.isBlank(path) || matchesAny(scope.getIncludePaths(), path));
	}

	private boolean matchesIncludes(MagicAPIMcpProperties.Scope scope, MagicEntity entity, String path) {
		return matchesGroup(scope.getIncludeGroups(), entity.getGroupId())
				&& (scope.getIncludePaths().isEmpty() || matchesAny(scope.getIncludePaths(), path));
	}

	private boolean matchesGroup(List<String> patterns, String groupId) {
		if (patterns == null || patterns.isEmpty()) {
			return true;
		}
		String groupPath = normalizePath(groupPathResolver.apply(groupId));
		for (String pattern : patterns) {
			if (matchesPattern(normalizePath(pattern), groupPath)) {
				return true;
			}
		}
		return false;
	}

	private boolean matchesMethod(MagicAPIMcpProperties.Api scope, ApiInfo info) {
		if (scope.getMethods() == null || scope.getMethods().isEmpty()) {
			return true;
		}
		String method = StringUtils.upperCase(info.getMethod(), Locale.ENGLISH);
		for (String configured : scope.getMethods()) {
			if (method.equals(StringUtils.upperCase(configured, Locale.ENGLISH))) {
				return true;
			}
		}
		return false;
	}

	private boolean isOptedIn(MagicAPIMcpProperties.Api scope, ApiInfo info) {
		String optionName = scope.getOptInOptionName();
		Map<String, String> options = safeOptions(info);
		if (options != null && options.containsKey(optionName)) {
			return Boolean.parseBoolean(String.valueOf(options.get(optionName)));
		}
		Object attribute = info.getAttribute(optionName);
		if (attribute != null) {
			return Boolean.parseBoolean(String.valueOf(attribute));
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

	private boolean matchesAny(List<String> patterns, String path) {
		if (patterns == null || patterns.isEmpty()) {
			return false;
		}
		for (String pattern : patterns) {
			if (matchesPattern(normalizePath(pattern), path)) {
				return true;
			}
		}
		return false;
	}

	private boolean matchesPattern(String pattern, String path) {
		return pathMatcher.match(pattern, path) || pattern.equals(path);
	}

	private String normalizePath(String path) {
		if (StringUtils.isBlank(path)) {
			return "/";
		}
		return path.startsWith("/") ? path : "/" + path;
	}
}
