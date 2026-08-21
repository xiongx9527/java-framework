package com.xiongx9527.fw.plus.tinyengine.common.typehandler;

import com.xiongx9527.fw.plus.tinyengine.common.util.TinyEngineJsonUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class JsonObjectTypeHandler extends BaseTypeHandler<Map<String, Object>> {

	@Override
	public void setNonNullParameter(PreparedStatement statement, int index, Map<String, Object> parameter, JdbcType jdbcType)
			throws SQLException {
		statement.setString(index, TinyEngineJsonUtils.toJson(parameter));
	}

	@Override
	public Map<String, Object> getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
		return TinyEngineJsonUtils.parseObject(resultSet.getString(columnName));
	}

	@Override
	public Map<String, Object> getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
		return TinyEngineJsonUtils.parseObject(resultSet.getString(columnIndex));
	}

	@Override
	public Map<String, Object> getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
		return TinyEngineJsonUtils.parseObject(statement.getString(columnIndex));
	}
}
