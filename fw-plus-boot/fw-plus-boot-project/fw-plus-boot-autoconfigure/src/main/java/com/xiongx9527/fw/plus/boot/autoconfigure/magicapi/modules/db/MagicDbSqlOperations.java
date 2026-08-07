package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.ssssssss.magicapi.modules.db.BoundSql;
import org.ssssssss.magicapi.modules.db.SQLModule;
import org.ssssssss.magicapi.modules.db.model.Page;
import org.ssssssss.magicapi.modules.db.table.NamedTable;
import org.ssssssss.script.runtime.RuntimeContext;

import java.util.List;
import java.util.Map;

/**
 * 复用 Magic-API db 模块完整 SQL 能力。
 */
public interface MagicDbSqlOperations {

	SQLModule db();

	default SQLModule cache(String cacheName, long ttl) {
		return db().cache(cacheName, ttl);
	}

	default SQLModule cache(String cacheName) {
		return db().cache(cacheName);
	}

	default SQLModule deleteCache(String cacheName) {
		return db().deleteCache(cacheName);
	}

	default SQLModule camel() {
		return db().camel();
	}

	default SQLModule pascal() {
		return db().pascal();
	}

	default SQLModule lower() {
		return db().lower();
	}

	default SQLModule upper() {
		return db().upper();
	}

	default SQLModule normal() {
		return db().normal();
	}

	default SQLModule columnCase(String columnCase) {
		return db().columnCase(columnCase);
	}

	default List<Map<String, Object>> select(RuntimeContext context, String sql) {
		return db().select(context, sql);
	}

	default List<Map<String, Object>> select(RuntimeContext context, String sql, Map<String, Object> params) {
		return db().select(context, sql, params);
	}

	default List<Map<String, Object>> select(BoundSql boundSql) {
		return db().select(boundSql);
	}

	default Map<String, Object> selectOne(RuntimeContext context, String sql) {
		return db().selectOne(context, sql);
	}

	default Map<String, Object> selectOne(RuntimeContext context, String sql, Map<String, Object> params) {
		return db().selectOne(context, sql, params);
	}

	default Map<String, Object> selectOne(BoundSql boundSql) {
		return db().selectOne(boundSql);
	}

	default Object selectValue(RuntimeContext context, String sql) {
		return db().selectValue(context, sql);
	}

	default Object selectValue(RuntimeContext context, String sql, Map<String, Object> params) {
		return db().selectValue(context, sql, params);
	}

	default Integer selectInt(RuntimeContext context, String sql) {
		return db().selectInt(context, sql);
	}

	default Integer selectInt(RuntimeContext context, String sql, Map<String, Object> params) {
		return db().selectInt(context, sql, params);
	}

	default Integer selectInt(BoundSql boundSql) {
		return db().selectInt(boundSql);
	}

	default Integer count(RuntimeContext context, String sql) {
		return db().count(context, sql);
	}

	default Integer count(RuntimeContext context, String sql, Map<String, Object> params) {
		return db().count(context, sql, params);
	}

	default int update(RuntimeContext context, String sql) {
		return db().update(context, sql);
	}

	default int update(RuntimeContext context, String sql, Map<String, Object> params) {
		return db().update(context, sql, params);
	}

	default int update(BoundSql boundSql) {
		return db().update(boundSql);
	}

	default Object insert(RuntimeContext context, String sql) {
		return db().insert(context, sql);
	}

	default Object insert(RuntimeContext context, String sql, Map<String, Object> params) {
		return db().insert(context, sql, params);
	}

	default Object insert(RuntimeContext context, String sql, String generatedKey) {
		return db().insert(context, sql, generatedKey);
	}

	default Object insert(RuntimeContext context, String sql, String generatedKey, Map<String, Object> params) {
		return db().insert(context, sql, generatedKey, params);
	}

	default Object insert(BoundSql boundSql, String generatedKey) {
		return db().insert(boundSql, generatedKey);
	}

	default int batchUpdate(RuntimeContext context, String sql, List<Object[]> batchParameters) {
		return db().batchUpdate(context, sql, batchParameters);
	}

	default int batchUpdate(RuntimeContext context, String sql, int batchSize, List<Object[]> batchParameters) {
		return db().batchUpdate(context, sql, batchSize, batchParameters);
	}

	default int batchUpdate(List<String> sqlList) {
		return db().batchUpdate(sqlList);
	}

	default Object page(RuntimeContext context, String sql) {
		return db().page(context, sql);
	}

	default Object page(RuntimeContext context, String sql, Map<String, Object> params) {
		return db().page(context, sql, params);
	}

	default Object page(RuntimeContext context, String sql, long limit, long offset) {
		return db().page(context, sql, limit, offset);
	}

	default Object page(RuntimeContext context, String sql, long limit, long offset, Map<String, Object> params) {
		return db().page(context, sql, limit, offset, params);
	}

	default Object page(BoundSql boundSql) {
		return db().page(boundSql);
	}

	default Object page(RuntimeContext context, String countSql, String sql) {
		return db().page(context, countSql, sql);
	}

	default Object page(RuntimeContext context, String countSql, String sql, Map<String, Object> params) {
		return db().page(context, countSql, sql, params);
	}

	default Object page(RuntimeContext context, int total, String sql, long limit, long offset, Map<String, Object> params) {
		return db().page(context, total, sql, limit, offset, params);
	}

	default Object page(BoundSql boundSql, Page page) {
		return db().page(boundSql, page);
	}

	default Object call(RuntimeContext context, String sql) {
		return db().call(context, sql);
	}

	default NamedTable table(String tableName) {
		return db().table(tableName);
	}

	default JdbcTemplate getJdbcTemplate() {
		return db().getJdbcTemplate();
	}
}
