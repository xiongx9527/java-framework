package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.MaterialMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Material;
import com.xiongx9527.fw.plus.tinyengine.service.MaterialService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {

	@Override
	public List<Material> queryAllMaterial() {
		return baseMapper.queryAllMaterial();
	}

	@Override
	public Material queryMaterialById(Integer id) {
		return baseMapper.queryMaterialById(id);
	}

	@Override
	public List<Material> queryMaterialByCondition(Material material) {
		return baseMapper.queryMaterialByCondition(material);
	}

	@Override
	public Material deleteMaterialById(Integer id) {
		Material existing = queryMaterialById(id);
		if (baseMapper.deleteMaterialById(id) != 1) {
			throw failure();
		}
		return existing;
	}

	@Override
	public Material updateMaterialById(Material material) {
		if (material == null || material.getId() == null || baseMapper.updateMaterialById(material) != 1) {
			throw failure();
		}
		return queryMaterialById(material.getId());
	}

	@Override
	public Material createMaterial(Material material) {
		if (material == null || baseMapper.createMaterial(material) != 1) {
			throw failure();
		}
		return material;
	}

	private ServiceException failure() {
		return new ServiceException(ExceptionEnum.CM008.getResultCode(), ExceptionEnum.CM008.getResultMsg());
	}
}
