package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ResourceGroup;

import java.util.List;

public interface ResourceGroupService extends IService<ResourceGroup> {

	List<ResourceGroup> queryAllResourceGroupAndResource();
	List<ResourceGroup> queryResourceGroupByAppId(Integer appId);
	ResourceGroup queryResourceGroupById(Integer id);
	List<ResourceGroup> queryResourceGroupByCondition(ResourceGroup resourceGroup);
	ResourceGroup deleteResourceGroupById(Integer id);
	ResourceGroup updateResourceGroupById(ResourceGroup resourceGroup);
	ResourceGroup createResourceGroup(ResourceGroup resourceGroup);
}
