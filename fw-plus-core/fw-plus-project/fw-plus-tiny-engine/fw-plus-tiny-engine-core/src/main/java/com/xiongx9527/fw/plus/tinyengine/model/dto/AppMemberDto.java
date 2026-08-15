package com.xiongx9527.fw.plus.tinyengine.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class AppMemberDto {
	private Integer id;
	private String userId;
	private String username;
	private String roleCode;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime expiredTime;

	public Integer getId() { return id; }
	public void setId(Integer id) { this.id = id; }
	public String getUserId() { return userId; }
	public void setUserId(String userId) { this.userId = userId; }
	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }
	public String getRoleCode() { return roleCode; }
	public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
	public LocalDateTime getExpiredTime() { return expiredTime; }
	public void setExpiredTime(LocalDateTime expiredTime) { this.expiredTime = expiredTime; }
}
