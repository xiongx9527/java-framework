package com.xiongx9527.fw.plus.tinyengine.common.typehandler;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.tinyengine.common.util.TinyEngineJsonUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JsonObjectTypeHandler extends BaseTypeHandler<JSONObject> {

	@Override
	public void setNonNullParameter(PreparedStatement statement, int index, JSONObject parameter, JdbcType jdbcType)
			throws SQLException {
		statement.setString(index, TinyEngineJsonUtils.toJson(parameter));
	}

	@Override
	public JSONObject getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
		return TinyEngineJsonUtils.parseObject(resultSet.getString(columnName));
	}

	@Override
	public JSONObject getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
		return TinyEngineJsonUtils.parseObject(resultSet.getString(columnIndex));
	}

	@Override
	public JSONObject getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
		return TinyEngineJsonUtils.parseObject(statement.getString(columnIndex));
	}
}
