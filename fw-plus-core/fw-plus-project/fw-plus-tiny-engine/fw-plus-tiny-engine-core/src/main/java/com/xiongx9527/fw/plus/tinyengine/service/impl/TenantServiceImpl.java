package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.AuthUsersUnitsRolesMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.TenantMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AuthUsersUnitsRoles;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Tenant;
import com.xiongx9527.fw.plus.tinyengine.service.TenantService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {
	@Autowired
	private TinyEngineUserContext userContext;
	@Autowired
	private AuthUsersUnitsRolesMapper authUsersUnitsRolesMapper;

	@Override
	public List<Tenant> findAllTenant() {
		return baseMapper.queryAllTenant();
	}

	@Override
	public Tenant findTenantById(Integer id) {
		return baseMapper.queryTenantById(id);
	}

	@Override
	public List<Tenant> findTenantByCondition(Tenant tenant) {
		return baseMapper.queryTenantByCondition(tenant);
	}

	@Override
	public Integer deleteTenantById(Integer id) {
		return baseMapper.deleteTenantById(id);
	}

	@Override
	public Integer updateTenantById(Tenant tenant) {
		return baseMapper.updateTenantById(tenant);
	}

	@Override
	public Integer createTenant(Tenant tenant) {
		QueryWrapper<Tenant> query = new QueryWrapper<Tenant>().eq("name_en", tenant.getNameEn());
		if (baseMapper.selectOne(query) != null) {
			throw new ServiceException(ExceptionEnum.CM003.getResultCode(), ExceptionEnum.CM003.getResultMsg());
		}
		if (baseMapper.createTenant(tenant) != 1) {
			throw new ServiceException(ExceptionEnum.CM001.getResultCode(), ExceptionEnum.CM001.getResultMsg());
		}
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		if (user != null && user.getUserId() != null) {
			AuthUsersUnitsRoles relation = new AuthUsersUnitsRoles();
			Integer tenantId = Integer.valueOf(tenant.getId());
			relation.setTenantId(tenantId);
			relation.setRoleId(2);
			relation.setUnitType("tenant");
			relation.setUnitId(tenantId);
			relation.setUserId(Integer.valueOf(user.getUserId()));
			authUsersUnitsRolesMapper.createAuthUsersUnitsRoles(relation);
		}
		return Integer.valueOf(tenant.getId());
	}
}
