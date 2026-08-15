package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.common.util.TinyEngineFileSecurityUtils;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.ResourceMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.ResourceGroupResourceMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.ResourceRequestDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Resource;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ResourceGroupResource;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceGroupService;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import com.xiongx9527.fw.plus.tinyengine.service.AppRoleService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppPermission;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ResourceGroup;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUnsupportedCapabilityException;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class ResourceServiceImpl extends MapperServiceSupport<ResourceMapper> implements ResourceService {

	@Override
	public boolean save(Resource resource) {
		require(resource == null ? null : resource.getAppId(), AppPermission.DEVELOP);
		applyPersistedAppScope(resource);
		return baseMapper.insert(resource) == 1;
	}
	@Autowired
	private AppMapper appMapper;
	@Autowired
	private TinyEngineUserContext userContext;
	@Autowired
	private ResourceGroupResourceMapper resourceGroupResourceMapper;
	@Autowired
	private ResourceGroupService resourceGroupService;
	@Autowired
	private AppRoleService appRoleService;

	@Override
	public List<Resource> queryAllResource() {
		List<Resource> resources = baseMapper.queryAllResource();
		requireAll(resources, AppPermission.READ);
		return resources;
	}

	@Override
	public List<Resource> queryResourcesByNameAndDes(String name, String description) {
		List<Resource> resources = baseMapper.findResourcesByNameAndDes(name, description);
		requireAll(resources, AppPermission.READ);
		return resources;
	}

	@Override
	public Resource queryResourceById(Integer id) {
		Resource resource = baseMapper.queryResourceById(id);
		require(resource == null ? null : resource.getAppId(), AppPermission.READ);
		return resource;
	}

	@Override
	public Resource queryResourceByName(String name) {
		QueryWrapper<Resource> query = new QueryWrapper<Resource>();
		if (name != null && !name.startsWith("thumbnail_")) {
			query.eq("name", name);
		} else {
			query.eq("thumbnail_name", name);
		}
		Resource resource = baseMapper.selectOne(query);
		require(resource == null ? null : resource.getAppId(), AppPermission.READ);
		return resource;
	}

	@Override
	public List<Resource> queryResourceByResourceGroupId(Integer resourceGroupId) {
		ResourceGroup group = resourceGroupService.queryResourceGroupById(resourceGroupId);
		require(group == null ? null : group.getAppId(), AppPermission.READ);
		return baseMapper.findResourceByResourceGroupId(resourceGroupId, currentUserId());
	}

	@Override
	public Resource deleteResourceById(Integer id) {
		Resource resource = queryResourceById(id);
		require(resource == null ? null : resource.getAppId(), AppPermission.DEVELOP);
		if (baseMapper.deleteResourceById(id) != 1) {
			throw failure();
		}
		return resource;
	}

	@Override
	public Resource updateResourceById(Resource resource) {
		Resource existing = resource == null || resource.getId() == null ? null
				: baseMapper.queryResourceById(resource.getId());
		require(existing == null ? null : existing.getAppId(), AppPermission.DEVELOP);
		if (resource == null || existing == null) {
			throw failure();
		}
		resource.setAppId(existing.getAppId());
		resource.setTenantId(existing.getTenantId());
		resource.setRenterId(existing.getRenterId());
		resource.setSiteId(existing.getSiteId());
		if (baseMapper.updateResourceById(resource) != 1) {
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
			ResourceGroup group = resource == null || resource.getResourceGroupId() == null ? null
					: resourceGroupService.queryResourceGroupById(resource.getResourceGroupId());
			if (group == null) {
				throw createFailure();
			}
			require(group.getAppId(), AppPermission.DEVELOP);
			resource.setAppId(group.getAppId());
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

	private void applyPersistedAppScope(Resource resource) {
		if (resource == null || resource.getAppId() == null || appMapper == null) {
			return;
		}
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		String tenantId = user == null ? null : user.getTenantId();
		App app = appMapper.queryAppById(resource.getAppId(), tenantId);
		if (app == null) {
			throw createFailure();
		}
		resource.setTenantId(app.getTenantId());
		resource.setRenterId(app.getRenterId());
		resource.setSiteId(app.getSiteId());
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

	private void require(Integer appId, AppPermission permission) {
		if (appRoleService != null) {
			appRoleService.requirePermission(appId, permission);
		}
	}

	private void requireAll(List<Resource> resources, AppPermission permission) {
		if (resources == null) {
			return;
		}
		for (Resource resource : resources) {
			require(resource == null ? null : resource.getAppId(), permission);
		}
	}
}
