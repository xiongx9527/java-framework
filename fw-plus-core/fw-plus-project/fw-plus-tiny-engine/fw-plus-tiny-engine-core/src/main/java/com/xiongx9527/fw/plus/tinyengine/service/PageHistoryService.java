package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiongx9527.fw.plus.tinyengine.common.model.PageQueryVo;
import com.xiongx9527.fw.plus.tinyengine.model.dto.PublishedPageVo;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageHistory;

import java.util.List;

public interface PageHistoryService {

	List<PageHistory> findAllPageHistory();

	PageHistory findPageHistoryById(Integer historyId);

	List<PageHistory> findPageHistories(Integer pageId);

	List<PageHistory> findPageHistoryByCondition(PageHistory pageHistory);

	Integer deletePageHistoryById(Integer id);

	Integer updatePageHistoryById(PageHistory pageHistory);

	Integer createPageHistory(PageHistory pageHistory);

	List<PageHistory> findPageHistoryByName(String name, Integer app);

	IPage<PublishedPageVo> findLatestPublishPage(PageQueryVo<PublishedPageVo> pageQueryVo);

	String selectMaxVersionOfPageHistory(String name, Integer app);
}
