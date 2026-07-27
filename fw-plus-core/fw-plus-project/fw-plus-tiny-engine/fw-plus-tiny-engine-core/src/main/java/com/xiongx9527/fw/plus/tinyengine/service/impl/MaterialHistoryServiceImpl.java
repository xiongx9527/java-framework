package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
}
