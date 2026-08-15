package com.xiongx9527.fw.plus.tinyengine.service;

import com.xiongx9527.fw.plus.tinyengine.model.entity.AppExtension;

import java.util.List;

public interface AppExtensionService {

	List<AppExtension> findAllAppExtension();
	AppExtension findAppExtensionById(Integer id);
	List<AppExtension> findAppExtensionByCondition(AppExtension appExtension);
	AppExtension deleteAppExtensionById(Integer id);
	AppExtension updateAppExtensionById(AppExtension appExtension);
	AppExtension createAppExtension(AppExtension appExtension);
}
