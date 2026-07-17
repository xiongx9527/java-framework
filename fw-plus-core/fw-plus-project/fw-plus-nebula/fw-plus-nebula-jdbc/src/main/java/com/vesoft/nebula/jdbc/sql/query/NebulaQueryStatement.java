package com.vesoft.nebula.jdbc.sql.query;

public class NebulaQueryStatement {
	private final StatementKind kind;
	private final String tableName;
	private final String alias;
	private final boolean selectAll;
	private final QueryExpression where;

	private NebulaQueryStatement(Builder builder) {
		this.kind = builder.kind;
		this.tableName = builder.tableName;
		this.alias = builder.alias == null ? builder.tableName : builder.alias;
		this.selectAll = builder.selectAll;
		this.where = builder.where;
	}

	public static Builder select(String tableName) {
		return new Builder(StatementKind.SELECT, tableName);
	}

	public StatementKind getKind() {
		return kind;
	}

	public String getTableName() {
		return tableName;
	}

	public String getAlias() {
		return alias;
	}

	public boolean isSelectAll() {
		return selectAll;
	}

	public QueryExpression getWhere() {
		return where;
	}

	public static class Builder {
		private final StatementKind kind;
		private final String tableName;
		private String alias;
		private boolean selectAll;
		private QueryExpression where;

		private Builder(StatementKind kind, String tableName) {
			this.kind = kind;
			this.tableName = tableName;
		}

		public Builder alias(String alias) {
			this.alias = alias;
			return this;
		}

		public Builder selectAll() {
			this.selectAll = true;
			return this;
		}

		public Builder where(QueryExpression where) {
			this.where = where;
			return this;
		}

		public NebulaQueryStatement build() {
			return new NebulaQueryStatement(this);
		}
	}
}
