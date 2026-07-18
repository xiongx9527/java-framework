package com.vesoft.nebula.jdbc.sql.write;

import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaColumnDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NebulaDdlDescriptor {
	private final NebulaDdlKind kind;
	private final String schemaName;
	private final List<NebulaColumnDefinition> columns;
	private final String sourceSql;
	private final String ngql;

	public NebulaDdlDescriptor(NebulaDdlKind kind, String schemaName, List<NebulaColumnDefinition> columns,
			String sourceSql, String ngql) {
		if (kind == null) {
			throw new IllegalArgumentException("DDL kind must not be null.");
		}
		if (isBlank(schemaName)) {
			throw new IllegalArgumentException("Schema name must not be blank.");
		}
		if (isBlank(ngql)) {
			throw new IllegalArgumentException("nGQL must not be blank.");
		}
		this.kind = kind;
		this.schemaName = schemaName;
		this.columns = columns == null ? Collections.<NebulaColumnDefinition>emptyList()
				: Collections.unmodifiableList(new ArrayList<NebulaColumnDefinition>(columns));
		this.sourceSql = sourceSql;
		this.ngql = ngql;
	}

	public NebulaDdlKind getKind() {
		return kind;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public List<NebulaColumnDefinition> getColumns() {
		return columns;
	}

	public String getSourceSql() {
		return sourceSql;
	}

	public String getNgql() {
		return ngql;
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
