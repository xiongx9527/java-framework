package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;

import java.util.List;

public interface AppService extends IService<App> {

	List<App> queryAllApp();
}
