package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.dynamic.sql.TinyEngineSqlIdentifierValidator;
import com.xiongx9527.fw.plus.tinyengine.mapper.ModelMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Model;
import com.xiongx9527.fw.plus.tinyengine.model.dto.ParametersDto;
import com.xiongx9527.fw.plus.tinyengine.service.ModelService;
import com.xiongx9527.fw.plus.tinyengine.service.AppRoleService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppPermission;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ModelServiceImpl extends MapperServiceSupport<ModelMapper> implements ModelService {
	private static final Pattern SQL_DEFAULT_LITERAL = Pattern.compile(
			"^(?:NULL|TRUE|FALSE|CURRENT_TIMESTAMP|[-+]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)|'(?:[^'\\\\\\r\\n]|'')*')$",
			Pattern.CASE_INSENSITIVE);

	@Autowired
	private TinyEngineUserContext userContext;
	@Autowired
	private AppRoleService appRoleService;

	@Override
	public boolean save(Model model) {
		require(model == null ? null : model.getAppId(), AppPermission.MANAGE);
		prepareCreate(model);
		return baseMapper.createModel(model) == 1;
	}

	@Override
	public Model queryModelById(Integer id) {
		Model model = baseMapper.selectById(id);
		require(model == null ? null : model.getAppId(), AppPermission.READ);
		return model;
	}

	@Override
	public Model queryModelDetailById(Integer id) {
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		if (id == null || user == null || user.getTenantId() == null || user.getTenantId().trim().isEmpty()) {
			return null;
		}
		Model model = baseMapper.selectOne(new QueryWrapper<Model>()
				.eq("id", id)
				.eq("tenant_id", user.getTenantId()));
		require(model == null ? null : model.getAppId(), AppPermission.READ);
		return model;
	}

	@Override
	public List<Model> getModelByName(String nameCn) {
		List<Model> models = baseMapper.selectList(new QueryWrapper<Model>().like("name_cn", nameCn));
		requireAll(models, AppPermission.READ);
		return models;
	}

	@Override
	public List<Model> getModelByEnName(String nameEn) {
		List<Model> models = baseMapper.selectList(new QueryWrapper<Model>().eq("name_en", nameEn));
		requireAll(models, AppPermission.READ);
		return models;
	}

	@Override
	public Page<Model> pageQuery(int currentPage, int pageSize, String nameCn, String nameEn) {
		QueryWrapper<Model> query = new QueryWrapper<Model>();
		final boolean hasNameCn = nameCn != null && !nameCn.isEmpty();
		final boolean hasNameEn = nameEn != null && !nameEn.isEmpty();
		if (hasNameCn || hasNameEn) {
			query.and(names -> {
				if (hasNameCn) {
					names.like("name_cn", nameCn);
				}
				if (hasNameEn) {
					if (hasNameCn) {
						names.or();
					}
					names.like("name_en", nameEn);
				}
			});
		}
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		if (user != null) {
			query.eq("created_by", user.getUserId()).eq("tenant_id", user.getTenantId());
		}
		Page<Model> page = new Page<Model>(currentPage, pageSize);
		baseMapper.selectPage(page, query);
		requireAll(page.getRecords(), AppPermission.READ);
		return page;
	}

	@Override
	public String getTableById(Integer id) {
		Model model = queryModelById(id);
		if (model == null) {
			return "";
		}
		StringBuilder sql = new StringBuilder(tableSql(model));
		for (ParametersDto field : safeParameters(model)) {
			if (Boolean.TRUE.equals(field.getIsModel()) && field.getDefaultValue() != null) {
				Model nested = queryModelById(Integer.valueOf(field.getDefaultValue()));
				if (nested != null) {
					sql.append(tableSql(nested));
				}
			}
		}
		return sql.toString();
	}

	@Override
	public String getAllTable() {
		List<Model> models = baseMapper.selectList(null);
		requireAll(models, AppPermission.READ);
		StringJoiner sql = new StringJoiner(" ");
		if (models != null) {
			for (Model model : models) {
				sql.add(tableSql(model));
			}
		}
		return sql.toString();
	}

	@Override
	public List<String> getAllModelName() {
		List<Model> models = baseMapper.selectList(null);
		requireAll(models, AppPermission.READ);
		return models == null ? Collections.<String>emptyList()
				: models.stream().map(Model::getNameEn).collect(Collectors.toList());
	}

	@Override
	public List<Model> queryModelByCondition(Model model) {
		require(model == null ? null : model.getAppId(), AppPermission.READ);
		List<Model> models = baseMapper.queryModelByCondition(model);
		requireAll(models, AppPermission.READ);
		return models;
	}

	@Override
	public boolean updateById(Model model) {
		Model current = model == null ? null : baseMapper.selectById(model.getId());
		require(current == null ? null : current.getAppId(), AppPermission.DEVELOP);
		if (model != null && current != null) {
			model.setAppId(current.getAppId());
			model.setTenantId(current.getTenantId());
			model.setRenterId(current.getRenterId());
			model.setSiteId(current.getSiteId());
		}
		return baseMapper.updateById(model) == 1;
	}

	@Override
	public boolean removeById(java.io.Serializable id) {
		Model current = id == null ? null : baseMapper.selectById(id);
		require(current == null ? null : current.getAppId(), AppPermission.DEVELOP);
		return baseMapper.deleteById(id) == 1;
	}

	private void prepareCreate(Model model) {
		if (model == null) {
			return;
		}
		if (isBlank(model.getNameCn()) && !isBlank(model.getNameEn())) {
			model.setNameCn(model.getNameEn());
		}
		if (isBlank(model.getNameEn()) && !isBlank(model.getNameCn())) {
			model.setNameEn(model.getNameCn());
		}
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		LocalDateTime now = LocalDateTime.now();
			if (user != null) {
				model.setTenantId(user.getTenantId());
				model.setRenterId(user.getRenterId());
				model.setCreatedBy(user.getUserId());
				model.setLastUpdatedBy(user.getUserId());
			if (model.getPlatformId() == null) {
				model.setPlatformId(user.getPlatformId());
				}
			}
			if (model.getParameters() == null) {
				model.setParameters(Collections.<ParametersDto>emptyList());
			}
			model.setCreatedTime(now);
			model.setLastUpdatedTime(now);
		}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private List<ParametersDto> safeParameters(Model model) {
		return model.getParameters() == null ? Collections.<ParametersDto>emptyList() : model.getParameters();
	}

	private String tableSql(Model model) {
		String tableName = TinyEngineSqlIdentifierValidator.validateTableName(model.getNameEn());
		StringBuilder sql = new StringBuilder("CREATE TABLE ").append(tableName).append(" (");
		List<ParametersDto> fields = safeParameters(model);
		for (int index = 0; index < fields.size(); index++) {
			ParametersDto field = fields.get(index);
			sql.append(TinyEngineSqlIdentifierValidator.validateFieldName(field.getProp()))
					.append(' ').append(sqlType(field.getType()));
			if (field.getDefaultValue() != null && !field.getDefaultValue().isEmpty()) {
				sql.append(" DEFAULT ").append(requireDefaultLiteral(field.getDefaultValue()));
			}
			if (index < fields.size() - 1) {
				sql.append(", ");
			}
		}
		return sql.append(");").toString();
	}

	private String requireDefaultLiteral(String defaultValue) {
		if (!SQL_DEFAULT_LITERAL.matcher(defaultValue).matches()) {
			throw invalidDdl("Invalid SQL default literal");
		}
		return defaultValue;
	}

	private ServiceException invalidDdl(String message) {
		return new ServiceException(ExceptionEnum.CM002.getResultCode(), message);
	}

	private String sqlType(String javaType) {
		if ("String".equals(javaType)) {
			return "VARCHAR(500)";
		}
		if ("Number".equals(javaType)) {
			return "INT";
		}
		if ("Boolean".equals(javaType)) {
			return "BOOLEAN";
		}
		if ("Date".equals(javaType)) {
			return "TIMESTAMP";
		}
		return "LONGTEXT";
	}

	private void require(Integer appId, AppPermission permission) {
		if (appRoleService != null) {
			appRoleService.requirePermission(appId, permission);
		}
	}

	private void requireAll(List<Model> models, AppPermission permission) {
		if (models == null) {
			return;
		}
		for (Model model : models) {
			require(model == null ? null : model.getAppId(), permission);
		}
	}
}
