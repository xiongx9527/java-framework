package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Material;

import java.util.List;

public interface MaterialService extends IService<Material> {

	List<Material> findAllMaterial();
}
