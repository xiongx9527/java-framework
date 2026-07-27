package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
}
