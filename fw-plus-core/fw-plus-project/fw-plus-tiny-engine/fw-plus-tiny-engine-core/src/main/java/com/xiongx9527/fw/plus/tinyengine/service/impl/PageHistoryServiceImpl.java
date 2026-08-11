package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.model.PageQueryVo;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageHistoryMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.PublishedPageVo;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageHistory;
import com.xiongx9527.fw.plus.tinyengine.service.PageHistoryService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PageHistoryServiceImpl extends ServiceImpl<PageHistoryMapper, PageHistory> implements PageHistoryService {

	private static final String DEFAULT_VERSION = "0";

	@Autowired
	private TinyEngineUserContext userContext;

	@Override
	public List<PageHistory> findAllPageHistory() {
		return baseMapper.queryAllPageHistory();
	}

	@Override
	public PageHistory findPageHistoryById(Integer historyId) {
		PageHistory history = baseMapper.queryPageHistoryById(historyId);
		return belongsToCurrentTenant(history) ? history : null;
	}

	@Override
	public List<PageHistory> findPageHistories(Integer pageId) {
		PageHistory condition = new PageHistory();
		condition.setPage(pageId);
		condition.setTenantId(getTenantId());
		return baseMapper.queryPageHistoryByCondition(condition);
	}

	@Override
	public List<PageHistory> findPageHistoryByCondition(PageHistory pageHistory) {
		return baseMapper.queryPageHistoryByCondition(pageHistory);
	}

	@Override
	public Integer deletePageHistoryById(Integer id) {
		return baseMapper.deletePageHistoryById(id);
	}

	@Override
	public Integer updatePageHistoryById(PageHistory pageHistory) {
		return baseMapper.updatePageHistoryById(pageHistory);
	}

	@Override
	public Integer createPageHistory(PageHistory pageHistory) {
		pageHistory.setIsPublished(true);
		return baseMapper.createPageHistory(pageHistory);
	}

	@Override
	public List<PageHistory> findPageHistoryByName(String name, Integer app) {
		return baseMapper.queryPageHistoryByName(name, app);
	}

	@Override
	public IPage<PublishedPageVo> findLatestPublishPage(PageQueryVo<PublishedPageVo> pageQueryVo) {
		return baseMapper.findLatestPublishPage(pageQueryVo.getPage(), pageQueryVo.getData());
	}

	@Override
	public String selectMaxVersionOfPageHistory(String name, Integer app) {
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
}
