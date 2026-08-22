package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageHistory;
import com.xiongx9527.fw.plus.tinyengine.service.PageHistoryService;
import com.xiongx9527.fw.plus.tinyengine.service.AppRoleService;
import com.xiongx9527.fw.plus.tinyengine.service.PageService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppPermission;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import com.xiongx9527.fw.plus.tinyengine.support.hook.NoopTinyEngineSaveHook;
import com.xiongx9527.fw.plus.tinyengine.support.hook.TinyEngineSaveHook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class PageServiceImpl extends MapperServiceSupport<PageMapper> implements PageService {

	@Autowired
	private AppMapper appMapper;
	@Autowired
	private PageHistoryService pageHistoryService;
	@Autowired
	private TinyEngineUserContext userContext;
	@Autowired
	private AppRoleService appRoleService;

	private TinyEngineSaveHook saveHook = new NoopTinyEngineSaveHook();

	@Autowired(required = false)
	public void setSaveHook(TinyEngineSaveHook saveHook) {
		if (saveHook != null) {
			this.saveHook = saveHook;
		}
	}

	@Override
	public List<Page> queryAllPage(Integer appId) {
		if (appId != null) {
			require(appId, AppPermission.READ);
		}
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
			require(page == null ? null : page.getApp(), AppPermission.READ);
			if (Boolean.TRUE.equals(page.getIsPage())) {
				addIsHome(page);
			}
		}
		return pages;
	}

	@Override
	public Page queryPageById(Integer id) {
		Page page = baseMapper.queryPageById(id);
		if (page != null) {
			require(page.getApp(), AppPermission.READ);
		}
		return page != null && Boolean.TRUE.equals(page.getIsPage()) ? addIsHome(page) : page;
	}

	@Override
	public List<Page> queryPageByCondition(Page page) {
		if (page != null && page.getApp() != null) {
			require(page.getApp(), AppPermission.READ);
		}
		List<Page> pages = baseMapper.queryPageByCondition(page);
		if (pages != null) {
			for (Page result : pages) {
				require(result == null ? null : result.getApp(), AppPermission.READ);
			}
		}
		return pages;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Page delPage(Integer id) {
		Page page = baseMapper.queryPageById(id);
		if (!belongsToCurrentTenant(page)) {
			throw error(ExceptionEnum.CM009);
		}
		require(page.getApp(), AppPermission.DEVELOP);
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
	public Page copyPage(Page request) {
		if (request == null || request.getId() == null) {
			throw error(ExceptionEnum.CM002);
		}
		Page source = baseMapper.queryPageById(request.getId());
		if (!belongsToCurrentTenant(source)) {
			throw error(ExceptionEnum.CM009);
		}
		require(source.getApp(), AppPermission.DEVELOP);
		request.setId(null);
		request.setApp(source.getApp());
		request.setIsPage(source.getIsPage());
		request.setTenantId(null);
		request.setRenterId(null);
		request.setSiteId(null);
		request.setCreatedBy(null);
		request.setLastUpdatedBy(null);
		request.setCreatedTime(null);
		request.setLastUpdatedTime(null);
		request.setLatestVersion(null);
		request.setLatestHistoryId(null);
		return createPage(request);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Page restorePageHistory(Integer historyId) {
		PageHistory history = pageHistoryService.findPageHistoryById(historyId);
		if (history == null || history.getPage() == null) {
			throw error(ExceptionEnum.CM009);
		}
		Page current = baseMapper.queryPageById(history.getPage());
		if (!belongsToCurrentTenant(current)) {
			throw error(ExceptionEnum.CM009);
		}
		require(current.getApp(), AppPermission.DEVELOP);
		Page restored = new Page();
		BeanUtils.copyProperties(history, restored);
		restored.setId(current.getId());
		restored.setApp(current.getApp());
		restored.setTenantId(current.getTenantId());
		restored.setRenterId(current.getRenterId());
		restored.setSiteId(current.getSiteId());
		restored.setCreatedBy(current.getCreatedBy());
		restored.setCreatedTime(current.getCreatedTime());
		restored.setMessage("还原页面");
		return updatePage(restored);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Page createPage(Page page) {
		if (page == null || page.getApp() == null) {
			throw error(ExceptionEnum.CM002);
		}
		require(page.getApp(), AppPermission.DEVELOP);
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
		prepareCreateAudit(page);
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
		require(page.getApp(), AppPermission.DEVELOP);
		page.setDepth(getDepth(page.getParentId()) + 1);
		page.setGroup("staticPages");
		page.setIsDefault(false);
		page.setIsBody(true);
		page.setOccupierBy(getCurrentUserId());
		prepareCreateAudit(page);
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
		require(current.getApp(), AppPermission.DEVELOP);
		Boolean requestedHome = page.getIsHome();
		preserveScope(page, current);
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
		Page current = baseMapper.queryPageById(page.getId());
		require(current == null ? null : current.getApp(), AppPermission.DEVELOP);
		if (current == null) {
			throw error(ExceptionEnum.CM009);
		}
		preserveScope(page, current);
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
		require(page == null ? null : page.getApp(), AppPermission.DEVELOP);
		applyPersistedAppScope(page);
		boolean saved = baseMapper.insert(page) == 1;
		if (saved) {
			saveHook.afterSave(page);
		}
		return saved;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateById(Page page) {
		Page current = page == null ? null : baseMapper.queryPageById(page.getId());
		require(current == null ? null : current.getApp(), AppPermission.DEVELOP);
		if (page != null && current != null) {
			preserveScope(page, current);
		}
		boolean updated = baseMapper.updateById(page) == 1;
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
		app.setTenantId(getTenantId());
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
		if (history.getId() == null || !Integer.valueOf(1).equals(baseMapper.updatePageHistoryPointer(page.getId(),
				history.getVersion(), history.getId(), page.getTenantId()))) {
			throw error(ExceptionEnum.CM001);
		}
		page.setLatestHistoryId(history.getId());
		page.setLatestVersion(history.getVersion());
	}

	private void preserveScope(Page target, Page current) {
		target.setApp(current.getApp());
		target.setTenantId(current.getTenantId());
		target.setRenterId(current.getRenterId());
		target.setSiteId(current.getSiteId());
	}

	private void applyPersistedAppScope(Page page) {
		if (page == null || page.getApp() == null || appMapper == null) {
			return;
		}
		App app = appMapper.queryAppById(page.getApp(), getTenantId());
		if (app == null) {
			throw error(ExceptionEnum.CM009);
		}
		page.setTenantId(app.getTenantId());
		page.setRenterId(app.getRenterId());
		page.setSiteId(app.getSiteId());
	}

	private void prepareCreateAudit(Page page) {
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		String userId = user == null ? null : user.getUserId();
		LocalDateTime now = LocalDateTime.now();
		if (page.getCreatedBy() == null) {
			page.setCreatedBy(userId);
		}
		if (page.getLastUpdatedBy() == null) {
			page.setLastUpdatedBy(userId);
		}
		if (page.getCreatedTime() == null) {
			page.setCreatedTime(now);
		}
		if (page.getLastUpdatedTime() == null) {
			page.setLastUpdatedTime(now);
		}
		if (user != null) {
			if (page.getTenantId() == null) {
				page.setTenantId(user.getTenantId());
			}
			if (page.getRenterId() == null) {
				page.setRenterId(user.getRenterId());
			}
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

	private boolean belongsToCurrentTenant(Page page) {
		if (page == null) {
			return false;
		}
		String tenantId = getTenantId();
		return page.getTenantId() == null || tenantId == null || tenantId.equals(page.getTenantId());
	}

	private ServiceException error(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}

	private void require(Integer appId, AppPermission permission) {
		if (appRoleService != null) {
			appRoleService.requirePermission(appId, permission);
		}
	}
}
