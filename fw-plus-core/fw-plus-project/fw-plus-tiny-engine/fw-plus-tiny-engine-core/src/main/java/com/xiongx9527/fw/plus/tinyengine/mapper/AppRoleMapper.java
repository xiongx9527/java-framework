package com.xiongx9527.fw.plus.tinyengine.mapper;

import com.xiongx9527.fw.plus.tinyengine.model.dto.AppMemberDto;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppRoleMapper {
	String findActiveRoleCode(@Param("tenantId") String tenantId, @Param("appId") Integer appId,
			@Param("userId") String userId);
	List<AppMemberDto> findMembers(@Param("tenantId") String tenantId, @Param("appId") Integer appId);
	Integer lockApp(@Param("tenantId") String tenantId, @Param("appId") Integer appId);
	int countUserInTenant(@Param("tenantId") String tenantId, @Param("userId") String userId);
	int countAppRelation(@Param("tenantId") String tenantId, @Param("appId") Integer appId,
			@Param("userId") String userId);
	int countOwner(@Param("tenantId") String tenantId, @Param("appId") Integer appId);
	int insertRole(@Param("tenantId") String tenantId, @Param("appId") Integer appId,
			@Param("userId") String userId, @Param("roleCode") String roleCode,
			@Param("expiredTime") LocalDateTime expiredTime, @Param("operator") String operator);
	int updateRole(@Param("tenantId") String tenantId, @Param("appId") Integer appId,
			@Param("userId") String userId, @Param("roleCode") String roleCode,
			@Param("expiredTime") LocalDateTime expiredTime, @Param("operator") String operator);
	int deleteMemberRole(@Param("tenantId") String tenantId, @Param("appId") Integer appId,
			@Param("userId") String userId);
	int deleteAppRoles(@Param("tenantId") String tenantId, @Param("appId") Integer appId);
}
