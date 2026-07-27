package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.MaterialMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Material;
import com.xiongx9527.fw.plus.tinyengine.service.MaterialService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {

	@Override
	public List<Material> findAllMaterial() {
		return baseMapper.queryAllMaterial();
	}
}
