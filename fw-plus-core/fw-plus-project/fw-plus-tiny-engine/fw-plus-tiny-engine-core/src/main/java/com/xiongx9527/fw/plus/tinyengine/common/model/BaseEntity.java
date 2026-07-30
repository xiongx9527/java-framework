package com.xiongx9527.fw.plus.tinyengine.common.model;

import java.time.LocalDateTime;

public class BaseEntity {

	private Integer id;
	private String createdBy;
	private String lastUpdatedBy;
	private LocalDateTime createdTime;
	private LocalDateTime lastUpdatedTime;
	private String tenantId;
	private String renterId;
	private String siteId;

	public Integer getId() { return id; }
	public void setId(Integer id) { this.id = id; }
	public String getCreatedBy() { return createdBy; }
	public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
	public String getLastUpdatedBy() { return lastUpdatedBy; }
	public void setLastUpdatedBy(String lastUpdatedBy) { this.lastUpdatedBy = lastUpdatedBy; }
	public LocalDateTime getCreatedTime() { return createdTime; }
	public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
	public LocalDateTime getLastUpdatedTime() { return lastUpdatedTime; }
	public void setLastUpdatedTime(LocalDateTime lastUpdatedTime) { this.lastUpdatedTime = lastUpdatedTime; }
	public String getTenantId() { return tenantId; }
	public void setTenantId(String tenantId) { this.tenantId = tenantId; }
	public String getRenterId() { return renterId; }
	public void setRenterId(String renterId) { this.renterId = renterId; }
	public String getSiteId() { return siteId; }
	public void setSiteId(String siteId) { this.siteId = siteId; }
}
