package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BundleDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BundleResultDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.CustComponentDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.FileResult;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Component;

import java.util.List;

public interface ComponentService extends IService<Component> {

	List<Component> findAllComponent();
	Component findComponentById(Integer id);
	List<Component> findComponentByCondition(Component component);
	Integer deleteComponentById(Integer id);
	Integer updateComponentById(Component component);
	Integer createComponent(Component component);
	BundleResultDto parseBundle(BundleDto bundleDto);
	FileResult bulkCreate(List<Component> componentList);
	FileResult custComponentBatchCreate(CustComponentDto custComponentDto);
}
