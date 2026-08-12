package com.xiongx9527.fw.plus.tinyengine.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiongx9527.fw.plus.tinyengine.common.model.BaseEntity;

@TableName("t_ai_model")
public class AiModelConfig extends BaseEntity {

	private Integer appId;
	private Long serviceId;
	private String name;
	private String label;
	private String capabilities;
	private Boolean enabled;
	private Integer sortOrder;

	public Integer getAppId() { return appId; }
	public void setAppId(Integer appId) { this.appId = appId; }
	public Long getServiceId() { return serviceId; }
	public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getLabel() { return label; }
	public void setLabel(String label) { this.label = label; }
	public String getCapabilities() { return capabilities; }
	public void setCapabilities(String capabilities) { this.capabilities = capabilities; }
	public Boolean getEnabled() { return enabled; }
	public void setEnabled(Boolean enabled) { this.enabled = enabled; }
	public Integer getSortOrder() { return sortOrder; }
	public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
