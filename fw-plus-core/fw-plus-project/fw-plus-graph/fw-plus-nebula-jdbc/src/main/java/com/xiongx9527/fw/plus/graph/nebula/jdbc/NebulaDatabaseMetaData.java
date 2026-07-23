package com.xiongx9527.fw.plus.graph.nebula.jdbc;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeRecord;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeValue;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.utils.JdbcWrapperSupport;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.utils.NebulaPropertyKey;

public class NebulaDatabaseMetaData implements DatabaseMetaData {

	public static final String PRODUCT_NAME = "NebulaGraph";

	private final NebulaConnection connection;

	public NebulaDatabaseMetaData(NebulaConnection connection) {
		this.connection = connection;
	}

	@Override
	public boolean allProceduresAreCallable() throws SQLException {
		return false;
	}

	@Override
	public boolean allTablesAreSelectable() throws SQLException {
		return true;
	}

	@Override
	public String getURL() throws SQLException {
		return connection.getUrl();
	}

	@Override
	public String getUserName() throws SQLException {
		return connection.getConnectionConfig().getProperty(NebulaPropertyKey.USER.getKeyName(), "");
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return false;
	}

	@Override
	public boolean nullsAreSortedHigh() throws SQLException {
		return false;
	}

	@Override
	public boolean nullsAreSortedLow() throws SQLException {
		return false;
	}

	@Override
	public boolean nullsAreSortedAtStart() throws SQLException {
		return false;
	}

	@Override
	public boolean nullsAreSortedAtEnd() throws SQLException {
		return false;
	}

	@Override
	public String getDatabaseProductName() throws SQLException {
		return PRODUCT_NAME;
	}

	@Override
	public String getDatabaseProductVersion() throws SQLException {
		return "";
	}

	@Override
	public String getDriverName() throws SQLException {
		return NebulaDriver.DRIVER_NAME;
	}

	@Override
	public String getDriverVersion() throws SQLException {
		return "0.1.1-SNAPSHOT";
	}

	@Override
	public int getDriverMajorVersion() {
		return new NebulaDriver().getMajorVersion();
	}

	@Override
	public int getDriverMinorVersion() {
		return new NebulaDriver().getMinorVersion();
	}

	@Override
	public boolean usesLocalFiles() throws SQLException {
		return false;
	}

	@Override
	public boolean usesLocalFilePerTable() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsMixedCaseIdentifiers() throws SQLException {
		return true;
	}

	@Override
	public boolean storesUpperCaseIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesLowerCaseIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesMixedCaseIdentifiers() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
		return true;
	}

	@Override
	public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
		return false;
	}

	@Override
	public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
		return true;
	}

	@Override
	public String getIdentifierQuoteString() throws SQLException {
		return "`";
	}

	@Override
	public String getSQLKeywords() throws SQLException {
		return "EDGE,GO,LOOKUP,MATCH,TAG,UPSERT,VERTEX,YIELD";
	}

	@Override
	public String getNumericFunctions() throws SQLException {
		return "";
	}

	@Override
	public String getStringFunctions() throws SQLException {
		return "";
	}

	@Override
	public String getSystemFunctions() throws SQLException {
		return "";
	}

	@Override
	public String getTimeDateFunctions() throws SQLException {
		return "";
	}

	@Override
	public String getSearchStringEscape() throws SQLException {
		return "\\";
	}

	@Override
	public String getExtraNameCharacters() throws SQLException {
		return "_";
	}

	@Override
	public boolean supportsAlterTableWithAddColumn() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsAlterTableWithDropColumn() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsColumnAliasing() throws SQLException {
		return true;
	}

	@Override
	public boolean nullPlusNonNullIsNull() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsConvert() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsConvert(int fromType, int toType) throws SQLException {
		return fromType == toType;
	}

	@Override
	public boolean supportsTableCorrelationNames() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsDifferentTableCorrelationNames() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsExpressionsInOrderBy() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsOrderByUnrelated() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsGroupBy() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsGroupByUnrelated() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsGroupByBeyondSelect() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsLikeEscapeClause() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsMultipleResultSets() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsMultipleTransactions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsNonNullableColumns() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsMinimumSQLGrammar() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsCoreSQLGrammar() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsExtendedSQLGrammar() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsANSI92EntryLevelSQL() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsANSI92IntermediateSQL() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsANSI92FullSQL() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsIntegrityEnhancementFacility() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsOuterJoins() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsFullOuterJoins() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsLimitedOuterJoins() throws SQLException {
		return false;
	}

	@Override
	public String getSchemaTerm() throws SQLException {
		return "graphspace";
	}

	@Override
	public String getProcedureTerm() throws SQLException {
		return "procedure";
	}

	@Override
	public String getCatalogTerm() throws SQLException {
		return "";
	}

	@Override
	public boolean isCatalogAtStart() throws SQLException {
		return false;
	}

	@Override
	public String getCatalogSeparator() throws SQLException {
		return "";
	}

	@Override
	public boolean supportsSchemasInDataManipulation() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSchemasInProcedureCalls() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSchemasInTableDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSchemasInIndexDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCatalogsInDataManipulation() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCatalogsInProcedureCalls() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCatalogsInTableDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsPositionedDelete() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsPositionedUpdate() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSelectForUpdate() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsStoredProcedures() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsSubqueriesInComparisons() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSubqueriesInExists() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSubqueriesInIns() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsSubqueriesInQuantifieds() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsCorrelatedSubqueries() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsUnion() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsUnionAll() throws SQLException {
		return true;
	}

	@Override
	public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
		return false;
	}

	@Override
	public int getMaxBinaryLiteralLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxCharLiteralLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnsInGroupBy() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnsInIndex() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnsInOrderBy() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnsInSelect() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxColumnsInTable() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxConnections() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxCursorNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxIndexLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxSchemaNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxProcedureNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxCatalogNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxRowSize() throws SQLException {
		return 0;
	}

	@Override
	public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
		return false;
	}

	@Override
	public int getMaxStatementLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxStatements() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxTableNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxTablesInSelect() throws SQLException {
		return 0;
	}

	@Override
	public int getMaxUserNameLength() throws SQLException {
		return 0;
	}

	@Override
	public int getDefaultTransactionIsolation() throws SQLException {
		return Connection.TRANSACTION_NONE;
	}

	@Override
	public boolean supportsTransactions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
		return false;
	}

	@Override
	public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
		return false;
	}

	@Override
	public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
		return false;
	}

	@Override
	public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
		return false;
	}

	@Override
	public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
		return emptyResultSet(
				new String[] {"PROCEDURE_CAT", "PROCEDURE_SCHEM", "PROCEDURE_NAME", "RESERVED1", "RESERVED2",
						"RESERVED3", "REMARKS", "PROCEDURE_TYPE", "SPECIFIC_NAME"},
				new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
						Types.VARCHAR, Types.VARCHAR, Types.SMALLINT, Types.VARCHAR});
	}

	@Override
	public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
		return emptyResultSet(
				new String[] {"PROCEDURE_CAT", "PROCEDURE_SCHEM", "PROCEDURE_NAME", "COLUMN_NAME", "COLUMN_TYPE",
						"DATA_TYPE", "TYPE_NAME", "PRECISION", "LENGTH", "SCALE", "RADIX", "NULLABLE", "REMARKS",
						"COLUMN_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH", "ORDINAL_POSITION",
						"IS_NULLABLE", "SPECIFIC_NAME"},
				new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.SMALLINT,
						Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.SMALLINT, Types.SMALLINT,
						Types.SMALLINT, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER,
						Types.INTEGER, Types.VARCHAR, Types.VARCHAR});
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
		String[] columnNames = new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS", "TYPE_CAT",
				"TYPE_SCHEM", "TYPE_NAME", "SELF_REFERENCING_COL_NAME", "REF_GENERATION"};
		int[] columnTypes = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
				Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		String schema = currentGraphspace();
		if (connection == null || !matchesPattern(schema, schemaPattern)) {
			return emptyResultSet(columnNames, columnTypes);
		}
		List<Object[]> rows = new ArrayList<Object[]>();
		for (TableDescriptor table : tableDescriptors(types)) {
			if (matchesPattern(table.name, tableNamePattern)) {
				rows.add(new Object[] {null, schema, table.name, table.type, "", null, null, null, null, null});
			}
		}
		return rowsResultSet(columnNames, columnTypes, rows.toArray(new Object[rows.size()][]));
	}

	@Override
	public ResultSet getSchemas() throws SQLException {
		return singleRowResultSet(
				new String[] {"TABLE_SCHEM", "TABLE_CATALOG"},
				new int[] {Types.VARCHAR, Types.VARCHAR},
				new Object[] {currentGraphspace(), null});
	}

	@Override
	public ResultSet getCatalogs() throws SQLException {
		return emptyResultSet(new String[] {"TABLE_CAT"}, new int[] {Types.VARCHAR});
	}

	@Override
	public ResultSet getTableTypes() throws SQLException {
		return rowsResultSet(
				new String[] {"TABLE_TYPE"},
				new int[] {Types.VARCHAR},
				new Object[][] {{"TAG"}, {"EDGE"}});
	}

	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		String[] columnNames = new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME",
				"COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "NUM_PREC_RADIX", "NULLABLE", "REMARKS",
				"COLUMN_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH", "ORDINAL_POSITION",
				"IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA", "SCOPE_TABLE", "SOURCE_DATA_TYPE",
				"IS_AUTOINCREMENT", "IS_GENERATEDCOLUMN"};
		int[] columnTypes = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR,
				Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR,
				Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR,
				Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.SMALLINT, Types.VARCHAR, Types.VARCHAR};
		String schema = currentGraphspace();
		if (connection == null || !matchesPattern(schema, schemaPattern)) {
			return emptyResultSet(columnNames, columnTypes);
		}
		List<Object[]> rows = new ArrayList<Object[]>();
		for (TableDescriptor table : tableDescriptors(null)) {
			if (!matchesPattern(table.name, tableNamePattern)) {
				continue;
			}
			List<ColumnDescriptor> columns = describe(table);
			for (int i = 0; i < columns.size(); i++) {
				ColumnDescriptor column = columns.get(i);
				if (!matchesPattern(column.name, columnNamePattern)) {
					continue;
				}
				int jdbcType = jdbcType(column.typeName);
				Integer size = columnSize(column.typeName);
				rows.add(new Object[] {
						null, schema, table.name, column.name, Integer.valueOf(jdbcType), column.typeName,
						size, null, decimalDigits(column.typeName), Integer.valueOf(10), Integer.valueOf(column.nullable),
						column.comment, column.defaultValue, null, null, charOctetLength(jdbcType, size),
						Integer.valueOf(i + 1), nullableText(column.nullable), null, null, null, null, "NO", "NO"
				});
			}
		}
		return rowsResultSet(columnNames, columnTypes, rows.toArray(new Object[rows.size()][]));
	}

	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
		return emptyResultSet(
				new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "GRANTOR", "GRANTEE",
						"PRIVILEGE", "IS_GRANTABLE"},
				new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
						Types.VARCHAR, Types.VARCHAR});
	}

	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return emptyResultSet(
				new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "GRANTOR", "GRANTEE", "PRIVILEGE",
						"IS_GRANTABLE"},
				new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
						Types.VARCHAR});
	}

	@Override
	public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
		return emptyResultSet(
				new String[] {"SCOPE", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH",
						"DECIMAL_DIGITS", "PSEUDO_COLUMN"},
				new int[] {Types.SMALLINT, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER,
						Types.SMALLINT, Types.SMALLINT});
	}

	@Override
	public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
		return emptyResultSet(
				new String[] {"SCOPE", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH",
						"DECIMAL_DIGITS", "PSEUDO_COLUMN"},
				new int[] {Types.SMALLINT, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER,
						Types.SMALLINT, Types.SMALLINT});
	}

	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		String[] columnNames = new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "KEY_SEQ", "PK_NAME"};
		int[] columnTypes = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.SMALLINT, Types.VARCHAR};
		String currentSchema = currentGraphspace();
		if (connection == null || !matchesPattern(currentSchema, schema)) {
			return emptyResultSet(columnNames, columnTypes);
		}
		List<Object[]> rows = new ArrayList<Object[]>();
		for (TableDescriptor descriptor : tableDescriptors(null)) {
			if (table != null && !table.equals(descriptor.name)) {
				continue;
			}
			if ("TAG".equals(descriptor.type)) {
				rows.add(new Object[] {null, currentSchema, descriptor.name, "__vid", Short.valueOf((short) 1), "NEBULA_VERTEX_ID"});
			} else if ("EDGE".equals(descriptor.type)) {
				rows.add(new Object[] {null, currentSchema, descriptor.name, "__src", Short.valueOf((short) 1), "NEBULA_EDGE_ID"});
				rows.add(new Object[] {null, currentSchema, descriptor.name, "__dst", Short.valueOf((short) 2), "NEBULA_EDGE_ID"});
				rows.add(new Object[] {null, currentSchema, descriptor.name, "__rank", Short.valueOf((short) 3), "NEBULA_EDGE_ID"});
			}
		}
		return rowsResultSet(columnNames, columnTypes, rows.toArray(new Object[rows.size()][]));
	}

	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		return emptyResultSet(foreignKeyColumns(), foreignKeyTypes());
	}

	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		return emptyResultSet(foreignKeyColumns(), foreignKeyTypes());
	}

	@Override
	public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
		return emptyResultSet(foreignKeyColumns(), foreignKeyTypes());
	}

	@Override
	public ResultSet getTypeInfo() throws SQLException {
		return emptyResultSet(
				new String[] {"TYPE_NAME", "DATA_TYPE", "PRECISION", "LITERAL_PREFIX", "LITERAL_SUFFIX",
						"CREATE_PARAMS", "NULLABLE", "CASE_SENSITIVE", "SEARCHABLE", "UNSIGNED_ATTRIBUTE",
						"FIXED_PREC_SCALE", "AUTO_INCREMENT", "LOCAL_TYPE_NAME", "MINIMUM_SCALE", "MAXIMUM_SCALE",
						"SQL_DATA_TYPE", "SQL_DATETIME_SUB", "NUM_PREC_RADIX"},
				new int[] {Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.VARCHAR,
						Types.VARCHAR, Types.SMALLINT, Types.BOOLEAN, Types.SMALLINT, Types.BOOLEAN,
						Types.BOOLEAN, Types.BOOLEAN, Types.VARCHAR, Types.SMALLINT, Types.SMALLINT,
						Types.INTEGER, Types.INTEGER, Types.INTEGER});
	}

	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
		String[] columnNames = new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "NON_UNIQUE", "INDEX_QUALIFIER",
				"INDEX_NAME", "TYPE", "ORDINAL_POSITION", "COLUMN_NAME", "ASC_OR_DESC", "CARDINALITY",
				"PAGES", "FILTER_CONDITION"};
		int[] columnTypes = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BOOLEAN, Types.VARCHAR,
				Types.VARCHAR, Types.SMALLINT, Types.SMALLINT, Types.VARCHAR, Types.VARCHAR, Types.BIGINT,
				Types.BIGINT, Types.VARCHAR};
		String currentSchema = currentGraphspace();
		if (connection == null || !matchesPattern(currentSchema, schema) || unique) {
			return emptyResultSet(columnNames, columnTypes);
		}
		List<Object[]> rows = new ArrayList<Object[]>();
		for (IndexDescriptor descriptor : indexDescriptors("TAG", "SHOW TAG INDEXES")) {
			appendIndexRows(rows, currentSchema, table, descriptor);
		}
		for (IndexDescriptor descriptor : indexDescriptors("EDGE", "SHOW EDGE INDEXES")) {
			appendIndexRows(rows, currentSchema, table, descriptor);
		}
		return rowsResultSet(columnNames, columnTypes, rows.toArray(new Object[rows.size()][]));
	}

	@Override
	public boolean supportsResultSetType(int type) throws SQLException {
		return type == NebulaResultSet.SUPPORTED_TYPE;
	}

	@Override
	public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
		return type == NebulaResultSet.SUPPORTED_TYPE && concurrency == NebulaResultSet.SUPPORTED_CONCURRENCY;
	}

	@Override
	public boolean ownUpdatesAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean ownDeletesAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean ownInsertsAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean othersUpdatesAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean othersDeletesAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean othersInsertsAreVisible(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean updatesAreDetected(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean deletesAreDetected(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean insertsAreDetected(int type) throws SQLException {
		return false;
	}

	@Override
	public boolean supportsBatchUpdates() throws SQLException {
		return true;
	}

	@Override
	public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
		return emptyResultSet(
				new String[] {"TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "CLASS_NAME", "DATA_TYPE", "REMARKS",
						"BASE_TYPE"},
				new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR,
						Types.SMALLINT});
	}

	@Override
	public Connection getConnection() throws SQLException {
		return connection;
	}

	@Override
	public boolean supportsSavepoints() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsNamedParameters() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsMultipleOpenResults() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsGetGeneratedKeys() throws SQLException {
		return false;
	}

	@Override
	public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
		return emptyResultSet(
				new String[] {"TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SUPERTYPE_CAT", "SUPERTYPE_SCHEM",
						"SUPERTYPE_NAME"},
				new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
						Types.VARCHAR});
	}

	@Override
	public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return emptyResultSet(
				new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "SUPERTABLE_NAME"},
				new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR});
	}

	@Override
	public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
		return emptyResultSet(
				new String[] {"TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "ATTR_NAME", "DATA_TYPE", "ATTR_TYPE_NAME",
						"ATTR_SIZE", "DECIMAL_DIGITS", "NUM_PREC_RADIX", "NULLABLE", "REMARKS", "ATTR_DEF",
						"SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH", "ORDINAL_POSITION",
						"IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA", "SCOPE_TABLE", "SOURCE_DATA_TYPE"},
				new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR,
						Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.VARCHAR,
						Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.VARCHAR,
						Types.VARCHAR, Types.VARCHAR, Types.SMALLINT});
	}

	@Override
	public boolean supportsResultSetHoldability(int holdability) throws SQLException {
		return holdability == NebulaResultSet.SUPPORTED_HOLDABILITY;
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return NebulaResultSet.SUPPORTED_HOLDABILITY;
	}

	@Override
	public int getDatabaseMajorVersion() throws SQLException {
		return 0;
	}

	@Override
	public int getDatabaseMinorVersion() throws SQLException {
		return 0;
	}

	@Override
	public int getJDBCMajorVersion() throws SQLException {
		return 4;
	}

	@Override
	public int getJDBCMinorVersion() throws SQLException {
		return 2;
	}

	@Override
	public int getSQLStateType() throws SQLException {
		return DatabaseMetaData.sqlStateSQL;
	}

	@Override
	public boolean locatorsUpdateCopy() throws SQLException {
		return false;
	}

	@Override
	public boolean supportsStatementPooling() throws SQLException {
		return false;
	}

	@Override
	public RowIdLifetime getRowIdLifetime() throws SQLException {
		return RowIdLifetime.ROWID_UNSUPPORTED;
	}

	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		return getSchemas();
	}

	@Override
	public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
		return false;
	}

	@Override
	public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
		return false;
	}

	@Override
	public ResultSet getClientInfoProperties() throws SQLException {
		return emptyResultSet(
				new String[] {"NAME", "MAX_LEN", "DEFAULT_VALUE", "DESCRIPTION"},
				new int[] {Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR});
	}

	@Override
	public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
		return emptyResultSet(
				new String[] {"FUNCTION_CAT", "FUNCTION_SCHEM", "FUNCTION_NAME", "REMARKS", "FUNCTION_TYPE",
						"SPECIFIC_NAME"},
				new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.SMALLINT,
						Types.VARCHAR});
	}

	@Override
	public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
		return emptyResultSet(
				new String[] {"FUNCTION_CAT", "FUNCTION_SCHEM", "FUNCTION_NAME", "COLUMN_NAME", "COLUMN_TYPE",
						"DATA_TYPE", "TYPE_NAME", "PRECISION", "LENGTH", "SCALE", "RADIX", "NULLABLE", "REMARKS",
						"CHAR_OCTET_LENGTH", "ORDINAL_POSITION", "IS_NULLABLE", "SPECIFIC_NAME"},
				new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.SMALLINT,
						Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.SMALLINT, Types.SMALLINT,
						Types.SMALLINT, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.VARCHAR});
	}

	@Override
	public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		return emptyResultSet(
				new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "DATA_TYPE", "COLUMN_SIZE",
						"DECIMAL_DIGITS", "NUM_PREC_RADIX", "COLUMN_USAGE", "REMARKS", "CHAR_OCTET_LENGTH",
						"IS_NULLABLE"},
				new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER,
						Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR});
	}

	@Override
	public boolean generatedKeyAlwaysReturned() throws SQLException {
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return JdbcWrapperSupport.unwrap(this, iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return JdbcWrapperSupport.isWrapperFor(this, iface);
	}

	private ResultSet emptyResultSet(String[] columnNames, int[] columnTypes) throws SQLException {
		return rowsResultSet(columnNames, columnTypes, new Object[0][]);
	}

	private ResultSet singleRowResultSet(String[] columnNames, int[] columnTypes, Object[] row) throws SQLException {
		return rowsResultSet(columnNames, columnTypes, new Object[][] {row});
	}

	private ResultSet rowsResultSet(String[] columnNames, int[] columnTypes, Object[][] rows) throws SQLException {
		CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet();
		RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
		metaData.setColumnCount(columnNames.length);
		for (int i = 0; i < columnNames.length; i++) {
			metaData.setColumnName(i + 1, columnNames[i]);
			metaData.setColumnLabel(i + 1, columnNames[i]);
			metaData.setColumnType(i + 1, columnTypes[i]);
		}
		rowSet.setMetaData(metaData);
		for (int rowIndex = rows.length - 1; rowIndex >= 0; rowIndex--) {
			Object[] row = rows[rowIndex];
			rowSet.moveToInsertRow();
			for (int i = 0; i < columnNames.length; i++) {
				rowSet.updateObject(i + 1, i < row.length ? row[i] : null);
			}
			rowSet.insertRow();
			rowSet.moveToCurrentRow();
		}
		rowSet.beforeFirst();
		return rowSet;
	}

	private String currentGraphspace() throws SQLException {
		if (connection == null) {
			return null;
		}
		return connection.getSchema();
	}

	private List<TableDescriptor> tableDescriptors(String[] types) throws SQLException {
		List<TableDescriptor> tables = new ArrayList<TableDescriptor>();
		if (includeType(types, "TAG")) {
			for (String tag : schemaNames("SHOW TAGS")) {
				tables.add(new TableDescriptor(tag, "TAG"));
			}
		}
		if (includeType(types, "EDGE")) {
			for (String edge : schemaNames("SHOW EDGES")) {
				tables.add(new TableDescriptor(edge, "EDGE"));
			}
		}
		return tables;
	}

	private List<String> schemaNames(String command) throws SQLException {
		List<String> names = new ArrayList<String>();
		NebulaNativeResultSet resultSet = connection.execute(command);
		checkNativeResult(command, resultSet);
		if (resultSet == null || resultSet.isEmpty()) {
			return names;
		}
		for (NebulaNativeRecord record : rows(resultSet)) {
			String name = nativeString(record, 0);
			if (name != null && name.length() > 0) {
				names.add(name);
			}
		}
		return names;
	}

	private List<ColumnDescriptor> describe(TableDescriptor table) throws SQLException {
		String command = "DESCRIBE " + table.type + " " + quoteIdentifier(table.name);
		List<ColumnDescriptor> columns = new ArrayList<ColumnDescriptor>();
		NebulaNativeResultSet resultSet = connection.execute(command);
		checkNativeResult(command, resultSet);
		if (resultSet == null || resultSet.isEmpty()) {
			return columns;
		}
		for (NebulaNativeRecord record : rows(resultSet)) {
			String name = nativeString(record, 0);
			String typeName = nativeString(record, 1);
			String nullable = nativeString(record, 2);
			String defaultValue = nativeString(record, 3);
			String comment = nativeString(record, 4);
			if (name != null && name.length() > 0) {
				columns.add(new ColumnDescriptor(name, typeName, nullable(nullable), defaultValue, comment));
			}
		}
		return columns;
	}

	private List<IndexDescriptor> indexDescriptors(String type, String command) throws SQLException {
		List<IndexDescriptor> indexes = new ArrayList<IndexDescriptor>();
		NebulaNativeResultSet resultSet = connection.execute(command);
		checkNativeResult(command, resultSet);
		if (resultSet == null || resultSet.isEmpty()) {
			return indexes;
		}
		for (NebulaNativeRecord record : rows(resultSet)) {
			String indexName = nativeString(record, 0);
			String tableName = nativeString(record, 1);
			if (indexName != null && indexName.length() > 0 && tableName != null && tableName.length() > 0) {
				indexes.add(new IndexDescriptor(indexName, tableName, type));
			}
		}
		return indexes;
	}

	private void appendIndexRows(List<Object[]> rows, String schema, String table, IndexDescriptor descriptor) throws SQLException {
		if (table != null && !table.equals(descriptor.tableName)) {
			return;
		}
		List<ColumnDescriptor> columns = describeIndex(descriptor);
		for (int i = 0; i < columns.size(); i++) {
			rows.add(new Object[] {
					null, schema, descriptor.tableName, Boolean.TRUE, null, descriptor.indexName,
					Short.valueOf(DatabaseMetaData.tableIndexOther), Short.valueOf((short) (i + 1)),
					columns.get(i).name, null, null, null, null
			});
		}
	}

	private List<ColumnDescriptor> describeIndex(IndexDescriptor descriptor) throws SQLException {
		String command = "DESCRIBE " + descriptor.type + " INDEX " + quoteIdentifier(descriptor.indexName);
		List<ColumnDescriptor> columns = new ArrayList<ColumnDescriptor>();
		NebulaNativeResultSet resultSet = connection.execute(command);
		checkNativeResult(command, resultSet);
		if (resultSet == null || resultSet.isEmpty()) {
			return columns;
		}
		for (NebulaNativeRecord record : rows(resultSet)) {
			String name = nativeString(record, 0);
			String typeName = nativeString(record, 1);
			if (name != null && name.length() > 0) {
				columns.add(new ColumnDescriptor(name, typeName, DatabaseMetaData.columnNullableUnknown, null, null));
			}
		}
		return columns;
	}

	private void checkNativeResult(String command, NebulaNativeResultSet resultSet) throws SQLException {
		if (resultSet != null && !resultSet.isSucceeded()) {
			throw new SQLException("Nebula metadata command failed: " + command + ", error code: "
					+ resultSet.getErrorCode() + ", error message: " + resultSet.getErrorMessage());
		}
	}

	private List<NebulaNativeRecord> rows(NebulaNativeResultSet resultSet) throws SQLException {
		return resultSet.rows();
	}

	private String nativeString(NebulaNativeRecord record, int column) throws SQLException {
		try {
			if (record == null) {
				return null;
			}
			NebulaNativeValue value = record.get(column);
			if (value == null || value.isNull()) {
				return null;
			}
			return value.asString();
		} catch (Exception exception) {
			throw new SQLException("Failed to read Nebula metadata value.", exception);
		}
	}

	private boolean includeType(String[] types, String tableType) {
		if (types == null || types.length == 0) {
			return true;
		}
		for (String type : types) {
			if (type == null) {
				continue;
			}
			String normalized = type.toUpperCase(Locale.ROOT);
			if (tableType.equals(normalized) || "TABLE".equals(normalized) || "BASE TABLE".equals(normalized)) {
				return true;
			}
		}
		return false;
	}

	private boolean matchesPattern(String value, String pattern) {
		if (pattern == null || "%".equals(pattern)) {
			return true;
		}
		if (value == null) {
			return false;
		}
		return value.matches(toRegex(pattern));
	}

	private String toRegex(String pattern) {
		StringBuilder regex = new StringBuilder();
		for (int i = 0; i < pattern.length(); i++) {
			char c = pattern.charAt(i);
			if (c == '\\' && i + 1 < pattern.length()) {
				regex.append("\\Q").append(pattern.charAt(++i)).append("\\E");
			} else if (c == '%') {
				regex.append(".*");
			} else if (c == '_') {
				regex.append('.');
			} else {
				regex.append("\\Q").append(c).append("\\E");
			}
		}
		return regex.toString();
	}

	private String quoteIdentifier(String identifier) {
		return "`" + identifier.replace("`", "``") + "`";
	}

	private int jdbcType(String typeName) {
		String normalized = normalizeType(typeName);
		if ("BOOL".equals(normalized) || "BOOLEAN".equals(normalized)) {
			return Types.BOOLEAN;
		}
		if ("INT8".equals(normalized) || "TINYINT".equals(normalized)) {
			return Types.TINYINT;
		}
		if ("INT16".equals(normalized) || "SMALLINT".equals(normalized)) {
			return Types.SMALLINT;
		}
		if ("INT32".equals(normalized) || "INT".equals(normalized) || "INTEGER".equals(normalized)) {
			return Types.INTEGER;
		}
		if ("INT64".equals(normalized) || "BIGINT".equals(normalized) || "TIMESTAMP".equals(normalized)) {
			return Types.BIGINT;
		}
		if ("FLOAT".equals(normalized)) {
			return Types.FLOAT;
		}
		if ("DOUBLE".equals(normalized)) {
			return Types.DOUBLE;
		}
		if ("DATE".equals(normalized)) {
			return Types.DATE;
		}
		if ("TIME".equals(normalized)) {
			return Types.TIME;
		}
		if ("DATETIME".equals(normalized)) {
			return Types.TIMESTAMP;
		}
		if (normalized.startsWith("FIXED_STRING")) {
			return Types.CHAR;
		}
		if ("STRING".equals(normalized)) {
			return Types.VARCHAR;
		}
		return Types.JAVA_OBJECT;
	}

	private Integer columnSize(String typeName) {
		String normalized = normalizeType(typeName);
		if (normalized.startsWith("FIXED_STRING")) {
			int start = normalized.indexOf('(');
			int end = normalized.indexOf(')', start + 1);
			if (start >= 0 && end > start) {
				try {
					return Integer.valueOf(Integer.parseInt(normalized.substring(start + 1, end)));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		if ("BOOL".equals(normalized) || "BOOLEAN".equals(normalized)) {
			return Integer.valueOf(1);
		}
		if ("INT8".equals(normalized) || "TINYINT".equals(normalized)) {
			return Integer.valueOf(3);
		}
		if ("INT16".equals(normalized) || "SMALLINT".equals(normalized)) {
			return Integer.valueOf(5);
		}
		if ("INT32".equals(normalized) || "INT".equals(normalized) || "INTEGER".equals(normalized)) {
			return Integer.valueOf(10);
		}
		if ("INT64".equals(normalized) || "BIGINT".equals(normalized) || "TIMESTAMP".equals(normalized)) {
			return Integer.valueOf(19);
		}
		if ("FLOAT".equals(normalized)) {
			return Integer.valueOf(7);
		}
		if ("DOUBLE".equals(normalized)) {
			return Integer.valueOf(15);
		}
		return null;
	}

	private Integer decimalDigits(String typeName) {
		String normalized = normalizeType(typeName);
		if ("FLOAT".equals(normalized) || "DOUBLE".equals(normalized)) {
			return Integer.valueOf(0);
		}
		return null;
	}

	private Integer charOctetLength(int jdbcType, Integer size) {
		if ((jdbcType == Types.CHAR || jdbcType == Types.VARCHAR) && size != null) {
			return size;
		}
		return null;
	}

	private int nullable(String nullable) {
		if ("YES".equalsIgnoreCase(nullable)) {
			return DatabaseMetaData.columnNullable;
		}
		if ("NO".equalsIgnoreCase(nullable)) {
			return DatabaseMetaData.columnNoNulls;
		}
		return DatabaseMetaData.columnNullableUnknown;
	}

	private String nullableText(int nullable) {
		if (nullable == DatabaseMetaData.columnNullable) {
			return "YES";
		}
		if (nullable == DatabaseMetaData.columnNoNulls) {
			return "NO";
		}
		return "";
	}

	private String normalizeType(String typeName) {
		return typeName == null ? "" : typeName.trim().toUpperCase(Locale.ROOT);
	}

	private static class TableDescriptor {
		private final String name;
		private final String type;

		private TableDescriptor(String name, String type) {
			this.name = name;
			this.type = type;
		}
	}

	private static class ColumnDescriptor {
		private final String name;
		private final String typeName;
		private final int nullable;
		private final String defaultValue;
		private final String comment;

		private ColumnDescriptor(String name, String typeName, int nullable, String defaultValue, String comment) {
			this.name = name;
			this.typeName = typeName;
			this.nullable = nullable;
			this.defaultValue = defaultValue;
			this.comment = comment;
		}
	}

	private static class IndexDescriptor {
		private final String indexName;
		private final String tableName;
		private final String type;

		private IndexDescriptor(String indexName, String tableName, String type) {
			this.indexName = indexName;
			this.tableName = tableName;
			this.type = type;
		}
	}

	private String[] foreignKeyColumns() {
		return new String[] {"PKTABLE_CAT", "PKTABLE_SCHEM", "PKTABLE_NAME", "PKCOLUMN_NAME", "FKTABLE_CAT",
				"FKTABLE_SCHEM", "FKTABLE_NAME", "FKCOLUMN_NAME", "KEY_SEQ", "UPDATE_RULE", "DELETE_RULE",
				"FK_NAME", "PK_NAME", "DEFERRABILITY"};
	}

	private int[] foreignKeyTypes() {
		return new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
				Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.SMALLINT, Types.SMALLINT, Types.SMALLINT,
				Types.VARCHAR, Types.VARCHAR, Types.SMALLINT};
	}
}
