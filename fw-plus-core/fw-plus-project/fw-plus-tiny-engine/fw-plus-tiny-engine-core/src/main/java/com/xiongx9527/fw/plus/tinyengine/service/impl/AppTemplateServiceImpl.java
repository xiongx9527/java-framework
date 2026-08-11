package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppExtensionMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.DatasourceMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.I18nEntryMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.ModelMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageHistoryMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AppExtension;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Datasource;
import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nEntry;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Model;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageHistory;
import com.xiongx9527.fw.plus.tinyengine.service.AppTemplateService;
import com.xiongx9527.fw.plus.tinyengine.service.PageService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AppTemplateServiceImpl extends ServiceImpl<AppMapper, App> implements AppTemplateService {

	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int MAX_PAGE_SIZE = 1000;

	private final TinyEngineUserContext userContext;

	@Autowired
	private PageService pageService;
	@Autowired
	private PageMapper pageMapper;
	@Autowired
	private PageHistoryMapper pageHistoryMapper;
	@Autowired
	private AppExtensionMapper appExtensionMapper;
	@Autowired
	private DatasourceMapper datasourceMapper;
	@Autowired
	private I18nEntryMapper i18nEntryMapper;
	@Autowired
	private ModelMapper modelMapper;

	public AppTemplateServiceImpl(TinyEngineUserContext userContext) {
		this.userContext = userContext;
	}

	@Override
	public AppDto queryAllAppTemplate(Integer currentPage, Integer pageSize, String orderBy, App app) {
		int safePage = currentPage == null || currentPage < 1 ? 1 : currentPage;
		int safeSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
		long offsetValue = (long) (safePage - 1) * safeSize;
		int offset = offsetValue > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) offsetValue;
		App condition = app == null ? new App() : app;
		String tenantId = getTenantId();
		AppDto result = new AppDto();
		result.setApps(baseMapper.queryAllAppTemplate(safeSize, offset, condition.getName(),
				condition.getIndustryId(), condition.getSceneId(), condition.getFramework(), orderBy,
				condition.getCreatedBy(), tenantId));
		result.setTotal(baseMapper.queryAppTemplateTotal(tenantId));
		return result;
	}

	@Override
	public App queryAppTemplateById(Integer id) {
		App app = baseMapper.queryAppTemplateById(id, getTenantId());
		if (app == null) {
			throw error(ExceptionEnum.CM009);
		}
		return app;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public App createAppByTemplate(App app) {
		if (app == null || app.getId() == null) {
			throw error(ExceptionEnum.CM002);
		}
		int templateId = app.getId();
		if (baseMapper.queryAppTemplateById(templateId, getTenantId()) == null) {
			throw error(ExceptionEnum.CM009);
		}
		validateNewAppCode(app);
		app.setId(null);
		app.setIsTemplate(false);
		app.setSetTemplateBy(null);
		if (baseMapper.createApp(app) < 1 || app.getId() == null) {
			throw error(ExceptionEnum.CM001);
		}
		copyData(templateId, app.getId());
		App created = baseMapper.queryAppById(app.getId(), getTenantId());
		if (created == null) {
			throw error(ExceptionEnum.CM009);
		}
		return created;
	}

	private void validateNewAppCode(App app) {
		String code = app == null ? null : app.getCode();
		if (code == null || !code.trim().matches("^[A-Za-z0-9_-]{1,20}$")) {
			throw error(ExceptionEnum.CM002);
		}
		App condition = new App();
		condition.setCode(code.trim());
		condition.setTenantId(getTenantId());
		if (!baseMapper.queryAppByCondition(condition).isEmpty()) {
			throw error(ExceptionEnum.CM003);
		}
		app.setCode(code.trim());
	}

	@Override
	public Integer updateAppTemplateById(App app) {
		return baseMapper.updateAppById(app);
	}

	private void copyData(int templateId, int appId) {
		copyPages(templateId, appId);
		copyPageHistories(templateId, appId);
		copyAppExtensions(templateId, appId);
		copyDatasources(templateId, appId);
		copyI18nEntries(templateId, appId);
		copyModels(templateId, appId);
	}

	private void copyPages(int templateId, int appId) {
		List<Page> pages = pageMapper.queryPageByApp(templateId);
		if (pages == null) {
			return;
		}
		for (Page page : pages) {
			clearAudit(page);
			page.setApp(appId);
			pageService.createPage(page);
		}
	}

	private void copyPageHistories(int templateId, int appId) {
		List<PageHistory> histories = pageHistoryMapper.queryPageHistoryByAppId(templateId);
		if (histories == null) {
			return;
		}
		for (PageHistory history : histories) {
			history.setId(null);
			history.setCreatedBy(null);
			history.setCreatedTime(null);
			history.setLastUpdatedBy(null);
			history.setLastUpdatedTime(null);
			history.setApp(appId);
			pageHistoryMapper.createPageHistory(history);
		}
	}

	private void copyAppExtensions(int templateId, int appId) {
		App targetApp = baseMapper.queryAppById(appId, getTenantId());
		if (targetApp == null) {
			throw error(ExceptionEnum.CM009);
		}
		AppExtension condition = new AppExtension();
		condition.setApp(templateId);
		List<AppExtension> extensions = appExtensionMapper.queryAppExtensionByCondition(condition);
		if (extensions == null) {
			return;
		}
			for (AppExtension extension : extensions) {
				clearAudit(extension);
				extension.setApp(appId);
				extension.setPlatformId(targetApp.getPlatformId());
				extension.setTenantId(targetApp.getTenantId());
				extension.setRenterId(targetApp.getRenterId());
				extension.setSiteId(targetApp.getSiteId());
				appExtensionMapper.createAppExtension(extension);
			}
	}

	private void copyDatasources(int templateId, int appId) {
		Datasource condition = new Datasource();
		condition.setApp(templateId);
		List<Datasource> datasources = datasourceMapper.queryDatasourceByCondition(condition);
		if (datasources == null) {
			return;
		}
		for (Datasource datasource : datasources) {
			clearAudit(datasource);
			datasource.setApp(appId);
			datasourceMapper.createDatasource(datasource);
		}
	}

	private void copyI18nEntries(int templateId, int appId) {
		List<I18nEntryDto> entries = i18nEntryMapper.findI18nEntriesByHostandHostType(templateId, "app");
		if (entries == null) {
			return;
		}
		for (I18nEntryDto source : entries) {
			I18nEntry target = new I18nEntry();
			BeanUtils.copyProperties(source, target);
			target.setId(null);
			target.setKey(source.getKey() + "_" + UUID.randomUUID().toString());
			target.setHost(appId);
			target.setCreatedBy(null);
			target.setCreatedTime(null);
			target.setLastUpdatedBy(null);
			target.setLastUpdatedTime(null);
			i18nEntryMapper.createI18nEntry(target);
		}
	}

	private void copyModels(int templateId, int appId) {
		Model condition = new Model();
		condition.setAppId(templateId);
		List<Model> models = modelMapper.queryModelByCondition(condition);
		if (models == null) {
			return;
		}
		for (Model model : models) {
			clearAudit(model);
			model.setAppId(appId);
			modelMapper.createModel(model);
		}
	}

	private void clearAudit(com.xiongx9527.fw.plus.tinyengine.common.model.BaseEntity entity) {
		entity.setId(null);
		entity.setCreatedBy(null);
		entity.setCreatedTime(null);
		entity.setLastUpdatedBy(null);
		entity.setLastUpdatedTime(null);
	}

	private String getTenantId() {
		TinyEngineUser user = userContext.getCurrentUser();
		return user == null ? null : user.getTenantId();
	}

	private ServiceException error(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}
}
