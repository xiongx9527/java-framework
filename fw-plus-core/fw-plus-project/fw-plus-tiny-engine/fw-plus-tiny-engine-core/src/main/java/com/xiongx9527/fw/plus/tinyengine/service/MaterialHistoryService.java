package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.MaterialHistory;

import java.util.List;

public interface MaterialHistoryService extends IService<MaterialHistory> {

	List<MaterialHistory> findAllMaterialHistory();
	MaterialHistory findMaterialHistoryById(Integer id);
	List<MaterialHistory> findMaterialHistoryByCondition(MaterialHistory materialHistory);
	MaterialHistory deleteMaterialHistoryById(Integer id);
	MaterialHistory updateMaterialHistoryById(MaterialHistory materialHistory);
	MaterialHistory createMaterialHistory(MaterialHistory materialHistory);
}
