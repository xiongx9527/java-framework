package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.merge.MergeInsert;
import net.sf.jsqlparser.statement.merge.MergeUpdate;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import net.sf.jsqlparser.statement.values.ValuesStatement;
import org.apache.ibatis.mapping.ParameterMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractJSqlParserDataChangeDialect implements DataChangeDialect {
	private final JSqlParserTargetDetector detector = new JSqlParserTargetDetector();

	@Override
	public List<AnalyzedStatement> analyze(BoundSqlContext context) {
		List<ParameterMapping> allMappings = context.getParameterMappings();
		List<AnalyzedStatement> analyzed = new ArrayList<AnalyzedStatement>();
		int parameterOffset = 0;
		for (JSqlParserTargetDetector.ParsedStatement parsed : detector.parseSegments(context.getSql())) {
			List<ParameterMapping> statementMappings = slice(allMappings, parameterOffset,
					parsed.getParameterCount());
			parameterOffset += parsed.getParameterCount();
			Statement statement = parsed.getStatement();
			if (parsed.isCall() || statement instanceof Execute) {
				analyzed.add(AnalyzedStatement.nonAuditable(parsed.getStatementIndex()));
				continue;
			}
			StatementOperation operation = JSqlParserTargetDetector.operation(statement);
			if (operation == null) {
				analyzed.add(AnalyzedStatement.nonAuditable(parsed.getStatementIndex()));
				continue;
			}
			List<Table> tables = JSqlParserTargetDetector.targetTables(statement);
			if (tables.isEmpty()) {
				throw new SqlAnalysisException("Unable to determine DML target table");
			}
			List<ChangeTarget> targets = new ArrayList<ChangeTarget>();
			for (Table table : tables) {
				targets.add(target(statement, operation, table, statementMappings));
			}
			analyzed.add(new AnalyzedStatement(parsed.getStatementIndex(), true, targets));
		}
		return Collections.unmodifiableList(analyzed);
	}

	private ChangeTarget target(Statement statement, StatementOperation operation, Table table,
								List<ParameterMapping> mappings) {
		Expression where = where(statement);
		List<Join> joins = joins(statement);
		List<Expression> predicates = new ArrayList<Expression>();
		for (Join join : joins) {
			if (join.getOnExpressions() != null) {
				predicates.addAll(join.getOnExpressions());
			}
		}
		if (where != null) { predicates.add(where); }
		return new ChangeTarget(operation, JSqlParserTargetDetector.identifier(table), table, where, joins,
				assignedColumns(statement, mappings), mappings, mappingsFor(predicates, mappings),
				Collections.<String>emptyList());
	}

	private Expression where(Statement statement) {
		if (statement instanceof Update) { return ((Update) statement).getWhere(); }
		if (statement instanceof Delete) { return ((Delete) statement).getWhere(); }
		if (statement instanceof Merge) {
			MergeUpdate update = ((Merge) statement).getMergeUpdate();
			return update == null ? null : update.getWhereCondition();
		}
		return null;
	}

	private List<Join> joins(Statement statement) {
		List<Join> joins = new ArrayList<Join>();
		if (statement instanceof Update) {
			Update update = (Update) statement;
			if (update.getStartJoins() != null) { joins.addAll(update.getStartJoins()); }
			if (update.getJoins() != null) { joins.addAll(update.getJoins()); }
		} else if (statement instanceof Delete && ((Delete) statement).getJoins() != null) {
			joins.addAll(((Delete) statement).getJoins());
		} else if (statement instanceof Merge) {
			Merge merge = (Merge) statement;
			Join join = new Join();
			if (merge.getUsingTable() != null) {
				merge.getUsingTable().setAlias(merge.getUsingAlias());
				join.setRightItem(merge.getUsingTable());
			} else if (merge.getUsingSelect() != null) {
				join.setRightItem(merge.getUsingSelect());
			} else {
				throw new SqlAnalysisException("Unable to determine MERGE source");
			}
			join.addOnExpression(merge.getOnCondition());
			joins.add(join);
		}
		return joins;
	}

	private List<AssignedColumn> assignedColumns(Statement statement, List<ParameterMapping> mappings) {
		List<AssignedColumn> assigned = new ArrayList<AssignedColumn>();
		if (statement instanceof Update) {
			for (UpdateSet updateSet : ((Update) statement).getUpdateSets()) {
				addAssignments(assigned, updateSet.getColumns(), updateSet.getExpressions(), mappings);
			}
		} else if (statement instanceof Insert) {
			Insert insert = (Insert) statement;
			if (insert.isUseSet()) {
				addAssignments(assigned, insert.getSetColumns(), insert.getSetExpressionList(), mappings);
			} else {
				addAssignments(assigned, insert.getColumns(), insertValues(insert), mappings);
			}
		} else if (statement instanceof Merge) {
			Merge merge = (Merge) statement;
			MergeUpdate update = merge.getMergeUpdate();
			if (update != null) {
				addAssignments(assigned, update.getColumns(), update.getValues(), mappings);
			}
			MergeInsert insert = merge.getMergeInsert();
			if (insert != null) {
				addAssignments(assigned, insert.getColumns(), insert.getValues(), mappings);
			}
		}
		return assigned;
	}

	private List<Expression> insertValues(Insert insert) {
		if (insert.getSelect() == null || !(insert.getSelect().getSelectBody() instanceof SetOperationList)) {
			return Collections.emptyList();
		}
		List<SelectBody> valuesBodies = ((SetOperationList) insert.getSelect().getSelectBody()).getSelects();
		if (valuesBodies.size() != 1 || !(valuesBodies.get(0) instanceof ValuesStatement)) {
			return Collections.emptyList();
		}
		ItemsList itemsList = ((ValuesStatement) valuesBodies.get(0)).getExpressions();
		if (itemsList instanceof ExpressionList) {
			return ((ExpressionList) itemsList).getExpressions();
		}
		return Collections.emptyList();
	}

	private void addAssignments(List<AssignedColumn> assigned, List<Column> columns,
								List<Expression> expressions, List<ParameterMapping> mappings) {
		if (columns == null) { return; }
		for (int index = 0; index < columns.size(); index++) {
			Expression expression = expressions != null && index < expressions.size() ? expressions.get(index) : null;
			assigned.add(new AssignedColumn(columns.get(index).getColumnName(), expression,
					expression == null ? Collections.<ParameterMapping>emptyList()
							: mappingsFor(Collections.singletonList(expression), mappings)));
		}
	}

	private List<ParameterMapping> mappingsFor(List<Expression> expressions, List<ParameterMapping> mappings) {
		List<Integer> indexes = new ArrayList<Integer>();
		ExpressionVisitorAdapter visitor = new ExpressionVisitorAdapter() {
			@Override
			public void visit(JdbcParameter parameter) {
				if (parameter.getIndex() != null) {
					indexes.add(parameter.getIndex());
				}
			}
		};
		for (Expression expression : expressions) {
			if (expression != null) { expression.accept(visitor); }
		}
		List<ParameterMapping> selected = new ArrayList<ParameterMapping>();
		for (Integer index : indexes) {
			int mappingIndex = index.intValue() - 1;
			if (mappingIndex >= 0 && mappingIndex < mappings.size()) {
				selected.add(mappings.get(mappingIndex));
			}
		}
		return selected;
	}

	private List<ParameterMapping> slice(List<ParameterMapping> mappings, int offset, int count) {
		if (offset >= mappings.size() || count <= 0) {
			return Collections.emptyList();
		}
		return new ArrayList<ParameterMapping>(mappings.subList(offset, Math.min(offset + count, mappings.size())));
	}
}
