package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockHistoryMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockHistory;
import com.xiongx9527.fw.plus.tinyengine.service.BlockHistoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlockHistoryServiceImpl extends ServiceImpl<BlockHistoryMapper, BlockHistory> implements BlockHistoryService {

	@Override
	public List<BlockHistory> findAllBlockHistory() {
		return baseMapper.queryAllBlockHistory();
	}
}
