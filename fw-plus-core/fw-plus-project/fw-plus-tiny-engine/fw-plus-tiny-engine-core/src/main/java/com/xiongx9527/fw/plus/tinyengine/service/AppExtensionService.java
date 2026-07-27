package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AppExtension;

import java.util.List;

public interface AppExtensionService extends IService<AppExtension> {

	List<AppExtension> findAllAppExtension();
}
