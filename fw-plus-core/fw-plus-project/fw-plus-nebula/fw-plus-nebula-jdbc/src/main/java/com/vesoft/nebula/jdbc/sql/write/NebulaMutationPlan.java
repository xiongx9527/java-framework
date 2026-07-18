package com.vesoft.nebula.jdbc.sql.write;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NebulaMutationPlan {
	private final NebulaMutationKind mutationKind;
	private final String ngql;
	private final String targetName;
	private final Map<String, Object> keys;
	private final Map<String, Object> properties;
	private final String affectedRowsPolicy;
	private final NebulaMutationDescriptor descriptor;
	private final List<String> trace;

	public NebulaMutationPlan(NebulaMutationKind mutationKind, String ngql, String targetName,
			Map<String, Object> keys, Map<String, Object> properties, String affectedRowsPolicy,
			NebulaMutationDescriptor descriptor, List<String> trace) {
		if (mutationKind == null) {
			throw new IllegalArgumentException("Mutation kind must not be null.");
		}
		if (isBlank(ngql)) {
			throw new IllegalArgumentException("nGQL must not be blank.");
		}
		if (descriptor == null) {
			throw new IllegalArgumentException("Mutation descriptor must not be null.");
		}
		this.mutationKind = mutationKind;
		this.ngql = ngql;
		this.targetName = targetName;
		this.keys = copy(keys);
		this.properties = copy(properties);
		this.affectedRowsPolicy = affectedRowsPolicy;
		this.descriptor = descriptor;
		this.trace = trace == null ? Collections.<String>emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(trace));
	}

	public NebulaMutationKind getMutationKind() {
		return mutationKind;
	}

	public String getNgql() {
		return ngql;
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

	public String getAffectedRowsPolicy() {
		return affectedRowsPolicy;
	}

	public NebulaMutationDescriptor getDescriptor() {
		return descriptor;
	}

	public List<String> getTrace() {
		return trace;
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
