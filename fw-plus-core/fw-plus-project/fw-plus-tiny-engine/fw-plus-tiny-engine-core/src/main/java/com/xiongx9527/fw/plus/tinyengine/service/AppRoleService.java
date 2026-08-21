package com.xiongx9527.fw.plus.tinyengine.service;

import com.xiongx9527.fw.plus.tinyengine.model.dto.AppMemberDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppMemberRoleRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppPermission;

import java.util.List;

public interface AppRoleService {
	void requirePermission(Integer appId, AppPermission permission);
	List<AppMemberDto> members(Integer appId);
	AppMemberDto addMember(Integer appId, AppMemberRoleRequest request);
	AppMemberDto changeMember(Integer appId, String userId, AppMemberRoleRequest request);
	void removeMember(Integer appId, String userId);
	void createOwner(Integer appId, String tenantId, String userId);
	void deleteAppRoles(Integer appId, String tenantId);
}
