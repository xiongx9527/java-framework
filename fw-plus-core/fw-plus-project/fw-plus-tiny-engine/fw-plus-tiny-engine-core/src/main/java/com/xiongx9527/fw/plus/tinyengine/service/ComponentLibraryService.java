package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ComponentLibrary;

import java.util.List;

public interface ComponentLibraryService extends IService<ComponentLibrary> {

	List<ComponentLibrary> queryAllComponentLibrary();
	ComponentLibrary queryComponentLibraryById(Integer id);
	List<ComponentLibrary> queryComponentLibraryByCondition(ComponentLibrary componentLibrary);
	ComponentLibrary deleteComponentLibraryById(Integer id);
	ComponentLibrary updateComponentLibraryById(ComponentLibrary componentLibrary);
	ComponentLibrary createComponentLibrary(ComponentLibrary componentLibrary);
}
