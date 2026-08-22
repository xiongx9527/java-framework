package com.xiongx9527.fw.plus.tinyengine.model.dto;

import com.xiongx9527.fw.plus.tinyengine.model.entity.AiModelConfig;

public class AiModelDto {

	private Long id;
	private String name;
	private String label;
	private String capabilities;
	private String settings;
	private Boolean enabled;
	private Integer sortOrder;

	public static AiModelDto from(AiModelConfig model) {
		AiModelDto dto = new AiModelDto();
		dto.setId(model.getId());
		dto.setName(model.getName());
		dto.setLabel(model.getLabel());
		dto.setCapabilities(model.getCapabilities());
		dto.setSettings(model.getSettings());
		dto.setEnabled(model.getEnabled());
		dto.setSortOrder(model.getSortOrder());
		return dto;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getLabel() { return label; }
	public void setLabel(String label) { this.label = label; }
	public String getCapabilities() { return capabilities; }
	public void setCapabilities(String capabilities) { this.capabilities = capabilities; }
	public String getSettings() { return settings; }
	public void setSettings(String settings) { this.settings = settings; }
	public Boolean getEnabled() { return enabled; }
	public void setEnabled(Boolean enabled) { this.enabled = enabled; }
	public Integer getSortOrder() { return sortOrder; }
	public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
