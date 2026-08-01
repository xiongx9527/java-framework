package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.ResourceGroupResourceMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.ResourceGroupMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.ResourceMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Resource;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ResourceGroup;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ResourceGroupResource;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceGroupService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourceGroupServiceImpl extends ServiceImpl<ResourceGroupMapper, ResourceGroup> implements ResourceGroupService {

	private final TinyEngineUserContext userContext;
	@Autowired
	private ResourceGroupResourceMapper resourceGroupResourceMapper;
	@Autowired
	private ResourceMapper resourceMapper;

	public ResourceGroupServiceImpl(TinyEngineUserContext userContext) {
		this.userContext = userContext;
	}

	@Override
	public List<ResourceGroup> queryAllResourceGroupAndResource() {
		return baseMapper.queryAllResourceGroupAndResource(currentUserId());
	}

	@Override
	public List<ResourceGroup> queryResourceGroupByAppId(Integer appId) {
		return baseMapper.queryResourceGroupByAppId(appId, currentUserId());
	}

	@Override
	public ResourceGroup queryResourceGroupById(Integer id) {
		return baseMapper.queryResourceGroupById(id, currentUserId());
	}

	@Override
	public List<ResourceGroup> queryResourceGroupByCondition(ResourceGroup resourceGroup) {
		return baseMapper.queryResourceGroupByCondition(resourceGroup);
	}

	@Override
	public ResourceGroup deleteResourceGroupById(Integer id) {
		ResourceGroup group = queryResourceGroupById(id);
		if (baseMapper.deleteResourceGroupById(id) != 1) {
			throw failure(ExceptionEnum.CM008);
		}
		resourceGroupResourceMapper.deleteResourceGroupResourceByGroupId(id);
		return group;
	}

	@Override
	public ResourceGroup updateResourceGroupById(ResourceGroup resourceGroup) {
		List<Resource> resources = resourceGroup.getResources() == null
				? new ArrayList<Resource>() : resourceGroup.getResources();
		List<ResourceGroupResource> existing =
				resourceGroupResourceMapper.findResourceGroupResourceByResourceGroupId(resourceGroup.getId());
		List<Integer> oldIds = existing.stream().map(ResourceGroupResource::getResourceId).collect(Collectors.toList());
		List<Integer> newIds = resources.stream().map(Resource::getId).collect(Collectors.toList());
		for (Integer oldId : oldIds) {
			if (!newIds.contains(oldId)) {
				ResourceGroupResource relation = resourceGroupResourceMapper
						.findResourceGroupResourceByResourceGroupIdAndResourceId(resourceGroup.getId(), oldId);
				if (relation != null) {
					resourceGroupResourceMapper.deleteById(relation.getId());
				}
				resourceMapper.deleteResourceById(oldId);
			}
		}
		for (Integer newId : newIds) {
			if (!oldIds.contains(newId)) {
				ResourceGroupResource relation = new ResourceGroupResource();
				relation.setResourceGroupId(resourceGroup.getId());
				relation.setResourceId(newId);
				resourceGroupResourceMapper.createResourceGroupResource(relation);
			}
		}
		if (baseMapper.updateResourceGroupById(resourceGroup) != 1) {
			throw failure(ExceptionEnum.CM008);
		}
		return queryResourceGroupById(resourceGroup.getId());
	}

	@Override
	public ResourceGroup createResourceGroup(ResourceGroup resourceGroup) {
		QueryWrapper<ResourceGroup> query = new QueryWrapper<ResourceGroup>()
				.eq("name", resourceGroup.getName()).eq("app_id", resourceGroup.getAppId());
		if (baseMapper.selectOne(query) != null) {
			throw failure(ExceptionEnum.CM003);
		}
		if (baseMapper.createResourceGroup(resourceGroup) != 1) {
			throw failure(ExceptionEnum.CM003);
		}
		return baseMapper.selectById(resourceGroup.getId());
	}

	private String currentUserId() {
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		return user == null ? null : user.getUserId();
	}

	private ServiceException failure(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}
}
