package com.vesoft.nebula.jdbc.sql.write;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NebulaDdlPlan {
	private final NebulaDdlKind kind;
	private final String ngql;
	private final NebulaDdlDescriptor descriptor;
	private final List<String> warnings;
	private final List<String> trace;

	public NebulaDdlPlan(NebulaDdlKind kind, String ngql, NebulaDdlDescriptor descriptor,
			List<String> warnings, List<String> trace) {
		if (kind == null) {
			throw new IllegalArgumentException("DDL kind must not be null.");
		}
		if (isBlank(ngql)) {
			throw new IllegalArgumentException("nGQL must not be blank.");
		}
		if (descriptor == null) {
			throw new IllegalArgumentException("DDL descriptor must not be null.");
		}
		this.kind = kind;
		this.ngql = ngql;
		this.descriptor = descriptor;
		this.warnings = copy(warnings);
		this.trace = copy(trace);
	}

	public NebulaDdlKind getKind() {
		return kind;
	}

	public String getNgql() {
		return ngql;
	}

	public NebulaDdlDescriptor getDescriptor() {
		return descriptor;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public List<String> getTrace() {
		return trace;
	}

	private static List<String> copy(List<String> source) {
		if (source == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(new ArrayList<String>(source));
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
