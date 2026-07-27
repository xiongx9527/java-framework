package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ResourceGroup;

import java.util.List;

public interface ResourceGroupService extends IService<ResourceGroup> {

	List<ResourceGroup> queryAllResourceGroupAndResource();
}
