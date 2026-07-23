package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.ast;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.StatementKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NebulaSelectStatement implements NebulaSqlStatement {

	private final List<NebulaSelectItem> selectItems;
	private final NebulaTableSource from;
	private final NebulaExpression where;
	private final List<NebulaExpression> groupBy;
	private final NebulaExpression having;
	private final List<NebulaOrderByItem> orderBy;
	private final NebulaLimit limit;
	private final NebulaUnion union;

	public NebulaSelectStatement(List<NebulaSelectItem> selectItems, NebulaTableSource from, NebulaExpression where,
								 List<NebulaExpression> groupBy, NebulaExpression having,
								 List<NebulaOrderByItem> orderBy, NebulaLimit limit, NebulaUnion union) {
		this.selectItems = immutable(selectItems);
		this.from = from;
		this.where = where;
		this.groupBy = immutable(groupBy);
		this.having = having;
		this.orderBy = immutable(orderBy);
		this.limit = limit;
		this.union = union;
	}

	private static <T> List<T> immutable(List<T> values) {
		return values == null ? Collections.<T>emptyList() : Collections.unmodifiableList(new ArrayList<T>(values));
	}

	public StatementKind getKind() {
		return StatementKind.SELECT;
	}

	public List<NebulaSelectItem> getSelectItems() {
		return selectItems;
	}

	public NebulaTableSource getFrom() {
		return from;
	}

	public NebulaExpression getWhere() {
		return where;
	}

	public List<NebulaExpression> getGroupBy() {
		return groupBy;
	}

	public NebulaExpression getHaving() {
		return having;
	}

	public List<NebulaOrderByItem> getOrderBy() {
		return orderBy;
	}

	public NebulaLimit getLimit() {
		return limit;
	}

	public NebulaUnion getUnion() {
		return union;
	}
}
