package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Platform;

import java.util.List;

public interface PlatformService extends IService<Platform> {

	List<Platform> queryAllPlatform();
	Platform queryPlatformById(Integer id);
	List<Platform> queryPlatformByCondition(Platform platform);
	Platform deletePlatformById(Integer id);
	Platform updatePlatformById(Platform platform);
	Platform createPlatform(Platform platform);
}
