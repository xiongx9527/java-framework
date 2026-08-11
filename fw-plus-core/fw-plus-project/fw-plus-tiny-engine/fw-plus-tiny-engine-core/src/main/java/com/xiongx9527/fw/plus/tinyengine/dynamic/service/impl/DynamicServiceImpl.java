package com.xiongx9527.fw.plus.tinyengine.dynamic.service.impl;

import com.xiongx9527.fw.plus.tinyengine.dynamic.mapper.ModelDataMapper;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicDelete;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicInsert;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicInsertResult;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicPageResult;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicQuery;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableColumn;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicUpdate;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.DynamicService;
import com.xiongx9527.fw.plus.tinyengine.dynamic.sql.TinyEngineSqlIdentifierValidator;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Model;
import com.xiongx9527.fw.plus.tinyengine.service.ModelService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.function.Supplier;

public class DynamicServiceImpl implements DynamicService {

	private final ModelDataMapper mapper;
	private final Supplier<ModelService> modelServiceSupplier;
	private final TinyEngineUserContext userContext;

	public DynamicServiceImpl(ModelDataMapper mapper, ModelService modelService, TinyEngineUserContext userContext) {
		this(mapper, constantModelService(modelService), userContext);
	}

	public DynamicServiceImpl(ModelDataMapper mapper, Supplier<ModelService> modelServiceSupplier,
			TinyEngineUserContext userContext) {
		if (mapper == null) {
			throw new IllegalArgumentException("ModelDataMapper must not be null");
		}
		if (modelServiceSupplier == null) {
			throw new IllegalArgumentException("ModelService supplier must not be null");
		}
		if (userContext == null) {
			throw new IllegalArgumentException("TinyEngineUserContext must not be null");
		}
		this.mapper = mapper;
		this.modelServiceSupplier = modelServiceSupplier;
		this.userContext = userContext;
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
	public DynamicInsertResult insert(DynamicInsert insert) {
		if (insert == null) {
			throw new IllegalArgumentException("DynamicInsert must not be null");
		}
		Map<String, Object> params = writeParams(insert.getTableName(), insert.getData(), null);
		int inserted = mapper.insert(params);
		return new DynamicInsertResult(inserted, generatedId(params.get("id")));
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
		params.put("tableName", resolveAuthorizedTableName(delete.getTableName()));
		validateFieldMap(delete.getConditions());
		params.put("conditions", delete.getConditions());
		return mapper.delete(params);
	}

	@Override
	public List<DynamicTableColumn> getTableStructure(String tableName) {
		return mapper.describe(resolveAuthorizedTableName(tableName));
	}

	private Map<String, Object> baseQueryParams(DynamicQuery query) {
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put("tableName", resolveAuthorizedTableName(query.getTableName()));
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
		params.put("tableName", resolveAuthorizedTableName(tableName));
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

	private String resolveAuthorizedTableName(String requestedName) {
		String validatedName = TinyEngineSqlIdentifierValidator.validateTableName(requestedName);
		String modelName = validatedName.startsWith("dynamic_") ? validatedName.substring("dynamic_".length()) : validatedName;
		TinyEngineSqlIdentifierValidator.validateTableName(modelName);
		TinyEngineUser user = userContext.getCurrentUser();
		String tenantId = user == null ? null : user.getTenantId();
		if (tenantId == null || tenantId.trim().isEmpty()) {
			throw new IllegalArgumentException("Current tenant is required for dynamic model access");
		}
		ModelService modelService = modelServiceSupplier.get();
		if (modelService == null) {
			throw new IllegalStateException("ModelService is required for dynamic model authorization");
		}
		List<Model> models = modelService.getModelByEnName(modelName);
		if (models == null || models.isEmpty()) {
			throw new IllegalArgumentException("Dynamic model is not registered: " + modelName);
		}
		for (Model model : models) {
			if (model != null && tenantId.equals(model.getTenantId())) {
				return "dynamic_" + modelName.toLowerCase(Locale.ROOT);
			}
		}
		throw new IllegalArgumentException("Dynamic model does not belong to current tenant: " + modelName);
	}

	private static Supplier<ModelService> constantModelService(final ModelService modelService) {
		if (modelService == null) {
			throw new IllegalArgumentException("ModelService must not be null");
		}
		return new Supplier<ModelService>() {
			@Override
			public ModelService get() {
				return modelService;
			}
		};
	}

	private BigInteger generatedId(Object value) {
		if (value == null) {
			throw new IllegalStateException("Dynamic insert did not return a generated id");
		}
		try {
			if (value instanceof BigInteger) {
				return (BigInteger) value;
			}
			if (value instanceof BigDecimal) {
				return ((BigDecimal) value).toBigIntegerExact();
			}
			return new BigInteger(String.valueOf(value));
		} catch (ArithmeticException ex) {
			throw new IllegalStateException("Dynamic insert returned a non-integral generated id", ex);
		} catch (NumberFormatException ex) {
			throw new IllegalStateException("Dynamic insert returned an invalid generated id", ex);
		}
	}
}
