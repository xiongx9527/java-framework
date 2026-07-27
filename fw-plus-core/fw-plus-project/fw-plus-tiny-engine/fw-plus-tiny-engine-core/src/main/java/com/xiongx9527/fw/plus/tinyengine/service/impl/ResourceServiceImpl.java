package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.ResourceMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Resource;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource> implements ResourceService {
	@Autowired
	private TinyEngineUserContext userContext;

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

	private String currentUserId() {
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		return user == null ? null : user.getUserId();
	}

	private ServiceException failure() {
		return new ServiceException(ExceptionEnum.CM008.getResultCode(), ExceptionEnum.CM008.getResultMsg());
	}
}
