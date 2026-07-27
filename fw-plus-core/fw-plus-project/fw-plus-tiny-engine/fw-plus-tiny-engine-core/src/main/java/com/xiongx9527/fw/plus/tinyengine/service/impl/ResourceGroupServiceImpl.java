package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.ResourceGroupMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ResourceGroup;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceGroupService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceGroupServiceImpl extends ServiceImpl<ResourceGroupMapper, ResourceGroup> implements ResourceGroupService {

	private final TinyEngineUserContext userContext;

	public ResourceGroupServiceImpl(TinyEngineUserContext userContext) {
		this.userContext = userContext;
	}

	@Override
	public List<ResourceGroup> queryAllResourceGroupAndResource() {
		TinyEngineUser user = userContext.getCurrentUser();
		return baseMapper.queryAllResourceGroupAndResource(user == null ? null : user.getUserId());
	}
}
