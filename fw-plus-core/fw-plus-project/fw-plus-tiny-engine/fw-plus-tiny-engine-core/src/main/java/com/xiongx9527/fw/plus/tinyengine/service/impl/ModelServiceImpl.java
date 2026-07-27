package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.ModelMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Model;
import com.xiongx9527.fw.plus.tinyengine.model.dto.ParametersDto;
import com.xiongx9527.fw.plus.tinyengine.service.ModelService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Collections;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ModelServiceImpl extends ServiceImpl<ModelMapper, Model> implements ModelService {
	private static final Pattern SQL_IDENTIFIER = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
	private static final Pattern SQL_DEFAULT_LITERAL = Pattern.compile(
			"^(?:NULL|TRUE|FALSE|CURRENT_TIMESTAMP|[-+]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)|'(?:[^'\\\\\\r\\n]|'')*')$",
			Pattern.CASE_INSENSITIVE);

	@Autowired
	private TinyEngineUserContext userContext;

	@Override
	public Model queryModelById(Integer id) {
		return baseMapper.selectById(id);
	}

	@Override
	public List<Model> getModelByName(String nameCn) {
		return baseMapper.selectList(new QueryWrapper<Model>().like("name_cn", nameCn));
	}

	@Override
	public List<Model> getModelByEnName(String nameEn) {
		return baseMapper.selectList(new QueryWrapper<Model>().eq("name_en", nameEn));
	}

	@Override
	public Page<Model> pageQuery(int currentPage, int pageSize, String nameCn, String nameEn) {
		QueryWrapper<Model> query = new QueryWrapper<Model>();
		if (nameCn != null && !nameCn.isEmpty()) {
			query.like("name_cn", nameCn);
		}
		if (nameEn != null && !nameEn.isEmpty()) {
			if (nameCn != null && !nameCn.isEmpty()) {
				query.or().like("name_en", nameEn);
			} else {
				query.like("name_en", nameEn);
			}
		}
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		if (user != null) {
			query.eq("created_by", user.getUserId()).eq("tenant_id", user.getTenantId());
		}
		Page<Model> page = new Page<Model>(currentPage, pageSize);
		baseMapper.selectPage(page, query);
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
		return models == null ? Collections.<String>emptyList()
				: models.stream().map(Model::getNameEn).collect(Collectors.toList());
	}

	@Override
	public List<Model> queryModelByCondition(Model model) {
		return baseMapper.queryModelByCondition(model);
	}

	private List<ParametersDto> safeParameters(Model model) {
		return model.getParameters() == null ? Collections.<ParametersDto>emptyList() : model.getParameters();
	}

	private String tableSql(Model model) {
		String tableName = requireIdentifier(model.getNameEn(), "table");
		StringBuilder sql = new StringBuilder("CREATE TABLE ").append(tableName).append(" (");
		List<ParametersDto> fields = safeParameters(model);
		for (int index = 0; index < fields.size(); index++) {
			ParametersDto field = fields.get(index);
			sql.append(requireIdentifier(field.getProp(), "column")).append(' ').append(sqlType(field.getType()));
			if (field.getDefaultValue() != null && !field.getDefaultValue().isEmpty()) {
				sql.append(" DEFAULT ").append(requireDefaultLiteral(field.getDefaultValue()));
			}
			if (index < fields.size() - 1) {
				sql.append(", ");
			}
		}
		return sql.append(");").toString();
	}

	private String requireIdentifier(String identifier, String kind) {
		if (identifier == null || !SQL_IDENTIFIER.matcher(identifier).matches()) {
			throw invalidDdl("Invalid SQL " + kind + " identifier");
		}
		return identifier;
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
}
