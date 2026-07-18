package com.vesoft.nebula.jdbc.sql.write;

import com.vesoft.nebula.jdbc.sql.parser.StatementKind;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaColumnDefinition;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaMetadataStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSchemaStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSqlStatement;
import com.vesoft.nebula.jdbc.sql.query.ColumnMapping;
import com.vesoft.nebula.jdbc.sql.query.TableKind;
import com.vesoft.nebula.jdbc.sql.query.TableMapping;
import com.vesoft.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefaultNebulaSqlDdlConverter implements NebulaSqlDdlConverter {

	public NebulaDdlPlan convert(NebulaSqlStatement statement, NebulaSchemaMappingRegistry schemaMapping)
			throws SQLException {
		if (statement instanceof NebulaSchemaStatement) {
			return convertSchema((NebulaSchemaStatement) statement, schemaMapping);
		}
		if (statement instanceof NebulaMetadataStatement) {
			return convertMetadata((NebulaMetadataStatement) statement, schemaMapping);
		}
		throw unsupported("Unsupported DDL statement: " + statement.getKind());
	}

	private NebulaDdlPlan convertSchema(NebulaSchemaStatement statement, NebulaSchemaMappingRegistry schemaMapping)
			throws SQLException {
		if (!statement.getUnsupportedOptions().isEmpty()) {
			throw unsupported("Unsupported CREATE TABLE options: " + join(statement.getUnsupportedOptions()));
		}
		TableMapping tableMapping = schemaMapping.getRequired(statement.getTableName());
		if (statement.getKind() == StatementKind.CREATE_SCHEMA) {
			return createPlan(statement, tableMapping);
		}
		if (statement.getKind() == StatementKind.DROP_SCHEMA) {
			return dropPlan(statement, tableMapping);
		}
		throw unsupported("Unsupported schema statement: " + statement.getKind());
	}

	private NebulaDdlPlan createPlan(NebulaSchemaStatement statement, TableMapping tableMapping) throws SQLException {
		List<String> columns = new ArrayList<String>();
		for (Map.Entry<String, ColumnMapping> entry : tableMapping.getProperties().entrySet()) {
			NebulaColumnDefinition definition = findColumn(statement, entry.getKey());
			columns.add(entry.getValue().getNebulaProperty() + " " + toNebulaType(definition.getDataType()));
		}
		NebulaDdlKind kind = tableMapping.getTableKind() == TableKind.TAG
				? NebulaDdlKind.CREATE_TAG : NebulaDdlKind.CREATE_EDGE;
		String objectKind = tableMapping.getTableKind() == TableKind.TAG ? "TAG" : "EDGE";
		String ngql = "CREATE " + objectKind + " " + tableMapping.getNebulaName() + "(" + join(columns) + ")";
		return plan(kind, tableMapping.getNebulaName(), statement.getColumns(), ngql,
				"create " + tableMapping.getTableKind() + " from " + statement.getTableName());
	}

	private NebulaDdlPlan dropPlan(NebulaSchemaStatement statement, TableMapping tableMapping) {
		NebulaDdlKind kind = tableMapping.getTableKind() == TableKind.TAG
				? NebulaDdlKind.DROP_TAG : NebulaDdlKind.DROP_EDGE;
		String objectKind = tableMapping.getTableKind() == TableKind.TAG ? "TAG" : "EDGE";
		String ngql = "DROP " + objectKind + " " + tableMapping.getNebulaName();
		return plan(kind, tableMapping.getNebulaName(), statement.getColumns(), ngql,
				"drop " + tableMapping.getTableKind() + " from " + statement.getTableName());
	}

	private NebulaDdlPlan convertMetadata(NebulaMetadataStatement statement, NebulaSchemaMappingRegistry schemaMapping)
			throws SQLException {
		if (statement.getTargetName() == null) {
			throw unsupported("Metadata statement without target is not a tag/edge schema plan.");
		}
		TableMapping tableMapping = schemaMapping.getRequired(statement.getTargetName());
		NebulaDdlKind kind = tableMapping.getTableKind() == TableKind.TAG
				? NebulaDdlKind.DESCRIBE_TAG : NebulaDdlKind.DESCRIBE_EDGE;
		String objectKind = tableMapping.getTableKind() == TableKind.TAG ? "TAG" : "EDGE";
		String ngql = "DESCRIBE " + objectKind + " " + tableMapping.getNebulaName();
		return plan(kind, tableMapping.getNebulaName(), Collections.<NebulaColumnDefinition>emptyList(), ngql,
				"describe " + tableMapping.getTableKind() + " from " + statement.getTargetName());
	}

	private NebulaColumnDefinition findColumn(NebulaSchemaStatement statement, String sqlColumn) throws SQLException {
		for (NebulaColumnDefinition column : statement.getColumns()) {
			if (sqlColumn.equals(column.getName())) {
				return column;
			}
		}
		throw unsupported("Mapped property column is missing from CREATE TABLE: " + sqlColumn);
	}

	private NebulaDdlPlan plan(NebulaDdlKind kind, String schemaName, List<NebulaColumnDefinition> columns,
			String ngql, String trace) {
		NebulaDdlDescriptor descriptor = new NebulaDdlDescriptor(kind, schemaName, columns, null, ngql);
		return new NebulaDdlPlan(kind, ngql, descriptor, Collections.<String>emptyList(),
				Collections.singletonList(trace));
	}

	private String toNebulaType(String dataType) throws SQLException {
		String normalized = dataType == null ? "" : dataType.toUpperCase();
		if (normalized.indexOf("CHAR") >= 0 || normalized.indexOf("TEXT") >= 0 || normalized.indexOf("STRING") >= 0) {
			return "string";
		}
		if (normalized.indexOf("INT") >= 0) {
			return "int64";
		}
		if (normalized.indexOf("DOUBLE") >= 0 || normalized.indexOf("FLOAT") >= 0 || normalized.indexOf("DECIMAL") >= 0) {
			return "double";
		}
		if (normalized.indexOf("BOOL") >= 0) {
			return "bool";
		}
		if (normalized.indexOf("DATE") >= 0 || normalized.indexOf("TIME") >= 0) {
			return "datetime";
		}
		throw unsupported("Unsupported column type: " + dataType);
	}

	private NebulaWriteConversionException unsupported(String message) {
		return NebulaWriteConversionException.of(NebulaWriteErrorCode.NEBULA_SQL_DDL_UNSUPPORTED_OPTION, message);
	}

	private String join(List<String> values) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < values.size(); i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append(values.get(i));
		}
		return builder.toString();
	}
}
