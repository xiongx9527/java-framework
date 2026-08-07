package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import net.sf.jsqlparser.expression.Expression;
import org.apache.ibatis.mapping.ParameterMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AssignedColumn {
	private final String column;
	private final Expression expression;
	private final List<ParameterMapping> parameterMappings;

	public AssignedColumn(String column, Expression expression, List<ParameterMapping> parameterMappings) {
		this.column = column;
		this.expression = expression;
		this.parameterMappings = immutable(parameterMappings);
	}

	public String getColumn() { return column; }
	public Expression getExpression() { return expression; }
	public List<ParameterMapping> getParameterMappings() { return parameterMappings; }

	private List<ParameterMapping> immutable(List<ParameterMapping> mappings) {
		return Collections.unmodifiableList(new ArrayList<ParameterMapping>(
				mappings == null ? Collections.<ParameterMapping>emptyList() : mappings));
	}
}
