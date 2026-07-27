package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockHistory;

import java.util.List;

public interface BlockHistoryService extends IService<BlockHistory> {

	List<BlockHistory> findAllBlockHistory();
}
