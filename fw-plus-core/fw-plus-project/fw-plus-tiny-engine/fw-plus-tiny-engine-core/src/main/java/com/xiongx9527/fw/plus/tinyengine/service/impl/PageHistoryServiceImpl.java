package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiongx9527.fw.plus.tinyengine.common.model.PageQueryVo;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageHistoryMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.PublishedPageVo;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageHistory;
import com.xiongx9527.fw.plus.tinyengine.service.PageHistoryService;
import com.xiongx9527.fw.plus.tinyengine.service.AppRoleService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppPermission;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PageHistoryServiceImpl extends MapperServiceSupport<PageHistoryMapper> implements PageHistoryService {

	private static final String DEFAULT_VERSION = "0";

	@Autowired
	private TinyEngineUserContext userContext;
	@Autowired
	private AppRoleService appRoleService;

	@Override
	public List<PageHistory> findAllPageHistory() {
		List<PageHistory> histories = baseMapper.queryAllPageHistory();
		requireAll(histories, AppPermission.READ);
		return histories;
	}

	@Override
	public PageHistory findPageHistoryById(Integer historyId) {
		PageHistory history = baseMapper.queryPageHistoryById(historyId);
		PageHistory result = belongsToCurrentTenant(history) ? history : null;
		require(result == null ? null : result.getApp(), AppPermission.READ);
		return result;
	}

	@Override
	public List<PageHistory> findPageHistories(Integer pageId) {
		PageHistory condition = new PageHistory();
		condition.setPage(pageId);
		condition.setTenantId(getTenantId());
		List<PageHistory> histories = baseMapper.queryPageHistoryByCondition(condition);
		requireAll(histories, AppPermission.READ);
		return histories;
	}

	@Override
	public List<PageHistory> findPageHistoryByCondition(PageHistory pageHistory) {
		require(pageHistory == null ? null : pageHistory.getApp(), AppPermission.READ);
		List<PageHistory> histories = baseMapper.queryPageHistoryByCondition(pageHistory);
		requireAll(histories, AppPermission.READ);
		return histories;
	}

	@Override
	public Integer deletePageHistoryById(Integer id) {
		PageHistory history = baseMapper.queryPageHistoryById(id);
		require(history == null ? null : history.getApp(), AppPermission.DEVELOP);
		return baseMapper.deletePageHistoryById(id);
	}

	@Override
	public Integer updatePageHistoryById(PageHistory pageHistory) {
		PageHistory stored = pageHistory == null || pageHistory.getId() == null ? null
				: baseMapper.queryPageHistoryById(pageHistory.getId());
		PageHistory current = belongsToCurrentTenant(stored) ? stored : null;
		require(current == null ? null : current.getApp(), AppPermission.DEVELOP);
		pageHistory.setApp(current.getApp());
		pageHistory.setTenantId(current.getTenantId());
		return baseMapper.updatePageHistoryById(pageHistory);
	}

	@Override
	public Integer createPageHistory(PageHistory pageHistory) {
		require(pageHistory == null ? null : pageHistory.getApp(), AppPermission.DEVELOP);
		pageHistory.setTenantId(getTenantId());
		pageHistory.setIsPublished(true);
		return baseMapper.createPageHistory(pageHistory);
	}

	@Override
	public List<PageHistory> findPageHistoryByName(String name, Integer app) {
		require(app, AppPermission.READ);
		return baseMapper.queryPageHistoryByName(name, app);
	}

	@Override
	public IPage<PublishedPageVo> findLatestPublishPage(PageQueryVo<PublishedPageVo> pageQueryVo) {
		IPage<PublishedPageVo> result = baseMapper.findLatestPublishPage(pageQueryVo.getPage(), pageQueryVo.getData());
		if (result != null && result.getRecords() != null) {
			for (PublishedPageVo page : result.getRecords()) {
				require(page == null ? null : page.getAppId(), AppPermission.READ);
			}
		}
		return result;
	}

	@Override
	public String selectMaxVersionOfPageHistory(String name, Integer app) {
		require(app, AppPermission.READ);
		List<PageHistory> histories = baseMapper.queryPageHistoryByName(name, app);
		if (histories == null || histories.isEmpty()) {
			return DEFAULT_VERSION;
		}
		return histories.get(histories.size() - 1).getVersion();
	}

	private boolean belongsToCurrentTenant(PageHistory history) {
		if (history == null) {
			return false;
		}
		String tenantId = getTenantId();
		return tenantId == null ? history.getTenantId() == null : tenantId.equals(history.getTenantId());
	}

	private String getTenantId() {
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		return user == null ? null : user.getTenantId();
	}

	private void require(Integer appId, AppPermission permission) {
		if (appRoleService != null) {
			appRoleService.requirePermission(appId, permission);
		}
	}

	private void requireAll(List<PageHistory> histories, AppPermission permission) {
		if (histories == null) {
			return;
		}
		for (PageHistory history : histories) {
			PageHistory current = belongsToCurrentTenant(history) ? history : null;
			require(current == null ? null : current.getApp(), permission);
		}
	}
}
