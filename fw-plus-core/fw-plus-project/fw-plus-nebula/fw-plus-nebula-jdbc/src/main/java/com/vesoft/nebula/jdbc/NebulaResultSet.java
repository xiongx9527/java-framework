/* Copyright (c) 2020 vesoft inc. All rights reserved.
 *
 * This source code is licensed under Apache 2.0 License.
 */

package com.vesoft.nebula.jdbc;

import com.vesoft.nebula.jdbc.client.NebulaNativeRecord;
import com.vesoft.nebula.jdbc.client.NebulaNativeResultSet;
import com.vesoft.nebula.jdbc.client.NebulaNativeValue;
import com.vesoft.nebula.jdbc.lifecycle.NebulaResourceLifecycle;
import com.vesoft.nebula.jdbc.result.NebulaResultSetCursor;
import com.vesoft.nebula.jdbc.utils.ExceptionBuilder;
import com.vesoft.nebula.jdbc.utils.JdbcWrapperSupport;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NebulaResultSet implements java.sql.ResultSet {

    public static final int SUPPORTED_TYPE = TYPE_SCROLL_INSENSITIVE;
    public static final int SUPPORTED_CONCURRENCY = CONCUR_READ_ONLY;
    public static final int SUPPORTED_HOLDABILITY = CLOSE_CURSORS_AT_COMMIT;

    protected NebulaNativeResultSet nativeNebulaResultSet;

    protected final NebulaResourceLifecycle lifecycle = new NebulaResourceLifecycle();
    protected NebulaNativeValue lastColumnAccess;
    private int resultRowSize;
    private List<String> columnNames;
    private List<NebulaNativeRecord> rows;
    private NebulaNativeRecord currentRow;
    private Statement statement;
    private NebulaResultSetCursor cursor;
    private int fetchDirection = FETCH_FORWARD;
    private int fetchSize;

    public NebulaResultSet(NebulaNativeResultSet nebulaResultSet, Statement statement) {
        this.nativeNebulaResultSet = nebulaResultSet;
        this.statement = statement;
        this.columnNames = copyColumnNames(nativeNebulaResultSet);
        this.rows = materializeRows(nativeNebulaResultSet);
        this.resultRowSize = this.rows.size();
        this.cursor = new NebulaResultSetCursor(this.resultRowSize);
    }

    public Object getNativeNebulaResultSet() {
        if (nativeNebulaResultSet == null) {
            return null;
        }
        Object unwrapped = nativeNebulaResultSet.unwrap();
        return unwrapped == null ? nativeNebulaResultSet : unwrapped;
    }

    public NebulaNativeResultSet getNativeResultSet() {
        return nativeNebulaResultSet;
    }

    public List<NebulaNativeRecord> getRows() {
        return Collections.unmodifiableList(rows);
    }

    private static List<String> copyColumnNames(NebulaNativeResultSet resultSet) {
        try {
            if (resultSet == null || resultSet.getColumnNames() == null) {
                return new ArrayList<String>();
            }
            return new ArrayList<String>(resultSet.getColumnNames());
        } catch (SQLException exception) {
            return new ArrayList<String>();
        }
    }

    private static List<NebulaNativeRecord> materializeRows(NebulaNativeResultSet resultSet) {
        try {
            if (resultSet == null || resultSet.isEmpty()) {
                return new ArrayList<NebulaNativeRecord>();
            }
            return resultSet.rows();
        } catch (SQLException exception) {
            return new ArrayList<NebulaNativeRecord>();
        }
    }

    private static List<NebulaNativeRecord> copyRows(List<NebulaNativeRecord> rows) {
        if (rows == null) {
            return new ArrayList<NebulaNativeRecord>();
        }
        return new ArrayList<NebulaNativeRecord>(rows);
    }

    private boolean nativeType(NebulaNativeValue valueWrapper, String typeName) {
        if (valueWrapper == null) {
            return false;
        }
        if ("date".equals(typeName)) {
            return valueWrapper.isDate();
        }
        if ("time".equals(typeName)) {
            return valueWrapper.isTime();
        }
        if ("datetime".equals(typeName)) {
            return valueWrapper.isDateTime();
        }
        if ("double".equals(typeName)) {
            return valueWrapper.isDouble();
        }
        if ("float".equals(typeName)) {
            return valueWrapper.isFloat();
        }
        if ("long".equals(typeName)) {
            return valueWrapper.isLong();
        }
        if ("int".equals(typeName)) {
            return valueWrapper.isInt();
        }
        return false;
    }

    private Object rawValue(NebulaNativeValue valueWrapper) {
        return valueWrapper == null ? null : valueWrapper.rawValue();
    }

    private Object unwrapNestedValue(Object value) throws SQLException {
        if (value instanceof NebulaNativeValue) {
            return unwrapValue((NebulaNativeValue) value);
        }
        return value;
    }

    private List unwrapNestedList(List values) throws SQLException {
        if (values == null) {
            return null;
        }
        List unwrapped = new ArrayList();
        for (Object value : values) {
            unwrapped.add(unwrapNestedValue(value));
        }
        return unwrapped;
    }

    public void limitRows(int maxRows) throws SQLException {
        this.checkClosed();
        if (maxRows < 0) {
            throw new SQLException("Max rows must be >= 0.");
        }
        if (maxRows > 0 && maxRows < resultRowSize) {
            resultRowSize = maxRows;
            rows = new ArrayList<NebulaNativeRecord>(rows.subList(0, maxRows));
            cursor = new NebulaResultSetCursor(resultRowSize);
            currentRow = null;
            lastColumnAccess = null;
        }
    }


    public String getErrorMessage(){
        try {
            return nativeNebulaResultSet == null ? null : nativeNebulaResultSet.getErrorMessage();
        } catch (SQLException exception) {
            return null;
        }
    }

    @Override
    public void close() throws SQLException {
        lifecycle.close(new NebulaResourceLifecycle.ThrowingCloseAction() {
            @Override
            public void close() throws SQLException {
                currentRow = null;
                lastColumnAccess = null;
            }
        });
    }

    public void checkClosed() throws SQLException {
        lifecycle.checkOpen("ResultSet");

    }

    /** Some methods to move the cursor in ResultSet */
    @Override
    public boolean next() throws SQLException {
        this.checkClosed();
        boolean hasNext = cursor.next();
        if (!hasNext) {
            currentRow = null;
        }
        return hasNext;
    }

    @Override
    public boolean previous() throws SQLException {
        this.checkClosed();
        boolean hasPrevious = cursor.previous();
        if (!hasPrevious) {
            currentRow = null;
        }
        return hasPrevious;
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        this.checkClosed();
        return cursor.isBeforeFirst();
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        this.checkClosed();
        return cursor.isAfterLast();
    }

    @Override
    public boolean isFirst() throws SQLException {
        this.checkClosed();
        return cursor.isFirst();
    }

    @Override
    public boolean isLast() throws SQLException {
        this.checkClosed();
        return cursor.isLast();
    }

    @Override
    public void beforeFirst() throws SQLException {
        this.checkClosed();
        cursor.beforeFirst();
        currentRow = null;
    }

    @Override
    public void afterLast() throws SQLException {
        this.checkClosed();
        cursor.afterLast();
        currentRow = null;
    }

    @Override
    public boolean first() throws SQLException {
        this.checkClosed();
        return cursor.first();
    }

    @Override
    public boolean last() throws SQLException {
        this.checkClosed();
        return cursor.last();
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        this.checkClosed();
        boolean onRow = cursor.absolute(row);
        if (!onRow) {
            currentRow = null;
        }
        return onRow;
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        this.checkClosed();
        boolean onRow = cursor.relative(rows);
        if (!onRow) {
            currentRow = null;
        }
        return onRow;
    }



    @Override
    public int getRow() throws SQLException {
        this.checkClosed();
        return cursor.getRow();
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        this.checkClosed();
        int index = 0;
        for(int i = 0; i < columnNames.size(); ++i){
            if(columnLabel.equals(columnNames.get(i))){
                index = i + 1;
            }
        }
        if(index <= 0){
            throw new SQLException(String.format("No such column [%s] found, please check your parameters.", columnLabel));
        }
        return index;
    }

    public void checkIndex(int index) throws SQLException {
        if(index <= 0 || index > columnNames.size()){
            throw new SQLException(String.format("column index [%d] is invalid, please check your parameters (the first one should be represent as 1 instead of 0).", index));
        }
    }

    public void checkNullValue() throws SQLException {
        if(this.wasNull()){
            throw new SQLException("The current value you accessing is __NULL__, " +
                    "please check your nGql");
        }
    }

    public void checkResultSetCursor() throws SQLException {
        cursor.checkReadable();
    }

    @Override
    public boolean wasNull() throws SQLException {
        this.checkClosed();
        return cursor.wasNull();
    }

    /** some getter methods */

    private NebulaNativeValue getValueWrapper(int columnIndex) throws SQLException {
        this.checkClosed();
        cursor.checkReadable();
        checkIndex(columnIndex);
        currentRow = rows.get(cursor.getCurrentRowNumber());
        NebulaNativeValue result = currentRow.get(columnIndex - 1);
        cursor.recordColumnAccess(result);
        this.lastColumnAccess = result;
        return result;
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        return unwrapValue(getValueWrapper(columnIndex));
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return getObject(findColumn(columnLabel));
    }

    private Object unwrapValue(NebulaNativeValue valueWrapper) throws SQLException {
        if (valueWrapper == null || valueWrapper.isNull()) {
            return null;
        }
        try {
            if (nativeType(valueWrapper, "date")) {
                return valueWrapper.asDate();
            }
            if (nativeType(valueWrapper, "time")) {
                return valueWrapper.asTime();
            }
            if (nativeType(valueWrapper, "datetime")) {
                return valueWrapper.asTimestamp();
            }
            String value = valueWrapper.asString();
            if (value != null) {
                return value;
            }
        } catch (Exception ignored) {
        }
        try {
            List list = valueWrapper.asList();
            if (list != null) {
                return list;
            }
        } catch (Exception ignored) {
        }
        try {
            return valueWrapper.asLong();
        } catch (Exception ignored) {
        }
        try {
            return valueWrapper.asBoolean();
        } catch (Exception ignored) {
        }
        try {
            return numericValue(valueWrapper);
        } catch (Exception ignored) {
        }
        return rawValue(valueWrapper);
    }

    private double numericValue(NebulaNativeValue valueWrapper) throws SQLException {
        try {
            if (nativeType(valueWrapper, "double")) {
                return valueWrapper.asDouble();
            }
            if (nativeType(valueWrapper, "float")) {
                return valueWrapper.asFloat();
            }
            if (nativeType(valueWrapper, "long")) {
                return valueWrapper.asLong();
            }
            if (nativeType(valueWrapper, "int")) {
                return valueWrapper.asInt();
            }
            Object value = rawValue(valueWrapper);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception exception) {
            throw new SQLException(exception);
        }
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        NebulaNativeValue valueWrapper = getValueWrapper(columnIndex);
        if (this.wasNull()) {
            return null;
        }
        return valueWrapper.asString();
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        return getString(findColumn(columnLabel));
    }


    @Override
    public int getInt(int columnIndex) throws SQLException {
        NebulaNativeValue valueWrapper = getValueWrapper(columnIndex);
        if (this.wasNull()) {
            return 0;
        }
        return new Long(valueWrapper.asLong()).intValue();
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        return getInt(findColumn(columnLabel));
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        NebulaNativeValue valueWrapper = getValueWrapper(columnIndex);
        if (this.wasNull()) {
            return 0L;
        }
        return valueWrapper.asLong();
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        return getLong(findColumn(columnLabel));
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        NebulaNativeValue valueWrapper = getValueWrapper(columnIndex);
        if (this.wasNull()) {
            return false;
        }
        return valueWrapper.asBoolean();
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        return getBoolean(findColumn(columnLabel));
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        NebulaNativeValue valueWrapper = getValueWrapper(columnIndex);
        if (this.wasNull()) {
            return 0.0d;
        }
        return numericValue(valueWrapper);
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        return getDouble(findColumn(columnLabel));
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        NebulaNativeValue valueWrapper = getValueWrapper(columnIndex);
        if (this.wasNull()) {
            return null;
        }
        return getDateFromValueWrapper(valueWrapper);
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        return getDate(findColumn(columnLabel));
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        NebulaNativeValue valueWrapper = getValueWrapper(columnIndex);
        if (this.wasNull()) {
            return null;
        }
        return getTimeFromValueWrapper(valueWrapper);
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        return getTime(findColumn(columnLabel));
    }

    public LocalDateTime getDateTime(int columnIndex) throws SQLException {
        NebulaNativeValue valueWrapper = getValueWrapper(columnIndex);
        if (this.wasNull()) {
            return null;
        }
        return localDateTime(valueWrapper);
    }

    private Date getDateFromValueWrapper(NebulaNativeValue valueWrapper) throws SQLException {
        return valueWrapper.asDate();
    }

    private Time getTimeFromValueWrapper(NebulaNativeValue valueWrapper) throws SQLException {
        return valueWrapper.asTime();
    }

    private Timestamp getTimestampFromValueWrapper(NebulaNativeValue valueWrapper) throws SQLException {
        return valueWrapper.asTimestamp();
    }

    private LocalDateTime localDateTime(NebulaNativeValue valueWrapper) throws SQLException {
        Timestamp timestamp = valueWrapper.asTimestamp();
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    public LocalDateTime getDateTime(String columnLabel) throws SQLException {
        return getDateTime(findColumn(columnLabel));
    }

    public Object getNode(int columnIndex) throws SQLException {
        NebulaNativeValue valueWrapper = getValueWrapper(columnIndex);
        if (this.wasNull()) {
            return null;
        }
        return valueWrapper.asNode();
    }

    public Object getNode(String columnLabel) throws SQLException {
        return getNode(findColumn(columnLabel));
    }

    public Object getEdge(int columnIndex) throws SQLException {
        NebulaNativeValue valueWrapper = getValueWrapper(columnIndex);
        if (this.wasNull()) {
            return null;
        }
        return valueWrapper.asEdge();
    }

    public Object getEdge(String columnLabel) throws SQLException {
        return getEdge(findColumn(columnLabel));
    }

    public Object getPath(int columnIndex) throws SQLException {
        NebulaNativeValue valueWrapper = getValueWrapper(columnIndex);
        if (this.wasNull()) {
            return null;
        }
        return valueWrapper.asPath();
    }

    public Object getPath(String columnLabel) throws SQLException {
        return getPath(findColumn(columnLabel));
    }

    public List getList(int columnIndex) throws SQLException {
        NebulaNativeValue valueWrapper = getValueWrapper(columnIndex);
        if (this.wasNull()) {
            return null;
        }
        return valueWrapper.asList();
    }

    public List getList(String columnLabel) throws SQLException {
        return getList(findColumn(columnLabel));
    }

    public Set getSet(int columnIndex) throws SQLException {
        NebulaNativeValue valueWrapper = getValueWrapper(columnIndex);
        if (this.wasNull()) {
            return null;
        }
        return valueWrapper.asSet();
    }

    public Set getSet(String columnLabel) throws SQLException {
        return getSet(findColumn(columnLabel));
    }

    public Map getMap(int columnIndex) throws SQLException {
        NebulaNativeValue valueWrapper = getValueWrapper(columnIndex);
        if (this.wasNull()) {
            return null;
        }
        return valueWrapper.asMap();
    }

    public Map getMap(String columnLabel) throws SQLException {
        return getMap(findColumn(columnLabel));
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        this.checkClosed();
        return NebulaResultSetMetaData.getInstance(this);
    }

    @Override
    public Statement getStatement() throws SQLException {
        this.checkClosed();
        return statement;
    }



    public List<String> getColumnNames() {
        return columnNames;
    }

    @Override
    public int getType() throws SQLException {
        this.checkClosed();
        return SUPPORTED_TYPE;
    }

    @Override
    public int getConcurrency() throws SQLException {
        this.checkClosed();
        return SUPPORTED_CONCURRENCY;
    }

    @Override
    public int getHoldability() throws SQLException {
        this.checkClosed();
        return SUPPORTED_HOLDABILITY;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return lifecycle.isClosed();
    }

    /**
     * -----------------------Not implement yet-------------------------
     *
     * */

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        byte[] value = getBytes(columnIndex);
        return value == null ? null : new NebulaRowId(value);
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        return getRowId(findColumn(columnLabel));
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).byteValue();
        }
        try {
            return Byte.parseByte(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw conversionFailure(value, "byte", exception);
        }
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).shortValue();
        }
        try {
            return Short.parseShort(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw conversionFailure(value, "short", exception);
        }
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        if (value == null) {
            return 0.0f;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return Float.parseFloat(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw conversionFailure(value, "float", exception);
        }
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        BigDecimal value = getBigDecimal(columnIndex);
        return value == null ? null : value.setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        return String.valueOf(value).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        NebulaNativeValue valueWrapper = getValueWrapper(columnIndex);
        if (this.wasNull()) {
            return null;
        }
        return getTimestampFromValueWrapper(valueWrapper);
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        return value == null ? null : new ByteArrayInputStream(value.getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        return value == null ? null : new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        byte[] value = getBytes(columnIndex);
        return value == null ? null : new ByteArrayInputStream(value);
    }


    @Override
    public byte getByte(String columnLabel) throws SQLException {
        return getByte(findColumn(columnLabel));
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        return getShort(findColumn(columnLabel));
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        return getFloat(findColumn(columnLabel));
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        return getBigDecimal(findColumn(columnLabel), scale);
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        return getBytes(findColumn(columnLabel));
    }


    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        return getTimestamp(findColumn(columnLabel));
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        return getAsciiStream(findColumn(columnLabel));
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        return getUnicodeStream(findColumn(columnLabel));
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        return getBinaryStream(findColumn(columnLabel));
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        this.checkClosed();
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
        this.checkClosed();
    }

    @Override
    public String getCursorName() throws SQLException {
        this.checkClosed();
        return null;
    }


    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        return value == null ? null : new StringReader(value);
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        return getCharacterStream(findColumn(columnLabel));
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return BigDecimal.valueOf(((Number) value).longValue());
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw conversionFailure(value, "BigDecimal", exception);
        }
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        return getBigDecimal(findColumn(columnLabel));
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        this.checkClosed();
        if (direction != FETCH_FORWARD && direction != FETCH_REVERSE && direction != FETCH_UNKNOWN) {
            throw new SQLException("Invalid fetch direction: " + direction);
        }
        this.fetchDirection = direction;
    }

    @Override
    public int getFetchDirection() throws SQLException {
        this.checkClosed();
        return fetchDirection;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        this.checkClosed();
        if (rows < 0) {
            throw new SQLException("Fetch size must be greater than or equal to zero.");
        }
        this.fetchSize = rows;
    }

    @Override
    public int getFetchSize() throws SQLException {
        this.checkClosed();
        return fetchSize;
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        this.checkClosed();
        return false;
    }

    @Override
    public boolean rowInserted() throws SQLException {
        this.checkClosed();
        return false;
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        this.checkClosed();
        return false;
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void insertRow() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateRow() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void deleteRow() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void refreshRow() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        return getObject(columnIndex);
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        if (value == null) {
            return null;
        }
        if (value instanceof Ref) {
            return (Ref) value;
        }
        return new NebulaRef("JAVA_OBJECT", value);
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        byte[] value = getBytes(columnIndex);
        return value == null ? null : new NebulaBlob(value);
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        return value == null ? null : new NebulaClob(value);
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        if (value == null) {
            return null;
        }
        if (value instanceof Array) {
            return (Array) value;
        }
        if (value instanceof List) {
            List list = (List) value;
            Object[] values = new Object[list.size()];
            for (int i = 0; i < list.size(); i++) {
                values[i] = unwrapNestedValue(list.get(i));
            }
            return new NebulaArray("JAVA_OBJECT", values);
        }
        if (value instanceof Object[]) {
            return new NebulaArray("JAVA_OBJECT", (Object[]) value);
        }
        return new NebulaArray("JAVA_OBJECT", new Object[] {value});
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        return getObject(columnLabel);
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        return getRef(findColumn(columnLabel));
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        return getBlob(findColumn(columnLabel));
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        return getClob(findColumn(columnLabel));
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        return getArray(findColumn(columnLabel));
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        return getDate(columnIndex);
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return getDate(findColumn(columnLabel), cal);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        return getTime(columnIndex);
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        return getTime(findColumn(columnLabel), cal);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        return getTimestamp(columnIndex);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        return getTimestamp(findColumn(columnLabel), cal);
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        if (value == null) {
            return null;
        }
        if (value instanceof URL) {
            return (URL) value;
        }
        try {
            return new URL(String.valueOf(value));
        } catch (Exception exception) {
            throw new SQLException("Cannot convert column value to URL: " + value, exception);
        }
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        return getURL(findColumn(columnLabel));
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        String value = getNString(columnIndex);
        return value == null ? null : new NebulaNClob(value);
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        return getNClob(findColumn(columnLabel));
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        return value == null ? null : new NebulaSQLXML(value);
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return getSQLXML(findColumn(columnLabel));
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        return getString(columnIndex);
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        return getNString(findColumn(columnLabel));
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        String value = getNString(columnIndex);
        return value == null ? null : new StringReader(value);
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        return getNCharacterStream(findColumn(columnLabel));
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        return convertObject(getObject(columnIndex), type);
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return getObject(findColumn(columnLabel), type);
    }

    @Override
    public void updateObject(int columnIndex, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateObject(String columnLabel, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateObject(int columnIndex, Object x, SQLType targetSqlType) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void updateObject(String columnLabel, Object x, SQLType targetSqlType) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return JdbcWrapperSupport.unwrap(this, iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return JdbcWrapperSupport.isWrapperFor(this, iface);
    }

    private <T> T convertObject(Object value, Class<T> type) throws SQLException {
        if (type == null) {
            throw new SQLException("Target type must not be null.");
        }
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        if (type == String.class) {
            return type.cast(String.valueOf(value));
        }
        if (type == URL.class) {
            return type.cast(getUrlFromValue(value));
        }
        if (Number.class.isAssignableFrom(type)) {
            return convertNumber(value, type);
        }
        if (type == Boolean.class) {
            if (value instanceof Boolean) {
                return type.cast(value);
            }
            if (value instanceof Number) {
                return type.cast(((Number) value).intValue() != 0);
            }
            return type.cast(Boolean.valueOf(String.valueOf(value)));
        }
        if (type == Date.class && value instanceof java.util.Date) {
            return type.cast(new Date(((java.util.Date) value).getTime()));
        }
        if (type == Time.class && value instanceof java.util.Date) {
            return type.cast(new Time(((java.util.Date) value).getTime()));
        }
        if (type == Timestamp.class && value instanceof java.util.Date) {
            return type.cast(new Timestamp(((java.util.Date) value).getTime()));
        }
        throw new SQLException("Cannot convert column value of type " + value.getClass().getName()
                + " to " + type.getName() + ".");
    }

    private URL getUrlFromValue(Object value) throws SQLException {
        if (value instanceof URL) {
            return (URL) value;
        }
        try {
            return new URL(String.valueOf(value));
        } catch (Exception exception) {
            throw new SQLException("Cannot convert column value to URL: " + value, exception);
        }
    }

    private <T> T convertNumber(Object value, Class<T> type) throws SQLException {
        BigDecimal decimal;
        if (value instanceof BigDecimal) {
            decimal = (BigDecimal) value;
        } else if (value instanceof Number) {
            decimal = new BigDecimal(String.valueOf(value));
        } else {
            try {
                decimal = new BigDecimal(String.valueOf(value));
            } catch (NumberFormatException exception) {
                throw new SQLException("Cannot convert column value to number: " + value, exception);
            }
        }
        if (type == Byte.class) {
            return type.cast(Byte.valueOf(decimal.byteValue()));
        }
        if (type == Short.class) {
            return type.cast(Short.valueOf(decimal.shortValue()));
        }
        if (type == Integer.class) {
            return type.cast(Integer.valueOf(decimal.intValue()));
        }
        if (type == Long.class) {
            return type.cast(Long.valueOf(decimal.longValue()));
        }
        if (type == Float.class) {
            return type.cast(Float.valueOf(decimal.floatValue()));
        }
        if (type == Double.class) {
            return type.cast(Double.valueOf(decimal.doubleValue()));
        }
        if (type == BigDecimal.class) {
            return type.cast(decimal);
        }
        throw new SQLException("Unsupported numeric target type: " + type.getName() + ".");
    }

    private SQLException conversionFailure(Object value, String targetType, Exception cause) {
        return new SQLException("Cannot convert column value to " + targetType + ": " + value, cause);
    }
}
