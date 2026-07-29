package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataColumn;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataTable;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataRowPredicate;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.ParenthesisFromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AskDataSqlAuthorizationProcessor {

    private AskDataSqlAuthorizationProcessor() {
    }

    public static String validateColumns(String sql, AskDataMetadata metadata) {
        ColumnScope scope = ColumnScope.from(metadata);
        if (!scope.hasColumnScope()) {
            return null;
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                return "无法按授权列解析非 SELECT SQL";
            }
            Select select = (Select) statement;
            collectSelectStructure(select, scope);
            ColumnFinder finder = new ColumnFinder();
            finder.getTableList(statement);
            for (Column column : finder.columns) {
                if (!scope.isAuthorized(column)) {
                    return "SQL 访问了 metadata 未授权列: " + column.getFullyQualifiedName();
                }
            }
            if (!scope.areWildcardsAuthorized()) {
                return "SQL 通配列超出 metadata 授权范围";
            }
            return null;
        } catch (Exception ex) {
            return "无法安全解析 SQL 列引用";
        }
    }

    public static String injectRowPredicates(String sql, String datasourceKey,
            List<AskDataRowPredicate> rowPredicates) {
        if (rowPredicates == null || rowPredicates.isEmpty()) {
            return sql;
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                throw new IllegalArgumentException("无法向非 SELECT SQL 注入行级谓词");
            }
            boolean injected = false;
            for (AskDataRowPredicate predicate : rowPredicates) {
                if (predicate == null || !equals(datasourceKey,
                        predicate.getAuthorizedObject().getDatasourceKey())) {
                    continue;
                }
                PredicateInjector injector = new PredicateInjector(
                        predicate.getAuthorizedObject().getTableName(), predicate.getSqlFragment());
                injector.getTableList(statement);
                injected = injected || injector.injected;
            }
            return injected ? statement.toString() : sql;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("无法安全注入行级谓词", ex);
        }
    }

    private static void collectSelectStructure(Select select, ColumnScope scope) {
        if (select.getWithItemsList() != null) {
            for (WithItem withItem : select.getWithItemsList()) {
                scope.addDerivedName(normalize(withItem.getName()));
                if (withItem.getSubSelect() != null) {
                    collectSelectBody(withItem.getSubSelect().getSelectBody(), scope);
                }
            }
        }
        collectSelectBody(select.getSelectBody(), scope);
    }

    private static void collectSelectBody(SelectBody body, ColumnScope scope) {
        if (body instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) body;
            collectFromItem(plainSelect.getFromItem(), scope);
            if (plainSelect.getJoins() != null) {
                for (Join join : plainSelect.getJoins()) {
                    collectFromItem(join.getRightItem(), scope);
                }
            }
            Set<String> localSelectAliases = new HashSet<String>();
            for (SelectItem item : plainSelect.getSelectItems()) {
                if (item instanceof SelectExpressionItem) {
                    SelectExpressionItem expressionItem = (SelectExpressionItem) item;
                    if (expressionItem.getAlias() != null) {
                        localSelectAliases.add(normalize(expressionItem.getAlias().getName()));
                    }
                } else if (item instanceof AllColumns) {
                    scope.wildcardTables.add(null);
                } else if (item instanceof AllTableColumns) {
                    scope.wildcardTables.add(normalize(((AllTableColumns) item).getTable().getName()));
                }
            }
            if (plainSelect.getOrderByElements() != null) {
                for (OrderByElement orderByElement : plainSelect.getOrderByElements()) {
                    ColumnFinder finder = new ColumnFinder();
                    orderByElement.getExpression().accept(finder);
                    for (Column column : finder.columns) {
                        String qualifier = column.getTable() == null ? null
                                : normalize(column.getTable().getName());
                        if ((qualifier == null || qualifier.length() == 0)
                                && localSelectAliases.contains(normalize(column.getColumnName()))) {
                            scope.allowedSelectAliasReferences.add(column);
                        }
                    }
                }
            }
        } else if (body instanceof SetOperationList) {
            for (SelectBody selectBody : ((SetOperationList) body).getSelects()) {
                collectSelectBody(selectBody, scope);
            }
        } else if (body instanceof WithItem) {
            WithItem withItem = (WithItem) body;
            if (withItem.getSubSelect() != null) {
                collectSelectBody(withItem.getSubSelect().getSelectBody(), scope);
            }
        }
    }

    private static void collectFromItem(FromItem fromItem, ColumnScope scope) {
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            String tableName = normalize(table.getFullyQualifiedName());
            if (!scope.derivedNames.contains(tableName)) {
                scope.referencedTables.add(tableName);
            }
            if (table.getAlias() != null) {
                scope.addTableAlias(normalize(table.getAlias().getName()), tableName);
            }
        } else if (fromItem instanceof SubSelect) {
            SubSelect subSelect = (SubSelect) fromItem;
            if (subSelect.getAlias() != null) {
                scope.addDerivedName(normalize(subSelect.getAlias().getName()));
            }
            collectSelectBody(subSelect.getSelectBody(), scope);
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("`", "").replace("\"", "").trim().toLowerCase(Locale.ENGLISH);
    }

    private static boolean equals(Object first, Object second) {
        return first == null ? second == null : first.equals(second);
    }

    private static final class PredicateInjector extends TablesNamesFinder {

        private final String tableName;
        private final String sqlFragment;
        private boolean injected;

        private PredicateInjector(String tableName, String sqlFragment) {
            this.tableName = normalize(tableName);
            this.sqlFragment = sqlFragment;
        }

        @Override
        public void visit(PlainSelect plainSelect) {
            if (containsTable(plainSelect.getFromItem(), tableName)
                    || containsTable(plainSelect.getJoins(), tableName)) {
                Expression predicate;
                try {
                    predicate = CCJSqlParserUtil.parseCondExpression(sqlFragment);
                } catch (Exception ex) {
                    throw new IllegalArgumentException("无法安全解析行级谓词", ex);
                }
                Expression where = plainSelect.getWhere();
                plainSelect.setWhere(where == null ? predicate
                        : new AndExpression(new Parenthesis(where), new Parenthesis(predicate)));
                injected = true;
            }
            super.visit(plainSelect);
        }

        private static boolean containsTable(List<Join> joins, String tableName) {
            if (joins == null) {
                return false;
            }
            for (Join join : joins) {
                if (join != null && containsTable(join.getRightItem(), tableName)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean containsTable(FromItem fromItem, String tableName) {
            if (fromItem instanceof Table) {
                return tableName.equals(normalize(((Table) fromItem).getFullyQualifiedName()));
            }
            if (fromItem instanceof ParenthesisFromItem) {
                return containsTable(((ParenthesisFromItem) fromItem).getFromItem(), tableName);
            }
            if (fromItem instanceof SubJoin) {
                SubJoin subJoin = (SubJoin) fromItem;
                return containsTable(subJoin.getLeft(), tableName)
                        || containsTable(subJoin.getJoinList(), tableName);
            }
            return false;
        }
    }

    private static final class ColumnFinder extends TablesNamesFinder {

        private final List<Column> columns = new ArrayList<Column>();

        @Override
        public void visit(Column column) {
            columns.add(column);
            super.visit(column);
        }
    }

    private static final class ColumnScope {

        private final Map<String, Set<String>> columnsByTable = new HashMap<String, Set<String>>();
        private final Set<String> tableLevelTables = new HashSet<String>();
        private final Map<String, String> aliases = new HashMap<String, String>();
        private final List<String> referencedTables = new ArrayList<String>();
        private final Set<String> derivedNames = new HashSet<String>();
        private final Set<String> ambiguousQualifiers = new HashSet<String>();
        private final Set<Column> allowedSelectAliasReferences =
                java.util.Collections.newSetFromMap(new IdentityHashMap<Column, Boolean>());
        private final List<String> wildcardTables = new ArrayList<String>();

        private static ColumnScope from(AskDataMetadata metadata) {
            ColumnScope scope = new ColumnScope();
            if (metadata == null || metadata.getTables() == null) {
                return scope;
            }
            for (AskDataTable table : metadata.getTables()) {
                if (table == null || table.getName() == null) {
                    continue;
                }
                Set<String> columns = new HashSet<String>();
                for (AskDataColumn column : table.getColumns()) {
                    if (column != null && column.getName() != null) {
                        columns.add(normalize(column.getName()));
                    }
                }
                String tableName = normalize(table.getName());
                scope.columnsByTable.put(tableName, columns);
                if (table.isTableLevelAuthorized()) {
                    scope.tableLevelTables.add(tableName);
                }
            }
            return scope;
        }

        private boolean hasColumnScope() {
            return !columnsByTable.isEmpty();
        }

        private boolean isAuthorized(Column column) {
            String columnName = normalize(column.getColumnName());
            String qualifier = column.getTable() == null ? null : normalize(column.getTable().getName());
            if (qualifier == null || qualifier.length() == 0) {
                if (allowedSelectAliasReferences.contains(column)) {
                    return true;
                }
                if (referencedTables.size() != 1) {
                    return false;
                }
                return isAuthorized(referencedTables.get(0), columnName);
            }
            if (ambiguousQualifiers.contains(qualifier)) {
                return false;
            }
            if (aliases.containsKey(qualifier)) {
                return isAuthorized(aliases.get(qualifier), columnName);
            }
            if (derivedNames.contains(qualifier)) {
                return true;
            }
            return isAuthorized(qualifier, columnName);
        }

        private boolean isAuthorized(String tableName, String columnName) {
            Set<String> columns = columnsByTable.get(tableName);
            return tableLevelTables.contains(tableName) || columns != null && columns.contains(columnName);
        }

        private void addTableAlias(String alias, String tableName) {
            if (derivedNames.contains(alias) || aliases.containsKey(alias)
                    && !aliases.get(alias).equals(tableName)) {
                ambiguousQualifiers.add(alias);
            }
            aliases.put(alias, tableName);
        }

        private void addDerivedName(String name) {
            if (aliases.containsKey(name)) {
                ambiguousQualifiers.add(name);
            }
            derivedNames.add(name);
        }

        private boolean areWildcardsAuthorized() {
            for (String wildcardTable : wildcardTables) {
                if (wildcardTable == null) {
                    for (String tableName : columnsByTable.keySet()) {
                        if (!tableLevelTables.contains(tableName)) {
                            return false;
                        }
                    }
                } else {
                    String tableName = aliases.containsKey(wildcardTable) ? aliases.get(wildcardTable) : wildcardTable;
                    if (!tableLevelTables.contains(tableName)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
