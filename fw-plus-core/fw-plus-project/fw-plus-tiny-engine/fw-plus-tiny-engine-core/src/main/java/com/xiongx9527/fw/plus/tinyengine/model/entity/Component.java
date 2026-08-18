/**
 * Copyright (c) 2023 - present TinyEngine Authors.
 * Copyright (c) 2023 - present Huawei Cloud Computing Technologies Co., Ltd.
 *
 * Use of this source code is governed by an MIT-style license.
 *
 * THE OPEN SOURCE SOFTWARE IN THIS PRODUCT IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR PURPOSE. SEE THE APPLICABLE LICENSES FOR MORE DETAILS.
 *
 */

package com.xiongx9527.fw.plus.tinyengine.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xiongx9527.fw.plus.tinyengine.common.model.BaseEntity;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonArrayTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonObjectTypeHandler;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 组件表
 * </p>
 *
 * @author lu -yg
 * @since 2024-10-17
 */
@TableName(value = "t_component", resultMap = "ComponentMap", autoResultMap = true)
public class Component extends BaseEntity {
private String version;

    @TableField(typeHandler = JsonObjectTypeHandler.class)
private Map<String, String> name;
@TableField(value = "name_en")
    private String component;
private String icon;
private String description;
private String docUrl;
private String screenshot;
private String tags;
private String keywords;
private String devMode;

    @TableField(typeHandler = JsonObjectTypeHandler.class)
private Map<String, Object> npm;
@TableField(value = "`group`")
    private String group;
private String category;
private Integer priority;

    @TableField(typeHandler = JsonArrayTypeHandler.class)
private List<Map<String, Object>> snippets;

    @TableField(typeHandler = JsonObjectTypeHandler.class)
private Map<String, Object> schemaFragment;
@JsonProperty("configure")
    @TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> configure;

    @JsonProperty("public")
@TableField(value = "public")
    private Integer publicStatus;
@TableField(value = "framework")
    private String framework;
private Boolean isOfficial;
private Boolean isDefault;
@TableField(value = "tiny_reserved")
    private Boolean isTinyReserved;

    @JsonProperty("component_metadata")
    @TableField(typeHandler = JsonObjectTypeHandler.class)
private Map<String, Object> componentMetadata;
private Integer libraryId;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Map<String, String> getName() {
		return name;
	}

	public void setName(Map<String, String> name) {
		this.name = name;
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDocUrl() {
		return docUrl;
	}

	public void setDocUrl(String docUrl) {
		this.docUrl = docUrl;
	}

	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String getDevMode() {
		return devMode;
	}

	public void setDevMode(String devMode) {
		this.devMode = devMode;
	}

	public Map<String, Object> getNpm() {
		return npm;
	}

	public void setNpm(Map<String, Object> npm) {
		this.npm = npm;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public List<Map<String, Object>> getSnippets() {
		return snippets;
	}

	public void setSnippets(List<Map<String, Object>> snippets) {
		this.snippets = snippets;
	}

	public Map<String, Object> getSchemaFragment() {
		return schemaFragment;
	}

	public void setSchemaFragment(Map<String, Object> schemaFragment) {
		this.schemaFragment = schemaFragment;
	}

	public Map<String, Object> getConfigure() {
		return configure;
	}

	public void setConfigure(Map<String, Object> configure) {
		this.configure = configure;
	}

	public Integer getPublicStatus() {
		return publicStatus;
	}

	public void setPublicStatus(Integer publicStatus) {
		this.publicStatus = publicStatus;
	}

	public String getFramework() {
		return framework;
	}

	public void setFramework(String framework) {
		this.framework = framework;
	}

	public Boolean getIsOfficial() {
		return isOfficial;
	}

	public void setIsOfficial(Boolean isOfficial) {
		this.isOfficial = isOfficial;
	}

	public Boolean getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(Boolean isDefault) {
		this.isDefault = isDefault;
	}

	public Boolean getIsTinyReserved() {
		return isTinyReserved;
	}

	public void setIsTinyReserved(Boolean isTinyReserved) {
		this.isTinyReserved = isTinyReserved;
	}

	public Map<String, Object> getComponentMetadata() {
		return componentMetadata;
	}

	public void setComponentMetadata(Map<String, Object> componentMetadata) {
		this.componentMetadata = componentMetadata;
	}

	public Integer getLibraryId() {
		return libraryId;
	}

	public void setLibraryId(Integer libraryId) {
		this.libraryId = libraryId;
	}
}
