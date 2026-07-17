package com.vesoft.nebula.jdbc.test;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.sql.NebulaParameterBindings;
import com.vesoft.nebula.jdbc.sql.NebulaSqlExecutor;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FakeNebulaSqlExecutor implements NebulaSqlExecutor {
	private final ResultSet resultSet;
	private String lastSql;
	private NebulaParameterBindings lastBindings;

	private FakeNebulaSqlExecutor(ResultSet resultSet) {
		this.resultSet = resultSet;
	}

	public static FakeNebulaSqlExecutor singleString(String value) throws SQLException {
		ResultSet resultSet = mock(ResultSet.class);
		ResultSetMetaData metaData = mock(ResultSetMetaData.class);
		when(metaData.getColumnCount()).thenReturn(1);
		when(metaData.getColumnLabel(1)).thenReturn("name");
		when(metaData.getColumnName(1)).thenReturn("name");
		when(metaData.getColumnType(1)).thenReturn(Types.VARCHAR);
		when(metaData.getColumnTypeName(1)).thenReturn("VARCHAR");
		when(metaData.getColumnClassName(1)).thenReturn(String.class.getName());
		when(resultSet.getMetaData()).thenReturn(metaData);
		when(resultSet.next()).thenReturn(true, false);
		when(resultSet.getString(1)).thenReturn(value);
		when(resultSet.getObject(1)).thenReturn(value);
		return new FakeNebulaSqlExecutor(resultSet);
	}

	public static FakeNebulaSqlExecutor singlePlayer(String playerName, int age) throws SQLException {
		ResultSet resultSet = mock(ResultSet.class);
		ResultSetMetaData metaData = mock(ResultSetMetaData.class);
		when(metaData.getColumnCount()).thenReturn(2);
		when(metaData.getColumnLabel(1)).thenReturn("playerName");
		when(metaData.getColumnName(1)).thenReturn("playerName");
		when(metaData.getColumnType(1)).thenReturn(Types.VARCHAR);
		when(metaData.getColumnTypeName(1)).thenReturn("VARCHAR");
		when(metaData.getColumnClassName(1)).thenReturn(String.class.getName());
		when(metaData.getColumnLabel(2)).thenReturn("age");
		when(metaData.getColumnName(2)).thenReturn("age");
		when(metaData.getColumnType(2)).thenReturn(Types.INTEGER);
		when(metaData.getColumnTypeName(2)).thenReturn("INTEGER");
		when(metaData.getColumnClassName(2)).thenReturn(Integer.class.getName());
		when(resultSet.getMetaData()).thenReturn(metaData);
		when(resultSet.next()).thenReturn(true, false);
		when(resultSet.getString("playerName")).thenReturn(playerName);
		when(resultSet.getString(1)).thenReturn(playerName);
		when(resultSet.getObject("playerName")).thenReturn(playerName);
		when(resultSet.getObject(1)).thenReturn(playerName);
		when(resultSet.getInt("age")).thenReturn(age);
		when(resultSet.getInt(2)).thenReturn(age);
		when(resultSet.getObject("age")).thenReturn(age);
		when(resultSet.getObject(2)).thenReturn(age);
		return new FakeNebulaSqlExecutor(resultSet);
	}

	public static FakeNebulaSqlExecutor singlePlayerSnakeCase(String playerName, int age) throws SQLException {
		ResultSet resultSet = mock(ResultSet.class);
		ResultSetMetaData metaData = mock(ResultSetMetaData.class);
		when(metaData.getColumnCount()).thenReturn(2);
		when(metaData.getColumnLabel(1)).thenReturn("player_name");
		when(metaData.getColumnName(1)).thenReturn("player_name");
		when(metaData.getColumnType(1)).thenReturn(Types.VARCHAR);
		when(metaData.getColumnTypeName(1)).thenReturn("VARCHAR");
		when(metaData.getColumnClassName(1)).thenReturn(String.class.getName());
		when(metaData.getColumnLabel(2)).thenReturn("age");
		when(metaData.getColumnName(2)).thenReturn("age");
		when(metaData.getColumnType(2)).thenReturn(Types.INTEGER);
		when(metaData.getColumnTypeName(2)).thenReturn("INTEGER");
		when(metaData.getColumnClassName(2)).thenReturn(Integer.class.getName());
		when(resultSet.getMetaData()).thenReturn(metaData);
		when(resultSet.next()).thenReturn(true, false);
		when(resultSet.getString("player_name")).thenReturn(playerName);
		when(resultSet.getString(1)).thenReturn(playerName);
		when(resultSet.getObject("player_name")).thenReturn(playerName);
		when(resultSet.getObject(1)).thenReturn(playerName);
		when(resultSet.getInt("age")).thenReturn(age);
		when(resultSet.getInt(2)).thenReturn(age);
		when(resultSet.getObject("age")).thenReturn(age);
		when(resultSet.getObject(2)).thenReturn(age);
		return new FakeNebulaSqlExecutor(resultSet);
	}

	@Override
	public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) {
		this.lastSql = sql;
		this.lastBindings = bindings;
		return resultSet;
	}

	@Override
	public int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
		throw new SQLException("not used");
	}

	public String getLastSql() {
		return lastSql;
	}

	public NebulaParameterBindings getLastBindings() {
		return lastBindings;
	}
}
