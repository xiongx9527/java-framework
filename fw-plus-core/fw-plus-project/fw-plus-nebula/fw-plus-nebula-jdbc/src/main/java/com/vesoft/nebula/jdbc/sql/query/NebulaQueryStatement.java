package com.vesoft.nebula.jdbc.sql.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NebulaQueryStatement {
	private final StatementKind kind;
	private final String tableName;
	private final String alias;
	private final boolean selectAll;
	private final List<QueryProjection> projections;
	private final QueryExpression where;
	private final String orderByColumn;
	private final SortDirection sortDirection;
	private final Integer limit;
	private final Integer offset;
	private final UnsupportedQueryFeature unsupportedFeature;
	private final String unsupportedReason;

	private NebulaQueryStatement(Builder builder) {
		this.kind = builder.kind;
		this.tableName = builder.tableName;
		this.alias = builder.alias == null ? builder.tableName : builder.alias;
		this.selectAll = builder.selectAll;
		this.projections = Collections.unmodifiableList(new ArrayList<QueryProjection>(builder.projections));
		this.where = builder.where;
		this.orderByColumn = builder.orderByColumn;
		this.sortDirection = builder.sortDirection;
		this.limit = builder.limit;
		this.offset = builder.offset;
		this.unsupportedFeature = builder.unsupportedFeature;
		this.unsupportedReason = builder.unsupportedReason;
	}

	public static Builder select(String tableName) {
		return new Builder(StatementKind.SELECT, tableName);
	}

	public static NebulaQueryStatement nonQuery(StatementKind statementKind) {
		return new Builder(statementKind, null).build();
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

	public List<QueryProjection> getProjections() {
		return projections;
	}

	public QueryExpression getWhere() {
		return where;
	}

	public String getOrderByColumn() {
		return orderByColumn;
	}

	public SortDirection getSortDirection() {
		return sortDirection;
	}

	public Integer getLimit() {
		return limit;
	}

	public Integer getOffset() {
		return offset;
	}

	public UnsupportedQueryFeature getUnsupportedFeature() {
		return unsupportedFeature;
	}

	public String getUnsupportedReason() {
		return unsupportedReason;
	}

	public static class Builder {
		private final StatementKind kind;
			private final String tableName;
			private String alias;
			private boolean selectAll;
			private final List<QueryProjection> projections = new ArrayList<QueryProjection>();
			private QueryExpression where;
			private String orderByColumn;
		private SortDirection sortDirection;
		private Integer limit;
		private Integer offset;
		private UnsupportedQueryFeature unsupportedFeature;
		private String unsupportedReason;

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

			public Builder projection(QueryExpression expression, String alias) {
				this.projections.add(new QueryProjection(expression, alias));
				return this;
			}

			public Builder where(QueryExpression where) {
				this.where = where;
				return this;
		}

		public Builder orderBy(String column, SortDirection direction) {
			this.orderByColumn = column;
			this.sortDirection = direction;
			return this;
		}

		public Builder limit(int limit) {
			this.limit = Integer.valueOf(limit);
			return this;
		}

		public Builder offset(int offset) {
			this.offset = Integer.valueOf(offset);
			return this;
		}

		public Builder unsupported(UnsupportedQueryFeature feature, String reason) {
			this.unsupportedFeature = feature;
			this.unsupportedReason = reason;
			return this;
		}

		public NebulaQueryStatement build() {
			return new NebulaQueryStatement(this);
		}
	}
}
