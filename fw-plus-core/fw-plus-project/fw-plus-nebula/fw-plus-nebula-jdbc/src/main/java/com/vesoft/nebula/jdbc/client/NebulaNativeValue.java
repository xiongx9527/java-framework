package com.vesoft.nebula.jdbc.client;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface NebulaNativeValue {
	boolean isNull();

	boolean isString();

	boolean isLong();

	boolean isInt();

	boolean isFloat();

	boolean isDouble();

	boolean isBoolean();

	boolean isDate();

	boolean isTime();

	boolean isDateTime();

	String asString() throws SQLException;

	long asLong() throws SQLException;

	int asInt() throws SQLException;

	float asFloat() throws SQLException;

	double asDouble() throws SQLException;

	boolean asBoolean() throws SQLException;

	Date asDate() throws SQLException;

	Time asTime() throws SQLException;

	Timestamp asTimestamp() throws SQLException;

	List asList() throws SQLException;

	Set asSet() throws SQLException;

	Map asMap() throws SQLException;

	Object asNode() throws SQLException;

	Object asEdge() throws SQLException;

	Object asPath() throws SQLException;

	Object rawValue();

	Object unwrap();
}
