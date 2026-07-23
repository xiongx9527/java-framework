package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NebulaMutationDescriptor {
	private final NebulaMutationKind mutationKind;
	private final String targetName;
	private final Map<String, Object> keys;
	private final Map<String, Object> properties;
	private final String sourceSql;
	private final String ngql;

	public NebulaMutationDescriptor(NebulaMutationKind mutationKind, String targetName, Map<String, Object> keys,
			Map<String, Object> properties, String sourceSql, String ngql) {
		if (mutationKind == null) {
			throw new IllegalArgumentException("Mutation kind must not be null.");
		}
		if (isBlank(targetName)) {
			throw new IllegalArgumentException("Target name must not be blank.");
		}
		if (isBlank(ngql)) {
			throw new IllegalArgumentException("nGQL must not be blank.");
		}
		this.mutationKind = mutationKind;
		this.targetName = targetName;
		this.keys = copy(keys);
		this.properties = copy(properties);
		this.sourceSql = sourceSql;
		this.ngql = ngql;
	}

	public NebulaMutationKind getMutationKind() {
		return mutationKind;
	}

	public String getTargetName() {
		return targetName;
	}

	public Map<String, Object> getKeys() {
		return keys;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public String getSourceSql() {
		return sourceSql;
	}

	public String getNgql() {
		return ngql;
	}

	private static Map<String, Object> copy(Map<String, Object> source) {
		if (source == null) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(source));
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
