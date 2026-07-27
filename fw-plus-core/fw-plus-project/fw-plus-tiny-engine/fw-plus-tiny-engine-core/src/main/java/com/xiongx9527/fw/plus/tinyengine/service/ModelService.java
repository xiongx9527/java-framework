package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Model;

import java.util.List;

public interface ModelService extends IService<Model> {

	List<Model> queryModelByCondition(Model model);
}
