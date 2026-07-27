package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.service.AppTemplateService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.stereotype.Service;

@Service
public class AppTemplateServiceImpl extends ServiceImpl<AppMapper, App> implements AppTemplateService {

	private final TinyEngineUserContext userContext;

	public AppTemplateServiceImpl(TinyEngineUserContext userContext) {
		this.userContext = userContext;
	}

	@Override
	public AppDto queryAllAppTemplate(Integer currentPage, Integer pageSize, String orderBy, App app) {
		int safePage = currentPage == null || currentPage < 1 ? 1 : currentPage;
		int safeSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
		int offset = (safePage - 1) * safeSize;
		App condition = app == null ? new App() : app;
		TinyEngineUser user = userContext.getCurrentUser();
		String tenantId = user == null ? null : user.getTenantId();
		AppDto result = new AppDto();
		result.setApps(baseMapper.queryAllAppTemplate(safeSize, offset, condition.getName(),
				condition.getIndustryId(), condition.getSceneId(), condition.getFramework(), orderBy,
				condition.getCreatedBy(), tenantId));
		result.setTotal(baseMapper.queryAppTemplateTotal(tenantId));
		return result;
	}
}
