package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api;

import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.service.AppService;
import com.xiongx9527.fw.plus.tinyengine.service.PageService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefaultTinyEngineApiFacade implements TinyEngineApiFacade {

	private final AppService appService;
	private final PageService pageService;

	public DefaultTinyEngineApiFacade(AppService appService, PageService pageService) {
		this.appService = appService;
		this.pageService = pageService;
	}

	@Override
	public AppDto appsPage(Integer currentPage, Integer pageSize, String orderBy, App app) {
		return appService.queryAllAppByPage(currentPage, pageSize, orderBy, app);
	}

	@Override
	public App createApp(App app) {
		return appService.createApp(app);
	}

	@Override
	public App appDetail(Integer id) {
		return appService.queryAppById(id);
	}

	@Override
	public Map<String, Object> appSchema(Integer id) {
		App app = appDetail(id);
		if (app == null) {
			return null;
		}
		Map<String, Object> config = app.getConfig();
		if (config == null) {
			return Collections.emptyMap();
		}
		return config;
	}

	@Override
	public Page createPage(Page page) {
		if (Boolean.FALSE.equals(page.getIsPage())) {
			return pageService.createFolder(page);
		}
		return pageService.createPage(page);
	}

	@Override
	public Page updatePage(Integer id, Page page) {
		page.setId(id);
		page.setCreatedTime(null);
		page.setLastUpdatedTime(null);
		page.setLastUpdatedBy(null);
		if (Boolean.FALSE.equals(page.getIsPage())) {
			return pageService.update(page);
		}
		return pageService.updatePage(page);
	}

	@Override
	public List<Page> pages(Integer appId) {
		return pageService.queryAllPage(appId);
	}

	@Override
	public Page pageDetail(Integer id) {
		return pageService.queryPageById(id);
	}
}
