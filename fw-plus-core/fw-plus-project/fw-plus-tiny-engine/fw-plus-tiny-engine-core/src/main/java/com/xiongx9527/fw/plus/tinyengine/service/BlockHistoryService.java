package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockHistory;

import java.util.List;

public interface BlockHistoryService extends IService<BlockHistory> {

	List<BlockHistory> findAllBlockHistory();
	BlockHistory findBlockHistoryById(Integer id);
	List<BlockHistory> findBlockHistoryByCondition(BlockHistory blockHistory);
	Integer deleteBlockHistoryById(Integer id);
	Integer updateBlockHistoryById(BlockHistory blockHistory);
	Integer createBlockHistory(BlockHistory blockHistory);
}
