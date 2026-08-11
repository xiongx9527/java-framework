package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppRoleMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppMemberDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppMemberRoleRequest;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.service.AppRoleService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppPermission;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppRoleCode;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineAccessDeniedException;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class AppRoleServiceImpl implements AppRoleService {
	private final AppRoleMapper roleMapper;
	private final AppMapper appMapper;
	private final TinyEngineUserContext userContext;

	public AppRoleServiceImpl(AppRoleMapper roleMapper, AppMapper appMapper, TinyEngineUserContext userContext) {
		this.roleMapper = roleMapper;
		this.appMapper = appMapper;
		this.userContext = userContext;
	}

	@Override
	public void requirePermission(Integer appId, AppPermission permission) {
		TinyEngineUser user = currentUser();
		if (appId == null || user == null || user.getTenantId() == null) {
			throw denied();
		}
		App app = appMapper.queryAppById(appId, user.getTenantId());
		if (app == null) {
			throw denied();
		}
		if (Boolean.FALSE.equals(app.getEnable()) && permission != AppPermission.MANAGE
				&& permission != AppPermission.MANAGE_MEMBERS) {
			throw denied();
		}
		if (Boolean.TRUE.equals(app.getIsTemplate()) && permission == AppPermission.READ) {
			return;
		}
		if (isSystem(user)) {
			return;
		}
		String roleCode = roleMapper.findActiveRoleCode(user.getTenantId(), appId, user.getUserId());
		if (!allowed(AppRoleCode.fromCode(roleCode), permission)) {
			throw denied();
		}
	}

	@Override
	public List<AppMemberDto> members(Integer appId) {
		requirePermission(appId, AppPermission.MANAGE_MEMBERS);
		TinyEngineUser user = currentUser();
		List<AppMemberDto> result = roleMapper.findMembers(user.getTenantId(), appId);
		return result == null ? Collections.<AppMemberDto>emptyList() : result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AppMemberDto addMember(Integer appId, AppMemberRoleRequest request) {
		TinyEngineUser operator = requireMemberOperation(appId, request == null ? null : request.getUserId());
		AppRoleCode role = mutableRole(request == null ? null : request.getRoleCode());
		lockExistingApp(operator.getTenantId(), appId);
		requireTenantUser(operator.getTenantId(), request.getUserId());
		if (roleMapper.countAppRelation(operator.getTenantId(), appId, request.getUserId()) > 0) {
			throw new IllegalStateException("User already has an application role");
		}
		try {
			if (roleMapper.insertRole(operator.getTenantId(), appId, request.getUserId(), role.getCode(),
					request.getExpiredTime(), operator.getUserId()) != 1) {
				throw new IllegalStateException("Application role does not exist");
			}
		} catch (DataIntegrityViolationException ex) {
			throw new IllegalStateException("User already has an application role", ex);
		}
		return findMember(operator.getTenantId(), appId, request.getUserId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AppMemberDto changeMember(Integer appId, String userId, AppMemberRoleRequest request) {
		TinyEngineUser operator = requireMemberOperation(appId, userId);
		AppRoleCode role = mutableRole(request == null ? null : request.getRoleCode());
		lockExistingApp(operator.getTenantId(), appId);
		requireTenantUser(operator.getTenantId(), userId);
		if (roleMapper.updateRole(operator.getTenantId(), appId, userId, role.getCode(),
				request.getExpiredTime(), operator.getUserId()) != 1) {
			throw new IllegalStateException("Mutable application member does not exist");
		}
		return findMember(operator.getTenantId(), appId, userId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeMember(Integer appId, String userId) {
		TinyEngineUser operator = requireMemberOperation(appId, userId);
		lockExistingApp(operator.getTenantId(), appId);
		if (roleMapper.deleteMemberRole(operator.getTenantId(), appId, userId) != 1) {
			throw new IllegalStateException("Mutable application member does not exist");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void createOwner(Integer appId, String tenantId, String userId) {
		if (userId == null || "system".equals(userId)) {
			return;
		}
		lockExistingApp(tenantId, appId);
		if (roleMapper.countOwner(tenantId, appId) > 0) {
			throw new IllegalStateException("Application already has an owner");
		}
		if (roleMapper.countAppRelation(tenantId, appId, userId) > 0) {
			throw new IllegalStateException("Application creator already has an application role");
		}
		if (roleMapper.insertRole(tenantId, appId, userId, AppRoleCode.APP_OWNER.getCode(), null, userId) != 1) {
			throw new IllegalStateException("Application owner role does not exist");
		}
	}

	@Override
	public void deleteAppRoles(Integer appId, String tenantId) {
		roleMapper.deleteAppRoles(tenantId, appId);
	}

	private TinyEngineUser requireMemberOperation(Integer appId, String targetUserId) {
		requirePermission(appId, AppPermission.MANAGE_MEMBERS);
		TinyEngineUser operator = currentUser();
		if (operator == null || targetUserId == null || targetUserId.equals(operator.getUserId())) {
			throw denied();
		}
		return operator;
	}

	private void lockExistingApp(String tenantId, Integer appId) {
		if (roleMapper.lockApp(tenantId, appId) == null) {
			throw denied();
		}
	}

	private void requireTenantUser(String tenantId, String userId) {
		if (roleMapper.countUserInTenant(tenantId, userId) != 1) {
			throw denied();
		}
	}

	private AppMemberDto findMember(String tenantId, Integer appId, String userId) {
		for (AppMemberDto member : roleMapper.findMembers(tenantId, appId)) {
			if (userId.equals(member.getUserId())) {
				return member;
			}
		}
		throw new IllegalStateException("Application member was not persisted");
	}

	private AppRoleCode mutableRole(String roleCode) {
		AppRoleCode role = AppRoleCode.fromCode(roleCode);
		if (role != AppRoleCode.APP_ADMIN && role != AppRoleCode.APP_DEV) {
			throw denied();
		}
		return role;
	}

	private boolean allowed(AppRoleCode role, AppPermission permission) {
		if (role == AppRoleCode.APP_OWNER) {
			return true;
		}
		if (role == AppRoleCode.APP_ADMIN) {
			return permission != AppPermission.DELETE;
		}
		if (role == AppRoleCode.APP_DEV) {
			return permission == AppPermission.READ || permission == AppPermission.DEVELOP
					|| permission == AppPermission.PREVIEW;
		}
		return false;
	}

	private TinyEngineUser currentUser() {
		return userContext == null ? null : userContext.getCurrentUser();
	}

	private boolean isSystem(TinyEngineUser user) {
		return user != null && "system".equals(user.getUserId());
	}

	private TinyEngineAccessDeniedException denied() {
		return new TinyEngineAccessDeniedException("TinyEngine application permission denied");
	}
}
