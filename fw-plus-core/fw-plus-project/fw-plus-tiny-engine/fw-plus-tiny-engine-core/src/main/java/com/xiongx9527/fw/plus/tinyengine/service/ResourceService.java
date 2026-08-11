package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.dto.ResourceRequestDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Resource;

import java.util.List;

public interface ResourceService extends IService<Resource> {

	List<Resource> queryAllResource();
	List<Resource> queryResourcesByNameAndDes(String name, String description);
	Resource queryResourceById(Integer id);
	Resource queryResourceByName(String name);
	List<Resource> queryResourceByResourceGroupId(Integer resourceGroupId);
	Resource deleteResourceById(Integer id);
	Resource updateResourceById(Resource resource);
	List<Resource> createResources(List<Resource> resources);
	Object uploadResource(ResourceRequestDto request);
}
