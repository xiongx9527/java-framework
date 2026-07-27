package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Platform;

import java.util.List;

public interface PlatformService extends IService<Platform> {

	List<Platform> queryAllPlatform();
}
