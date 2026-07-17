/* Copyright (c) 2020 vesoft inc. All rights reserved.
 *
 * This source code is licensed under Apache 2.0 License.
 */

package com.vesoft.nebula.jdbc.statement;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.NebulaParameterMetaData;

import com.vesoft.nebula.jdbc.sql.NebulaParameterBindings;
import com.vesoft.nebula.jdbc.sql.NebulaSqlExecutor;
import com.vesoft.nebula.jdbc.utils.ExceptionBuilder;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NebulaPreparedStatementImpl extends NebulaStatementImpl implements NebulaPreparedStatement {

    protected static final Pattern NAMED_PARAMETER_REGEX = Pattern.compile("\\?(?=[^\"]*(?:\"[^\"]*\"[^\"]*)*$)");
    protected String                  rawNGQL;
    protected String                  nGql;
    protected Map<Object, Object> parameters;
    protected NebulaParameterBindings parameterBindings;
    protected int                     parametersNumber;

    public NebulaPreparedStatementImpl(NebulaConnection connection, String rawNGQL) {
        this(connection, rawNGQL, null);
    }

    public NebulaPreparedStatementImpl(NebulaConnection connection, String rawNGQL, NebulaSqlExecutor sqlExecutor) {
        super(connection, sqlExecutor);
       this.rawNGQL = rawNGQL;
       this.parametersNumber = namedParameterCount(rawNGQL);
       this.parameters = new HashMap<>();
       this.parameterBindings = new NebulaParameterBindings(this.parametersNumber);
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        this.checkClosed();
        this.parameterBindings.validateComplete();
        this.currentResultSet = this.sqlExecutor.executeQuery(this.nebulaConnection, this.rawNGQL, this.parameterBindings.copy());
        return currentResultSet;
    }


    /** This method will not return the number of data which influenced by the executed ngql.
     *  If the user inserts multiple points or edges in a single insert statement, some of them may succeed,
     *  but the server will only return to tell the user that it has failed, however, the user may actually be able to find out some of the data have inserted.
     */
    @Override
    public int executeUpdate() throws SQLException {
        this.checkClosed();
        this.parameterBindings.validateComplete();
        return this.sqlExecutor.executeUpdate(this.nebulaConnection, this.rawNGQL, this.parameterBindings.copy());
    }

    @Override
    public boolean execute() throws SQLException {
        this.checkClosed();
        this.parameterBindings.validateComplete();
        if (isQuery(this.rawNGQL)) {
            this.currentResultSet = this.sqlExecutor.executeQuery(
                    this.nebulaConnection,
                    this.rawNGQL,
                    this.parameterBindings.copy()
            );
            return true;
        }
        this.sqlExecutor.executeUpdate(this.nebulaConnection, this.rawNGQL, this.parameterBindings.copy());
        this.currentResultSet = null;
        return false;
    }

    protected String replacePlaceHolderWithParam(String rawNGQL) throws SQLException {
        Integer index = 1;
        String digested = rawNGQL;

        Matcher matcher = NAMED_PARAMETER_REGEX.matcher(digested);

        while (matcher.find()) {
            if(!parameters.containsKey(index)){
                throw new SQLException(String.format("Can not get param in index [%d], please check your nGql.", index));
            }
            Object param = parameters.get(index);

            if(param != null) {
                String paramTypeName = param.getClass().getTypeName();
                switch (paramTypeName) {
                    case ("java.lang.String"):
                        param = String.format("\"%s\"", param);
                        break;
                    case ("java.sql.Date"):
                        param = String.format("date(\"%s\")", param);
                        break;
                    case ("java.sql.Time"):
                        param = String.format("time(\"%s\")", param);
                        break;
                    case ("java.util.Date"):
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
                        String datetimeString = formatter.format(param);
                        param = String.format("datetime(\"%s\")", datetimeString);
                        break;
                    default:
                        break;
                }
            }

            digested = NAMED_PARAMETER_REGEX.matcher(digested).replaceFirst(Objects.toString(param));
            index++;
        }

        return digested;
    }

    /**  set methods  */

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        insertParameter(parameterIndex, x);
        this.parameterBindings.bind(parameterIndex, x, Types.BOOLEAN);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        insertParameter(parameterIndex, x);
        this.parameterBindings.bind(parameterIndex, x, Types.INTEGER);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        insertParameter(parameterIndex, x);
        this.parameterBindings.bind(parameterIndex, x, Types.DOUBLE);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        insertParameter(parameterIndex, x);
        this.parameterBindings.bind(parameterIndex, x, Types.VARCHAR);
    }

    @Override
    public void setDate(int parameterIndex, Date date) throws SQLException {
        insertParameter(parameterIndex, date);
        this.parameterBindings.bind(parameterIndex, date, Types.DATE);
    }

    @Override
    public void setTime(int parameterIndex, Time time) throws SQLException {
        insertParameter(parameterIndex, time);
        this.parameterBindings.bind(parameterIndex, time, Types.TIME);
    }

    public void setDatetime(int parameterIndex, java.util.Date datetime) throws SQLException {
        insertParameter(parameterIndex, datetime);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return NebulaParameterMetaData.getInstance(this);
    }


    @Override
    public void checkParamsNumber(int parameterIndex) throws SQLException {
        if (parameterIndex < 1 || parameterIndex > this.parametersNumber) {
            throw new SQLException("ParameterIndex does not correspond to a parameter marker in the nGQL statement.");
        }
    }

    protected int namedParameterCount(String rawNGQL) {
        if (rawNGQL == null || rawNGQL.isEmpty()) {
            return 0;
        }
        int count = 0;
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean backtickQuoted = false;
        boolean lineComment = false;
        boolean blockComment = false;
        for (int i = 0; i < rawNGQL.length(); i++) {
            char current = rawNGQL.charAt(i);
            char next = i + 1 < rawNGQL.length() ? rawNGQL.charAt(i + 1) : '\0';

            if (lineComment) {
                if (current == '\n' || current == '\r') {
                    lineComment = false;
                }
                continue;
            }
            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    i++;
                }
                continue;
            }
            if (singleQuoted) {
                if (current == '\\' && next != '\0') {
                    i++;
                } else if (current == '\'' && next == '\'') {
                    i++;
                } else if (current == '\'') {
                    singleQuoted = false;
                }
                continue;
            }
            if (doubleQuoted) {
                if (current == '\\' && next != '\0') {
                    i++;
                } else if (current == '"' && next == '"') {
                    i++;
                } else if (current == '"') {
                    doubleQuoted = false;
                }
                continue;
            }
            if (backtickQuoted) {
                if (current == '`') {
                    backtickQuoted = false;
                }
                continue;
            }

            if (current == '-' && next == '-') {
                lineComment = true;
                i++;
            } else if (current == '/' && next == '*') {
                blockComment = true;
                i++;
            } else if (current == '\'') {
                singleQuoted = true;
            } else if (current == '"') {
                doubleQuoted = true;
            } else if (current == '`') {
                backtickQuoted = true;
            } else if (current == '?') {
                count++;
            }
        }
        return count;
    }

    @Override
    public void insertParameter(int parameterIndex, Object obj) throws SQLException {
        this.checkClosed();
        this.checkParamsNumber(parameterIndex);
        this.parameters.put(parameterIndex, obj);
    }


    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return currentResultSet != null ? currentResultSet.getMetaData() : null;
    }

    @Override
    public void clearParameters() throws SQLException {
        checkClosed();
        this.parameters.clear();
        this.parameterBindings = new NebulaParameterBindings(this.parametersNumber);
    }

    public Map<Object, Object> getParameters() {
        return parameters;
    }

    public int getParametersNumber() {
        return parametersNumber;
    }

    public NebulaParameterBindings getParameterBindings() {
        return parameterBindings;
    }

    @Override public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLException("Method execute(String, int) cannot be called on PreparedStatement.");
    }

    @Override public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLException("Method execute(String, int[]) cannot be called on PreparedStatement.");
    }

    @Override public boolean execute(String sql, String[] columnNames) throws SQLException {
        throw new SQLException("Method execute(String, String[]) cannot be called on PreparedStatement.");
    }

    @Override public ResultSet executeQuery(String sql) throws SQLException {
        throw new SQLException("Method executeQuery(String) cannot be called on PreparedStatement.");
    }

    @Override public int executeUpdate(String sql) throws SQLException {
        throw new SQLException("Method executeUpdate(String) cannot be called on PreparedStatement.");
    }

    @Override public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLException("Method executeUpdate(String, int) cannot be called on PreparedStatement.");
    }

    @Override public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLException("Method executeUpdate(String, int[]) cannot be called on PreparedStatement.");
    }

    @Override public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        throw new SQLException("Method executeUpdate(String, String[]) cannot be called on PreparedStatement.");
    }

    /**
     * -----------------------Not implement yet-------------------------
     *
     * */

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        insertParameter(parameterIndex, x);
        this.parameterBindings.bind(parameterIndex, x, Types.TINYINT);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        insertParameter(parameterIndex, x);
        this.parameterBindings.bind(parameterIndex, x, Types.SMALLINT);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        insertParameter(parameterIndex, x);
        this.parameterBindings.bind(parameterIndex, x, Types.BIGINT);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        insertParameter(parameterIndex, x);
        this.parameterBindings.bind(parameterIndex, x, Types.FLOAT);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        insertParameter(parameterIndex, x);
        this.parameterBindings.bind(parameterIndex, x, Types.TIMESTAMP);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        insertParameter(parameterIndex, x);
        this.parameterBindings.bind(parameterIndex, x, targetSqlType);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        checkClosed();
        if (x == null) {
            setNull(parameterIndex, Types.OTHER);
        } else if (x instanceof SQLXML) {
            setSQLXML(parameterIndex, (SQLXML) x);
        } else if (x instanceof String) {
            setString(parameterIndex, (String) x);
        } else if (x instanceof BigDecimal) {
            insertParameter(parameterIndex, x);
            this.parameterBindings.bind(parameterIndex, x, Types.DECIMAL);
        } else if (x instanceof Short) {
            insertParameter(parameterIndex, x);
            this.parameterBindings.bind(parameterIndex, x, Types.SMALLINT);
        } else if (x instanceof Integer) {
            setInt(parameterIndex, (Integer) x);
        } else if (x instanceof Long) {
            setLong(parameterIndex, (Long) x);
        } else if (x instanceof Float) {
            insertParameter(parameterIndex, x);
            this.parameterBindings.bind(parameterIndex, x, Types.FLOAT);
        } else if (x instanceof Double) {
            setDouble(parameterIndex, (Double) x);
        } else if (x instanceof byte[]) {
            setBytes(parameterIndex, (byte[]) x);
        } else if (x instanceof Date) {
            setDate(parameterIndex, (Date) x);
        } else if (x instanceof Time) {
            setTime(parameterIndex, (Time) x);
        } else if (x instanceof Timestamp) {
            setTimestamp(parameterIndex, (Timestamp) x);
        } else if (x instanceof Boolean) {
            setBoolean(parameterIndex, (Boolean) x);
        } else if (x instanceof Byte) {
            setByte(parameterIndex, (Byte) x);
        } else if (x instanceof Blob) {
            setBlob(parameterIndex, (Blob) x);
        } else if (x instanceof Clob) {
            setClob(parameterIndex, (Clob) x);
        } else if (x instanceof Array) {
            setArray(parameterIndex, (Array) x);
        } else if (x instanceof Character) {
            setString(parameterIndex, ((Character) x).toString());
        } else if (x instanceof LocalDate) {
            setDate(parameterIndex, Date.valueOf((LocalDate) x));
        } else if (x instanceof java.time.LocalTime) {
            setTime(parameterIndex, Time.valueOf((java.time.LocalTime) x));
        } else if (x instanceof java.time.LocalDateTime) {
            setTimestamp(parameterIndex, Timestamp.valueOf((java.time.LocalDateTime) x));
        } else if (x instanceof OffsetDateTime) {
            setObject(parameterIndex, ((OffsetDateTime) x).toLocalDateTime());
        } else {
            insertParameter(parameterIndex, x);
            this.parameterBindings.bind(parameterIndex, x, Types.JAVA_OBJECT);
        }
    }

    @Override
    public void addBatch() throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        insertParameter(parameterIndex, x);
        this.parameterBindings.bind(parameterIndex, x, Types.DECIMAL);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        insertParameter(parameterIndex, null);
        this.parameterBindings.bind(parameterIndex, null, sqlType);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        insertParameter(parameterIndex, x);
        this.parameterBindings.bind(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setObject(int parameterIndex, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setObject(int parameterIndex, Object x, SQLType targetSqlType) throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public long executeLargeUpdate() throws SQLException {
        throw  ExceptionBuilder.buildUnsupportedOperationException();
    }

}
