package com.xiongx9527.fw.plus.tinyengine.dynamic.service.impl;

import com.xiongx9527.fw.plus.tinyengine.dynamic.mapper.ModelDataMapper;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicDelete;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicInsert;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicPageResult;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicQuery;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableColumn;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicUpdate;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.DynamicService;
import com.xiongx9527.fw.plus.tinyengine.dynamic.sql.TinyEngineSqlIdentifierValidator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DynamicServiceImpl implements DynamicService {

	private final ModelDataMapper mapper;

	public DynamicServiceImpl(ModelDataMapper mapper) {
		if (mapper == null) {
			throw new IllegalArgumentException("ModelDataMapper must not be null");
		}
		this.mapper = mapper;
	}

	@Override
	public DynamicPageResult query(DynamicQuery query) {
		if (query == null) {
			throw new IllegalArgumentException("DynamicQuery must not be null");
		}
		int currentPage = positive(query.getCurrentPage(), "current page");
		int pageSize = positive(query.getPageSize(), "page size");
		long offset = (long) (currentPage - 1) * pageSize;
		if (offset > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Pagination offset is too large");
		}
		Map<String, Object> params = baseQueryParams(query);
		params.put("offset", Integer.valueOf((int) offset));
		params.put("pageSize", Integer.valueOf(pageSize));
		List<Map<String, Object>> list = mapper.select(params);
		long total = mapper.count(params);
		return new DynamicPageResult(list, total, currentPage, pageSize);
	}

	@Override
	public long insert(DynamicInsert insert) {
		if (insert == null) {
			throw new IllegalArgumentException("DynamicInsert must not be null");
		}
		return mapper.insert(writeParams(insert.getTableName(), insert.getData(), null));
	}

	@Override
	public int update(DynamicUpdate update) {
		if (update == null) {
			throw new IllegalArgumentException("DynamicUpdate must not be null");
		}
		return mapper.update(writeParams(update.getTableName(), update.getData(), update.getConditions()));
	}

	@Override
	public int delete(DynamicDelete delete) {
		if (delete == null) {
			throw new IllegalArgumentException("DynamicDelete must not be null");
		}
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put("tableName", TinyEngineSqlIdentifierValidator.validateTableName(delete.getTableName()));
		validateFieldMap(delete.getConditions());
		params.put("conditions", delete.getConditions());
		return mapper.delete(params);
	}

	@Override
	public List<DynamicTableColumn> getTableStructure(String tableName) {
		return mapper.describe(TinyEngineSqlIdentifierValidator.validateTableName(tableName));
	}

	private Map<String, Object> baseQueryParams(DynamicQuery query) {
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put("tableName", TinyEngineSqlIdentifierValidator.validateTableName(query.getTableName()));
		if (query.getFields() != null && !query.getFields().isEmpty()) {
			params.put("fields", TinyEngineSqlIdentifierValidator.validateFieldList(query.getFields(), "query field"));
		}
		validateOptionalFieldMap(query.getConditions());
		params.put("conditions", query.getConditions());
		if (query.getOrderBy() != null && !query.getOrderBy().trim().isEmpty()) {
			params.put("orderBy", TinyEngineSqlIdentifierValidator.validateOrderBy(query.getOrderBy()));
			params.put("orderType", TinyEngineSqlIdentifierValidator.validateSortDirection(query.getOrderType()).name());
		}
		return params;
	}

	private Map<String, Object> writeParams(String tableName, Map<String, Object> data, Map<String, Object> conditions) {
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put("tableName", TinyEngineSqlIdentifierValidator.validateTableName(tableName));
		validateFieldMap(data);
		params.put("data", data);
		if (conditions != null) {
			validateFieldMap(conditions);
			params.put("conditions", conditions);
		}
		return params;
	}

	private void validateFieldMap(Map<String, Object> values) {
		if (values == null || values.isEmpty()) {
			throw new IllegalArgumentException("Dynamic field values must not be empty");
		}
		for (String field : values.keySet()) {
			TinyEngineSqlIdentifierValidator.validateFieldName(field);
		}
	}

	private void validateOptionalFieldMap(Map<String, Object> values) {
		if (values == null || values.isEmpty()) {
			return;
		}
		validateFieldMap(values);
	}

	private int positive(Integer value, String label) {
		if (value == null || value.intValue() < 1) {
			throw new IllegalArgumentException(label + " must be greater than zero");
		}
		return value.intValue();
	}
}
