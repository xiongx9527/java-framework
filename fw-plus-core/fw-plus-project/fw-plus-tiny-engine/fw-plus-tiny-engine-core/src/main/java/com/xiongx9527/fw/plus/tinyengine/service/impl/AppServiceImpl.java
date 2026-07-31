package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.I18nEntryMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.SchemaI18n;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nEntry;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Platform;
import com.xiongx9527.fw.plus.tinyengine.service.AppService;
import com.xiongx9527.fw.plus.tinyengine.service.I18nEntryService;
import com.xiongx9527.fw.plus.tinyengine.service.PlatformService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {
	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int MAX_PAGE_SIZE = 1000;

	private final TinyEngineUserContext userContext;
	@Autowired
	private PlatformService platformService;
	@Autowired
	private I18nEntryService i18nEntryService;
	@Autowired
	private I18nEntryMapper i18nEntryMapper;

	public AppServiceImpl(TinyEngineUserContext userContext) {
		this.userContext = userContext;
	}

	@Override
	public List<App> queryAllApp() {
		return baseMapper.queryAllApp(tenantId());
	}

	@Override
	public AppDto queryAllAppByPage(Integer currentPage, Integer pageSize, String orderBy, App app) {
		int safePage = currentPage == null || currentPage < 1 ? 1 : currentPage;
		int safeSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
		long offsetValue = (long) (safePage - 1) * safeSize;
		int offset = offsetValue > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) offsetValue;
		App condition = app == null ? new App() : app;
		AppDto result = new AppDto();
		result.setApps(baseMapper.queryAllAppByPage(safeSize, offset, condition.getName(),
				condition.getIndustryId(), condition.getSceneId(), condition.getFramework(), orderBy,
				condition.getCreatedBy(), tenantId()));
		result.setTotal(baseMapper.queryAppTotal(tenantId()));
		return result;
	}

	@Override
	public App queryAppById(Integer id) {
		App app = baseMapper.queryAppById(id, tenantId());
		if (app == null) {
			throw failure(ExceptionEnum.CM009);
		}
		return app;
	}

	@Override
	public List<App> queryAppByCondition(App app) {
		return baseMapper.queryAppByCondition(app);
	}

	@Override
	public App deleteAppById(Integer id) {
		App app = baseMapper.queryAppById(id, tenantId());
		if (app == null || baseMapper.deleteAppById(id, tenantId()) < 1) {
			throw failure(ExceptionEnum.CM009);
		}
		return app;
	}

	@Override
	public App updateAppById(App app) {
		if (app == null || app.getId() == null) {
			throw failure(ExceptionEnum.CM002);
		}
		String targetTenant = app.getTenantId() == null ? tenantId() : app.getTenantId();
		app.setTenantId(targetTenant);
		App existing = baseMapper.queryAppById(app.getId(), targetTenant);
		if (existing == null) {
			throw failure(ExceptionEnum.CM009);
		}
		if (app.getExtendConfig() != null && !app.getExtendConfig().isEmpty()) {
			Platform platform = platformService.queryPlatformById(existing.getPlatformId());
			if (platform != null && platform.getAppExtendConfig() != null) {
				Map<String, Object> inherited = new HashMap<String, Object>(platform.getAppExtendConfig());
				inherited.remove("route");
				app.getExtendConfig().putAll(inherited);
			}
		}
		if (baseMapper.updateAppById(app) < 1) {
			throw failure(ExceptionEnum.CM001);
		}
		return baseMapper.queryAppById(app.getId(), targetTenant);
	}

	@Override
	public App createApp(App app) {
		String currentTenant = tenantId();
		if (currentTenant == null) {
			throw failure(ExceptionEnum.CM337);
		}
		app.setTenantId(currentTenant);
		if (!baseMapper.queryAppByCondition(app).isEmpty()) {
			throw failure(ExceptionEnum.CM003);
		}
		app.setIsPublish(false);
		app.setPlatformHistoryId("1");
		if (baseMapper.createApp(app) < 1) {
			throw failure(ExceptionEnum.CM001);
		}
		return app;
	}

	@Override
	public SchemaI18n formatI18nEntrites(List<I18nEntryDto> i18nEntries, Integer usedIn, Integer id) {
		if (i18nEntries != null && !i18nEntries.isEmpty()) {
			return i18nEntryService.formatEntriesList(i18nEntries);
		}
		I18nEntry condition = new I18nEntry();
		condition.setHostType(Integer.valueOf(1).equals(usedIn) ? "app" : "block");
		return i18nEntryService.formatEntriesList(
				i18nEntryMapper.findI18nEntriesByHostandHostType(id, condition.getHostType()));
	}

	private String tenantId() {
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		return user == null ? null : user.getTenantId();
	}

	private ServiceException failure(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}
}
