## ADDED Requirements

### Requirement: Deterministic JDBC URL and property parsing
`fw-plus-nebula-jdbc` SHALL parse JDBC URL and connection properties into a single effective configuration before creating a Nebula connection. Required fields, graph space, host list, credentials, and connection pool numeric options MUST be validated before use, and invalid input MUST fail with a clear `SQLException` or documented parse exception.

#### Scenario: Valid URL and properties produce effective configuration
- **WHEN** a caller provides a valid `jdbc:nebula://.../<space>` URL and supported connection properties
- **THEN** the driver uses the parsed effective configuration consistently for graph space, credentials, host addresses, and pool options

#### Scenario: Invalid URL fails clearly
- **WHEN** a caller provides a null, malformed, or unsupported JDBC URL
- **THEN** the driver rejects the input with a clear SQL-level exception and does not create a connection pool

#### Scenario: Invalid numeric pool option fails clearly
- **WHEN** a caller provides a non-numeric or out-of-range value for a numeric pool option
- **THEN** parsing or connection initialization fails before the value is applied to `NebulaPoolConfig`

### Requirement: Connection lifecycle is deterministic
`NebulaConnection` SHALL manage Nebula pool and session resources deterministically. A connection close operation MUST be idempotent, initialization failures MUST release resources that were already created, and supported operations invoked after close MUST fail with `SQLException`.

#### Scenario: Close is idempotent
- **WHEN** a caller invokes `close()` multiple times on the same connection
- **THEN** the connection remains closed and no additional failure is raised solely because close was repeated

#### Scenario: Initialization failure cleans up created resources
- **WHEN** pool initialization succeeds but session acquisition or graph space selection fails
- **THEN** the connection releases any created session or pool resources before reporting the failure

#### Scenario: Operation after close fails
- **WHEN** a caller invokes query execution or statement creation after the connection is closed
- **THEN** the operation fails with `SQLException` indicating the connection is closed

### Requirement: Statement execution respects JDBC state boundaries
`NebulaStatement` and `NebulaPreparedStatement` SHALL validate closed state, SQL text, and query/update method expectations before or immediately after execution. Query methods MUST not silently accept update-only statements, update methods MUST not silently accept query-only statements, and callers MUST be able to retrieve the current result only after a successful result-producing execution.

#### Scenario: Query method rejects update statement
- **WHEN** a caller executes an update-only nGQL statement through a query method
- **THEN** the statement fails with a clear `SQLException`

#### Scenario: Update method rejects query statement
- **WHEN** a caller executes a query-only nGQL statement through an update method
- **THEN** the statement fails with a clear `SQLException`

#### Scenario: Closed statement rejects execution
- **WHEN** a caller invokes execute, executeQuery, or executeUpdate after closing a statement
- **THEN** the statement fails with `SQLException` indicating it is closed

### Requirement: ResultSet and metadata validate cursor and column boundaries
`NebulaResultSet` and `NebulaResultSetMetaData` SHALL validate cursor state, closed state, and one-based column indexes for supported accessors. Empty results MUST be represented consistently, and invalid column indexes MUST fail with `SQLException`.

#### Scenario: Invalid metadata column index fails
- **WHEN** a caller requests metadata for column index 0 or a value greater than the column count
- **THEN** the metadata call fails with `SQLException`

#### Scenario: Empty result has zero columns when no column metadata exists
- **WHEN** a Nebula result contains no rows and no column names
- **THEN** metadata column count returns zero without throwing an unrelated runtime exception

#### Scenario: Closed result set rejects supported reads
- **WHEN** a caller closes a result set and then invokes a supported read operation
- **THEN** the read operation fails with `SQLException` indicating the result set is closed

### Requirement: Unsupported JDBC methods fail explicitly
JDBC methods that are not implemented by `fw-plus-nebula-jdbc` SHALL fail with `SQLFeatureNotSupportedException` instead of silently succeeding or returning misleading placeholder values, unless the method is intentionally implemented with documented behavior.

#### Scenario: Unsupported connection feature fails explicitly
- **WHEN** a caller invokes an unsupported JDBC feature such as savepoints or callable statements
- **THEN** the method fails with `SQLFeatureNotSupportedException`

#### Scenario: Supported simple state method returns documented value
- **WHEN** a caller invokes a simple state method that the driver intentionally supports
- **THEN** the method returns a stable documented value consistent with the current connection or statement state
