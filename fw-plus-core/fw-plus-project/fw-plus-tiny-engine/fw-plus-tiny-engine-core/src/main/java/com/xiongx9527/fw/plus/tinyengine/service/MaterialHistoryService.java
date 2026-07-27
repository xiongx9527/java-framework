package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.MaterialHistory;

import java.util.List;

public interface MaterialHistoryService extends IService<MaterialHistory> {

	List<MaterialHistory> findAllMaterialHistory();
}
