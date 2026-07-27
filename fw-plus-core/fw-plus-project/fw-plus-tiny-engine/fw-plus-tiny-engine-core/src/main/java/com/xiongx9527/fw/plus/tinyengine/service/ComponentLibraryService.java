package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ComponentLibrary;

import java.util.List;

public interface ComponentLibraryService extends IService<ComponentLibrary> {

	List<ComponentLibrary> queryAllComponentLibrary();
}
