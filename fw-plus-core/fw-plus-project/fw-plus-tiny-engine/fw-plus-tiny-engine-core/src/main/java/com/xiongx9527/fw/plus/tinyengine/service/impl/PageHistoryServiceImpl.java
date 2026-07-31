package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.model.PageQueryVo;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageHistoryMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.PublishedPageVo;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageHistory;
import com.xiongx9527.fw.plus.tinyengine.service.PageHistoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PageHistoryServiceImpl extends ServiceImpl<PageHistoryMapper, PageHistory> implements PageHistoryService {

	private static final String DEFAULT_VERSION = "0";

	@Override
	public List<PageHistory> findAllPageHistory() {
		return baseMapper.queryAllPageHistory();
	}

	@Override
	public PageHistory findPageHistoryById(Integer historyId) {
		return baseMapper.queryPageHistoryById(historyId);
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
}
