package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageHistory;
import com.xiongx9527.fw.plus.tinyengine.service.PageHistoryService;
import com.xiongx9527.fw.plus.tinyengine.service.PageService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import com.xiongx9527.fw.plus.tinyengine.support.hook.NoopTinyEngineSaveHook;
import com.xiongx9527.fw.plus.tinyengine.support.hook.TinyEngineSaveHook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class PageServiceImpl extends ServiceImpl<PageMapper, Page> implements PageService {

	@Autowired
	private AppMapper appMapper;
	@Autowired
	private PageHistoryService pageHistoryService;
	@Autowired
	private TinyEngineUserContext userContext;

	private TinyEngineSaveHook saveHook = new NoopTinyEngineSaveHook();

	@Autowired(required = false)
	public void setSaveHook(TinyEngineSaveHook saveHook) {
		if (saveHook != null) {
			this.saveHook = saveHook;
		}
	}

	@Override
	public List<Page> queryAllPage(Integer appId) {
		List<Page> pages = appId == null ? baseMapper.queryAllPage() : baseMapper.queryPageByApp(appId);
		if (pages == null) {
			return null;
		}
		if (appId != null) {
			int homePageId = getAppHomePageId(appId);
			for (Page page : pages) {
				if (Boolean.TRUE.equals(page.getIsPage())) {
					page.setIsHome(page.getId() != null && page.getId().intValue() == homePageId);
				}
			}
			return pages;
		}
		for (Page page : pages) {
			if (Boolean.TRUE.equals(page.getIsPage())) {
				addIsHome(page);
			}
		}
		return pages;
	}

	@Override
	public Page queryPageById(Integer id) {
		Page page = baseMapper.queryPageById(id);
		return page != null && Boolean.TRUE.equals(page.getIsPage()) ? addIsHome(page) : page;
	}

	@Override
	public List<Page> queryPageByCondition(Page page) {
		return baseMapper.queryPageByCondition(page);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Page delPage(Integer id) {
		Page page = baseMapper.queryPageById(id);
		if (page == null) {
			throw error(ExceptionEnum.CM009);
		}
		if (!Boolean.TRUE.equals(page.getIsPage())) {
			Page childCondition = new Page();
			childCondition.setParentId(String.valueOf(id));
			List<Page> children = baseMapper.queryPageByCondition(childCondition);
			if (children != null && !children.isEmpty()) {
				throw new ServiceException(ExceptionEnum.CM002.getResultCode(), "Folder is not empty");
			}
		}
		if (!Integer.valueOf(1).equals(baseMapper.deletePageIfAvailable(id, getCurrentUserId()))) {
			throw error(ExceptionEnum.CM001);
		}
		return page;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Page createPage(Page page) {
		if (page == null || page.getApp() == null) {
			throw error(ExceptionEnum.CM002);
		}
		if (Boolean.TRUE.equals(page.getIsHome()) && !"0".equals(page.getParentId())) {
			throw new ServiceException(ExceptionEnum.CM002.getResultCode(),
					"Homepage can only be set in the root directory");
		}
		if (page.getGroup() != null && Arrays.asList("static", "public").contains(page.getGroup())) {
			page.setGroup(page.getGroup() + "Pages");
		}
		page.setOccupierBy(getCurrentUserId());
		page.setIsDefault(false);
		page.setDepth(0);
		ensurePageDoesNotExist(page);
		if (baseMapper.createPage(page) < 1 || page.getId() == null) {
			throw error(ExceptionEnum.CM001);
		}
		Page created = baseMapper.queryPageById(page.getId());
		if (created == null) {
			throw error(ExceptionEnum.CM009);
		}
		if (Boolean.TRUE.equals(page.getIsHome())) {
			setAppHomePage(page.getApp(), created.getId());
			created.setIsHome(true);
		}
		else if (Boolean.TRUE.equals(created.getIsPage())) {
			created.setIsHome(false);
		}
		createHistory(created, page.getMessage(), "0");
		saveHook.afterSave(created);
		return created;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Page createFolder(Page page) {
		if (page == null || page.getParentId() == null) {
			throw error(ExceptionEnum.CM002);
		}
		page.setDepth(getDepth(page.getParentId()) + 1);
		page.setGroup("staticPages");
		page.setIsDefault(false);
		page.setIsBody(true);
		page.setOccupierBy(getCurrentUserId());
		ensurePageDoesNotExist(page);
		if (baseMapper.createPage(page) < 1 || page.getId() == null) {
			throw error(ExceptionEnum.CM001);
		}
		Page created = baseMapper.queryPageById(page.getId());
		if (created == null) {
			throw error(ExceptionEnum.CM009);
		}
		return created;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Page updatePage(Page page) {
		if (page == null || page.getId() == null) {
			throw error(ExceptionEnum.CM002);
		}
		Page current = queryPageById(page.getId());
		if (current == null) {
			throw error(ExceptionEnum.CM009);
		}
		Boolean requestedHome = page.getIsHome();
		if (Boolean.TRUE.equals(requestedHome) && !"0".equals(page.getParentId())) {
			throw new ServiceException(ExceptionEnum.CM002.getResultCode(), "isHome parameter error");
		}
		if (requestedHome != null) {
			if (requestedHome) {
				setAppHomePage(current.getApp(), page.getId());
			}
			else if (Boolean.TRUE.equals(current.getIsHome())) {
				setAppHomePage(current.getApp(), 0);
			}
		}
		if (baseMapper.updatePageById(page) < 1) {
			throw error(ExceptionEnum.CM001);
		}
		Page updated = queryPageById(page.getId());
		String version = pageHistoryService.selectMaxVersionOfPageHistory(updated.getName(), updated.getApp());
		createHistory(updated, page.getMessage(), version);
		saveHook.afterSave(updated);
		return updated;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Page update(Page page) {
		if (page == null || page.getId() == null) {
			throw error(ExceptionEnum.CM002);
		}
		if (page.getParentId() != null) {
			page.setDepth(getDepth(page.getParentId()) + 1);
		}
		if (baseMapper.updatePageById(page) < 1) {
			throw error(ExceptionEnum.CM001);
		}
		Page updated = baseMapper.queryPageById(page.getId());
		if (updated == null) {
			throw error(ExceptionEnum.CM009);
		}
		return updated;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean save(Page page) {
		boolean saved = super.save(page);
		if (saved) {
			saveHook.afterSave(page);
		}
		return saved;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateById(Page page) {
		boolean updated = super.updateById(page);
		if (updated) {
			saveHook.afterSave(page);
		}
		return updated;
	}

	public Page addIsHome(Page page) {
		if (page != null) {
			int homePageId = getAppHomePageId(page.getApp());
			page.setIsHome(page.getId() != null && page.getId().intValue() == homePageId);
		}
		return page;
	}

	public int getAppHomePageId(int appId) {
		App app = appMapper.queryAppById(appId, getTenantId());
		if (app == null) {
			throw new ServiceException(ExceptionEnum.CM340.getResultCode(), "App not found for ID: " + appId);
		}
		return app.getHomePage() == null ? 0 : app.getHomePage();
	}

	private void setAppHomePage(int appId, int pageId) {
		App app = new App();
		app.setId(appId);
		app.setHomePage(pageId);
		if (appMapper.updateAppById(app) < 1) {
			throw error(ExceptionEnum.CM001);
		}
	}

	private int getDepth(String parentId) {
		int parent;
		try {
			parent = Integer.parseInt(parentId);
		}
		catch (NumberFormatException exception) {
			throw new ServiceException(ExceptionEnum.CM342.getResultCode(), ExceptionEnum.CM342.getResultMsg());
		}
		if (parent == 0) {
			return 0;
		}
		Page parentPage = baseMapper.queryPageById(parent);
		if (parentPage == null || parentPage.getDepth() == null) {
			throw error(ExceptionEnum.CM009);
		}
		return parentPage.getDepth();
	}

	private void ensurePageDoesNotExist(Page page) {
		Page condition = new Page();
		condition.setName(page.getName());
		condition.setApp(page.getApp());
		List<Page> pages = baseMapper.queryPageByCondition(condition);
		if (pages != null && !pages.isEmpty()) {
			throw error(ExceptionEnum.CM003);
		}
	}

	private void createHistory(Page page, String message, String version) {
		PageHistory history = new PageHistory();
		BeanUtils.copyProperties(page, history);
		history.setPage(page.getId());
		history.setId(null);
		history.setMessage(message);
		history.setIsPublished(false);
		history.setVersion(version == null ? "0" : version);
		if (pageHistoryService.createPageHistory(history) < 1) {
			throw error(ExceptionEnum.CM001);
		}
	}

	private String getCurrentUserId() {
		TinyEngineUser user = userContext.getCurrentUser();
		return user == null ? null : user.getUserId();
	}

	private String getTenantId() {
		TinyEngineUser user = userContext.getCurrentUser();
		return user == null ? null : user.getTenantId();
	}

	private ServiceException error(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}
}
