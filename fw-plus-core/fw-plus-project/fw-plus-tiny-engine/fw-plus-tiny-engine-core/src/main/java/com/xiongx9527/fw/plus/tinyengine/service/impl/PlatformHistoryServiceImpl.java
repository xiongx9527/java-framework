package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.PlatformHistoryMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PlatformHistory;
import com.xiongx9527.fw.plus.tinyengine.service.PlatformHistoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlatformHistoryServiceImpl extends ServiceImpl<PlatformHistoryMapper, PlatformHistory> implements PlatformHistoryService {

	@Override
	public List<PlatformHistory> queryAllPlatformHistory() {
		return baseMapper.queryAllPlatformHistory();
	}

	@Override
	public PlatformHistory queryPlatformHistoryById(Integer id) {
		return baseMapper.queryPlatformHistoryById(id);
	}

	@Override
	public List<PlatformHistory> queryPlatformHistoryByCondition(PlatformHistory platformHistory) {
		return baseMapper.queryPlatformHistoryByCondition(platformHistory);
	}

	@Override
	public PlatformHistory deletePlatformHistoryById(Integer id) {
		PlatformHistory history = queryPlatformHistoryById(id);
		if (history == null) {
			return null;
		}
		if (baseMapper.deletePlatformHistoryById(id) != 1) {
			throw failure(ExceptionEnum.CM008);
		}
		return history;
	}

	@Override
	public PlatformHistory updatePlatformHistoryById(PlatformHistory platformHistory) {
		if (platformHistory == null || platformHistory.getId() == null
				|| baseMapper.updatePlatformHistoryById(platformHistory) != 1) {
			throw failure(ExceptionEnum.CM008);
		}
		return queryPlatformHistoryById(platformHistory.getId());
	}

	@Override
	public PlatformHistory createPlatformHistory(PlatformHistory platformHistory) {
		if (platformHistory == null || platformHistory.getRefId() == null
				|| platformHistory.getName() == null || platformHistory.getName().isEmpty()
				|| platformHistory.getVersion() == null || platformHistory.getVersion().isEmpty()
				|| platformHistory.getMaterialHistoryId() == null) {
			throw failure(ExceptionEnum.CM002);
		}
		if (baseMapper.createPlatformHistory(platformHistory) != 1) {
			throw failure(ExceptionEnum.CM008);
		}
		return platformHistory;
	}

	private ServiceException failure(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}
}
