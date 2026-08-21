package com.xiongx9527.fw.plus.ai.askdata.security;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 厂商无关的管理员授权范围。
 */
public final class AskDataAdministrativeScope {

	public enum Type {
		NONE, PROJECT, PLATFORM
	}

	private static final AskDataAdministrativeScope NONE = new AskDataAdministrativeScope(Type.NONE,
			Collections.<String>emptySet());

	private final Type type;
	private final Set<String> projectIds;

	private AskDataAdministrativeScope(Type type, Set<String> projectIds) {
		if (type == null || projectIds == null) {
			throw new IllegalArgumentException("type 和 projectIds 不能为空");
		}
		Set<String> copy = new LinkedHashSet<String>();
		for (String projectId : projectIds) {
			copy.add(requireNonBlank(projectId, "projectId"));
		}
		if (type != Type.NONE && copy.isEmpty()) {
			throw new IllegalArgumentException("管理员范围必须包含显式项目");
		}
		if (type == Type.NONE && !copy.isEmpty()) {
			throw new IllegalArgumentException("NONE 范围不能包含项目");
		}
		this.type = type;
		this.projectIds = Collections.unmodifiableSet(copy);
	}

	public static AskDataAdministrativeScope none() {
		return NONE;
	}

	public static AskDataAdministrativeScope project(String projectId) {
		return new AskDataAdministrativeScope(Type.PROJECT, Collections.singleton(projectId));
	}

	public static AskDataAdministrativeScope platform(Set<String> projectIds) {
		return new AskDataAdministrativeScope(Type.PLATFORM, projectIds);
	}

	public Type getType() {
		return type;
	}

	public Set<String> getProjectIds() {
		return projectIds;
	}

	public boolean isProjectAdministrator() {
		return type == Type.PROJECT;
	}

	public boolean isPlatformAdministrator() {
		return type == Type.PLATFORM;
	}

	public boolean allowsProject(String projectId) {
		return projectId != null && projectIds.contains(projectId);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AskDataAdministrativeScope)) {
			return false;
		}
		AskDataAdministrativeScope that = (AskDataAdministrativeScope) other;
		return type == that.type && projectIds.equals(that.projectIds);
	}

	@Override
	public int hashCode() {
		return 31 * type.hashCode() + projectIds.hashCode();
	}

	private static String requireNonBlank(String value, String fieldName) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(fieldName + " 不能为空白");
		}
		return value.trim();
	}
}
