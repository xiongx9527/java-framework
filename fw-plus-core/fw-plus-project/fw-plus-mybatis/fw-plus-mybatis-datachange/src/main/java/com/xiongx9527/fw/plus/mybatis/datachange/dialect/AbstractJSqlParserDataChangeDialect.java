package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.RowConstructor;
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
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;
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
				analyzed.add(AnalyzedStatement.nonAuditable(parsed.getStatementIndex(),
						AnalyzedStatement.JdbcResultRequirement.OPTIONAL));
				continue;
			}
			StatementOperation operation = JSqlParserTargetDetector.operation(statement);
			if (operation == null) {
				if (JSqlParserTargetDetector.isDmlToken(parsed.getFirstTokenKind())) {
					throw new SqlAnalysisException("SQL statement is not supported for data change auditing");
				}
				analyzed.add(AnalyzedStatement.nonAuditable(parsed.getStatementIndex(),
						statement instanceof Select ? AnalyzedStatement.JdbcResultRequirement.RESULT_SET
								: AnalyzedStatement.JdbcResultRequirement.UPDATE_COUNT));
				continue;
			}
			if (statement instanceof Insert && !JSqlParserTargetDetector.isSupportedInsert((Insert) statement)) {
				throw new SqlAnalysisException("INSERT statement cannot be modelled reliably");
			}
			List<Table> tables = JSqlParserTargetDetector.targetTables(statement);
			if (tables.isEmpty()) {
				throw new SqlAnalysisException("Unable to determine DML target table");
			}
			List<ChangeTarget> targets = new ArrayList<ChangeTarget>();
			for (Table table : tables) {
				targets.add(target(statement, operation, table, statementMappings, context));
			}
			analyzed.add(AnalyzedStatement.auditable(parsed.getStatementIndex(),
					jdbcResultRequirement(targets), targets));
		}
		return Collections.unmodifiableList(analyzed);
	}

	private AnalyzedStatement.JdbcResultRequirement jdbcResultRequirement(List<ChangeTarget> targets) {
		for (ChangeTarget target : targets) {
			if (target.isReturnsRows()) { return AnalyzedStatement.JdbcResultRequirement.RETURNING; }
		}
		return AnalyzedStatement.JdbcResultRequirement.UPDATE_COUNT;
	}

	private ChangeTarget target(Statement statement, StatementOperation operation, Table table,
									List<ParameterMapping> mappings, BoundSqlContext context) {
		Expression where = where(statement);
		List<Join> joins = joins(statement);
		List<Expression> matchExpressions = new ArrayList<Expression>();
		for (Join join : joins) {
			addJoinSourceExpressions(matchExpressions, join);
			if (join.getOnExpressions() != null) {
				matchExpressions.addAll(join.getOnExpressions());
			}
		}
		List<Expression> predicateExpressions = new ArrayList<Expression>(matchExpressions);
		if (where != null) { predicateExpressions.add(where); }
		Table snapshotSource = statement instanceof Delete ? ((Delete) statement).getTable() : table;
		return new ChangeTarget(operation, JSqlParserTargetDetector.identifier(table), table, where, joins,
					assignedColumns(statement, mappings), mappings, mappingsFor(predicateExpressions, mappings),
					Collections.<String>emptyList(), context.getBoundSql(), snapshotSource, isUpsert(statement),
					conflictTarget(statement)).withMatchParameterMappings(mappingsFor(matchExpressions, mappings))
					.withReturnsRows(returnsRows(statement));
	}

	private boolean isUpsert(Statement statement) {
		if (!(statement instanceof Insert)) { return false; }
		Insert insert = (Insert) statement;
		return insert.isUseDuplicate() || insert.getConflictAction() != null;
	}

	private boolean returnsRows(Statement statement) {
		if (statement instanceof Insert) {
			return ((Insert) statement).getReturningExpressionList() != null;
		}
		if (statement instanceof Update) {
			return ((Update) statement).getReturningExpressionList() != null;
		}
		return statement instanceof Delete && ((Delete) statement).getReturningExpressionList() != null;
	}

	private ConflictTarget conflictTarget(Statement statement) {
		if (!(statement instanceof Insert)) { return ConflictTarget.none(); }
		net.sf.jsqlparser.statement.insert.InsertConflictTarget conflict =
				((Insert) statement).getConflictTarget();
		if (conflict == null) { return ConflictTarget.none(); }
		if (conflict.getConstraintName() != null && !conflict.getConstraintName().trim().isEmpty()) {
			return ConflictTarget.constraint(conflict.getConstraintName());
		}
		if (conflict.getIndexExpression() != null) {
			return ConflictTarget.expression(conflict.getIndexExpression());
		}
		if (conflict.getIndexColumnName() != null && !conflict.getIndexColumnName().trim().isEmpty()) {
			return ConflictTarget.columns(Collections.singletonList(conflict.getIndexColumnName()));
		}
		return ConflictTarget.none();
	}

	private void addJoinSourceExpressions(List<Expression> expressions, Join join) {
		if (join.getRightItem() instanceof Table) {
			return;
		}
		if (!(join.getRightItem() instanceof SubSelect)) {
			throw new SqlAnalysisException("JOIN source cannot be modelled reliably");
		}
		SubSelect subSelect = (SubSelect) join.getRightItem();
		if (subSelect.getWithItemsList() != null && !subSelect.getWithItemsList().isEmpty()) {
			throw new SqlAnalysisException("JOIN source cannot be modelled reliably");
		}
		if (!(subSelect.getSelectBody() instanceof PlainSelect)) {
			throw new SqlAnalysisException("JOIN source cannot be modelled reliably");
		}
		PlainSelect select = (PlainSelect) subSelect.getSelectBody();
		if ((select.getFromItem() != null && !isSingleRowSource(select.getFromItem()))
				|| select.getJoins() != null || select.getWhere() != null
				|| select.getHaving() != null || select.getGroupBy() != null
				|| select.getOrderByElements() != null || select.getLimit() != null || select.getOffset() != null
				|| select.getFetch() != null || hasUnsupportedSourceClause(select)) {
			throw new SqlAnalysisException("JOIN source cannot be modelled reliably");
		}
		if (select.getTop() != null && select.getTop().getExpression() != null) {
			expressions.add(select.getTop().getExpression());
		}
		for (SelectItem item : select.getSelectItems()) {
			if (!(item instanceof SelectExpressionItem)) {
				throw new SqlAnalysisException("JOIN source cannot be modelled reliably");
			}
			expressions.add(((SelectExpressionItem) item).getExpression());
		}
	}

	protected boolean isSingleRowSource(FromItem fromItem) {
		return false;
	}

	private boolean hasUnsupportedSourceClause(PlainSelect select) {
		return select.getIntoTables() != null && !select.getIntoTables().isEmpty()
				|| select.getOptimizeFor() != null || select.getSkip() != null || select.getFirst() != null
				|| select.getDistinct() != null || select.getOracleHierarchical() != null
				|| select.isOracleSiblings() || select.isForUpdate() || select.getForUpdateTable() != null
				|| select.getOracleHint() != null || select.getWait() != null || select.getForXmlPath() != null
				|| select.getKsqlWindow() != null || select.isEmitChanges() || select.getWithIsolation() != null
				|| select.getWindowDefinitions() != null && !select.getWindowDefinitions().isEmpty()
				|| select.isSkipLocked() || select.getMySqlHintStraightJoin()
				|| select.getMySqlSqlCalcFoundRows() || select.getMySqlSqlCacheFlag() != null
				|| select.isNoWait();
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
				merge.getUsingSelect().setUseBrackets(true);
				merge.getUsingSelect().setAlias(merge.getUsingAlias());
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
			List<Expression> rows = ((ExpressionList) itemsList).getExpressions();
			if (rows.size() == 1 && !(rows.get(0) instanceof RowConstructor)) {
				return rows;
			}
			if (rows.size() == 1) {
				ExpressionList values = ((RowConstructor) rows.get(0)).getExprList();
				return values == null ? Collections.<Expression>emptyList() : values.getExpressions();
			}
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
