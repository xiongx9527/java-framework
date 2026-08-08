package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;

public final class OracleDataChangeDialect extends AbstractStandardDataChangeDialect {
	@Override
	public boolean supports(DatabaseMetadata metadata) {
		return metadata != null && "oracle".equals(metadata.getProductName());
	}

	@Override
	protected void appendLimit(StringBuilder sql, ChangeTarget target, boolean hasWhere) {
		sql.append(hasWhere ? " AND " : " WHERE ").append("ROWNUM <= ").append(queryLimit(target));
	}

	@Override
	protected String quote(String identifier) {
		String value = identifier == null ? "" : identifier;
		if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
			value = value.substring(1, value.length() - 1).replace("\"\"", "\"");
		} else {
			value = value.toUpperCase(java.util.Locale.ENGLISH);
		}
		return '"' + value.replace("\"", "\"\"") + '"';
	}

	@Override
	protected boolean isSingleRowSource(FromItem fromItem) {
		if (!(fromItem instanceof Table)) { return false; }
		Table table = (Table) fromItem;
		String name = table.getName();
		String database = table.getDatabase() == null ? null : table.getDatabase().getDatabaseName();
		return table.getNameParts().size() == 1
				&& name != null && name.equals(table.getNameParts().get(0))
				&& table.getSchemaName() == null && (database == null || database.isEmpty())
				&& "dual".equalsIgnoreCase(name)
				&& table.getAlias() == null && table.getPivot() == null && table.getUnPivot() == null
				&& table.getIndexHint() == null && table.getSqlServerHints() == null;
	}
}
