package com.xiongx9527.fw.plus.tinyengine.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class AppMemberRoleRequest {
	private String userId;
	private String roleCode;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime expiredTime;

	public String getUserId() { return userId; }
	public void setUserId(String userId) { this.userId = userId; }
	public String getRoleCode() { return roleCode; }
	public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
	public LocalDateTime getExpiredTime() { return expiredTime; }
	public void setExpiredTime(LocalDateTime expiredTime) { this.expiredTime = expiredTime; }
}
