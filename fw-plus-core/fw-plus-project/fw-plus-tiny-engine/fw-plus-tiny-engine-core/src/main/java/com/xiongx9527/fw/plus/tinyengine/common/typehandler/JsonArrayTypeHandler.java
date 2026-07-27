package com.xiongx9527.fw.plus.tinyengine.common.typehandler;

import com.alibaba.fastjson2.JSONArray;
import com.xiongx9527.fw.plus.tinyengine.common.util.TinyEngineJsonUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JsonArrayTypeHandler extends BaseTypeHandler<JSONArray> {

	@Override
	public void setNonNullParameter(PreparedStatement statement, int index, JSONArray parameter, JdbcType jdbcType)
			throws SQLException {
		statement.setString(index, TinyEngineJsonUtils.toJson(parameter));
	}

	@Override
	public JSONArray getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
		return TinyEngineJsonUtils.parseArray(resultSet.getString(columnName));
	}

	@Override
	public JSONArray getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
		return TinyEngineJsonUtils.parseArray(resultSet.getString(columnIndex));
	}

	@Override
	public JSONArray getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
		return TinyEngineJsonUtils.parseArray(statement.getString(columnIndex));
	}
}
