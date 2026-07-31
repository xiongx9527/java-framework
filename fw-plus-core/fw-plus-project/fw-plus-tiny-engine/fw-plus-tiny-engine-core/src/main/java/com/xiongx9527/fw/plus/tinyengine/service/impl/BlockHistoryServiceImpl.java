package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
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

	@Override
	public BlockHistory findBlockHistoryById(Integer id) {
		return baseMapper.queryBlockHistoryById(id);
	}

	@Override
	public List<BlockHistory> findBlockHistoryByCondition(BlockHistory blockHistory) {
		return baseMapper.queryBlockHistoryByCondition(blockHistory);
	}

	@Override
	public Integer deleteBlockHistoryById(Integer id) {
		return baseMapper.deleteBlockHistoryById(id);
	}

	@Override
	public Integer updateBlockHistoryById(BlockHistory blockHistory) {
		return baseMapper.updateBlockHistoryById(blockHistory);
	}

	@Override
	public Integer createBlockHistory(BlockHistory blockHistory) {
		return baseMapper.createBlockHistory(blockHistory);
	}
}
