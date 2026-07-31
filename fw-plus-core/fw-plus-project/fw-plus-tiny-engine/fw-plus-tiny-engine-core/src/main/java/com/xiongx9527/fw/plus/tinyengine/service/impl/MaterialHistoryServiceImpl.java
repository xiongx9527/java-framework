package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.MaterialHistoryMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.MaterialHistory;
import com.xiongx9527.fw.plus.tinyengine.service.MaterialHistoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaterialHistoryServiceImpl extends ServiceImpl<MaterialHistoryMapper, MaterialHistory> implements MaterialHistoryService {

	@Override
	public List<MaterialHistory> findAllMaterialHistory() {
		return baseMapper.queryAllMaterialHistory();
	}

	@Override
	public MaterialHistory findMaterialHistoryById(Integer id) {
		return baseMapper.queryMaterialHistoryById(id);
	}

	@Override
	public List<MaterialHistory> findMaterialHistoryByCondition(MaterialHistory materialHistory) {
		return baseMapper.queryMaterialHistoryByCondition(materialHistory);
	}

	@Override
	public MaterialHistory deleteMaterialHistoryById(Integer id) {
		MaterialHistory existing = findMaterialHistoryById(id);
		if (baseMapper.deleteMaterialHistoryById(id) != 1) {
			throw failure();
		}
		return existing;
	}

	@Override
	public MaterialHistory updateMaterialHistoryById(MaterialHistory materialHistory) {
		if (materialHistory == null || materialHistory.getId() == null
				|| baseMapper.updateMaterialHistoryById(materialHistory) != 1) {
			throw failure();
		}
		return findMaterialHistoryById(materialHistory.getId());
	}

	@Override
	public MaterialHistory createMaterialHistory(MaterialHistory materialHistory) {
		if (materialHistory == null || baseMapper.createMaterialHistory(materialHistory) != 1) {
			throw failure();
		}
		return materialHistory;
	}

	private ServiceException failure() {
		return new ServiceException(ExceptionEnum.CM008.getResultCode(), ExceptionEnum.CM008.getResultMsg());
	}
}
