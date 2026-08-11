package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.common.util.TinyEngineFileSecurityUtils;
import com.xiongx9527.fw.plus.tinyengine.mapper.ResourceMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.ResourceGroupResourceMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.ResourceRequestDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Resource;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ResourceGroupResource;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceGroupService;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUnsupportedCapabilityException;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource> implements ResourceService {
	@Autowired
	private TinyEngineUserContext userContext;
	@Autowired
	private ResourceGroupResourceMapper resourceGroupResourceMapper;
	@Autowired
	private ResourceGroupService resourceGroupService;

	@Override
	public List<Resource> queryAllResource() {
		return baseMapper.queryAllResource();
	}

	@Override
	public List<Resource> queryResourcesByNameAndDes(String name, String description) {
		return baseMapper.findResourcesByNameAndDes(name, description);
	}

	@Override
	public Resource queryResourceById(Integer id) {
		return baseMapper.queryResourceById(id);
	}

	@Override
	public Resource queryResourceByName(String name) {
		QueryWrapper<Resource> query = new QueryWrapper<Resource>();
		if (name != null && !name.startsWith("thumbnail_")) {
			query.eq("name", name);
		} else {
			query.eq("thumbnail_name", name);
		}
		return baseMapper.selectOne(query);
	}

	@Override
	public List<Resource> queryResourceByResourceGroupId(Integer resourceGroupId) {
		return baseMapper.findResourceByResourceGroupId(resourceGroupId, currentUserId());
	}

	@Override
	public Resource deleteResourceById(Integer id) {
		Resource resource = queryResourceById(id);
		if (baseMapper.deleteResourceById(id) != 1) {
			throw failure();
		}
		return resource;
	}

	@Override
	public Resource updateResourceById(Resource resource) {
		if (resource == null || resource.getId() == null || baseMapper.updateResourceById(resource) != 1) {
			throw failure();
		}
		return queryResourceById(resource.getId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public List<Resource> createResources(List<Resource> resources) {
		if (resources == null || resources.isEmpty()) {
			return Collections.emptyList();
		}
		for (Resource resource : resources) {
			if (resource == null || resource.getResourceGroupId() == null
					|| resourceGroupService.queryResourceGroupById(resource.getResourceGroupId()) == null) {
				throw createFailure();
			}
		}
		for (Resource resource : resources) {
			applyCurrentScope(resource);
			if (!save(resource)) {
				throw createFailure();
			}
			if (resource.getId() == null) {
				throw createFailure();
			}
			ResourceGroupResource relation = new ResourceGroupResource();
			relation.setResourceId(resource.getId());
			relation.setResourceGroupId(resource.getResourceGroupId());
			if (!Integer.valueOf(1).equals(resourceGroupResourceMapper.createResourceGroupResource(relation))) {
				throw createFailure();
			}
		}
		return resources;
	}

	@Override
	public Object uploadResource(ResourceRequestDto request) {
		if (request == null) {
			throw new IllegalArgumentException("Resource upload request is required");
		}
		TinyEngineFileSecurityUtils.validateRelativePath(request.getName());
		throw new TinyEngineUnsupportedCapabilityException("TinyEngine resource upload storage is unavailable");
	}

	private void applyCurrentScope(Resource resource) {
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		resource.setCreatedBy(user == null ? null : user.getUserId());
		resource.setLastUpdatedBy(user == null ? null : user.getUserId());
		resource.setTenantId(user == null ? null : user.getTenantId());
		resource.setRenterId(user == null ? null : user.getRenterId());
		resource.setPlatformId(user == null ? null : user.getPlatformId());
	}

	private String currentUserId() {
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		return user == null ? null : user.getUserId();
	}

	private ServiceException failure() {
		return new ServiceException(ExceptionEnum.CM008.getResultCode(), ExceptionEnum.CM008.getResultMsg());
	}

	private ServiceException createFailure() {
		return new ServiceException(ExceptionEnum.CM002.getResultCode(), ExceptionEnum.CM002.getResultMsg());
	}
}
