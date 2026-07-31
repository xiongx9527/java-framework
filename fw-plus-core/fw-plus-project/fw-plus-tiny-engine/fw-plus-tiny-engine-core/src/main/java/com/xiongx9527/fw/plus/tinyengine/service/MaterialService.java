package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Material;

import java.util.List;

public interface MaterialService extends IService<Material> {

	List<Material> queryAllMaterial();
	Material queryMaterialById(Integer id);
	List<Material> queryMaterialByCondition(Material material);
	Material deleteMaterialById(Integer id);
	Material updateMaterialById(Material material);
	Material createMaterial(Material material);
}
