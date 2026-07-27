package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PlatformHistory;

import java.util.List;

public interface PlatformHistoryService extends IService<PlatformHistory> {

	List<PlatformHistory> queryAllPlatformHistory();
	PlatformHistory queryPlatformHistoryById(Integer id);
	List<PlatformHistory> queryPlatformHistoryByCondition(PlatformHistory platformHistory);
	PlatformHistory deletePlatformHistoryById(Integer id);
	PlatformHistory updatePlatformHistoryById(PlatformHistory platformHistory);
	PlatformHistory createPlatformHistory(PlatformHistory platformHistory);
}
