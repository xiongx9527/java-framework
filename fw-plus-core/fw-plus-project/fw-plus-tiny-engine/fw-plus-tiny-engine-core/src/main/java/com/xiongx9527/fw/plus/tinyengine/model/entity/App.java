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
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xiongx9527.fw.plus.tinyengine.common.model.BaseEntity;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonArrayTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonObjectTypeHandler;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 应用表
 * </p>
 *
 * @author zhangjuncao
 * @since 2024-10-17
 */
@TableName(value = "t_app", autoResultMap = true)
public class App extends BaseEntity {
private String name;
private String appWebsite;
@JsonProperty("platform")
@JsonAlias({"platformId", "platform_id"})
    private Integer platformId;
private String platformHistoryId;
private String publishUrl;
private String editorUrl;
private String visitUrl;
@JsonProperty("image_url")
    private String imageUrl;
private String assetsUrl;
private Integer state;
@JsonProperty("published")
    private Boolean isPublish;
private Integer homePage;
private String css;
@TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> config;
private String constants;
@TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> dataHandler;
private String description;
private String latest;
private String gitGroup;
private String projectName;
private String branch;
private String isDemo;
private String isDefault;
private String templateType;
private Boolean isTemplate;
private Integer industryId;
private Integer sceneId;
private LocalDateTime setTemplateTime;
private String setTemplateBy;
private String setDefaultBy;
private String framework;
@JsonProperty("global_state")
    @TableField(typeHandler = JsonArrayTypeHandler.class)
    private List<Map<String, Object>> globalState;
private String defaultLang;
@TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> extendConfig;
private String dataHash;
private String canAssociate;
@JsonProperty("data_source_global")
    @TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> dataSourceGlobal;

    @TableField(exist = false)
private List<BusinessCategory> industry = new ArrayList<>();

    @TableField(exist = false)
private List<BusinessCategory> scene = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAppWebsite() {
		return appWebsite;
	}

	public void setAppWebsite(String appWebsite) {
		this.appWebsite = appWebsite;
	}

	public Integer getPlatformId() {
		return platformId;
	}

	public void setPlatformId(Integer platformId) {
		this.platformId = platformId;
	}

	public String getPlatformHistoryId() {
		return platformHistoryId;
	}

	public void setPlatformHistoryId(String platformHistoryId) {
		this.platformHistoryId = platformHistoryId;
	}

	public String getPublishUrl() {
		return publishUrl;
	}

	public void setPublishUrl(String publishUrl) {
		this.publishUrl = publishUrl;
	}

	public String getEditorUrl() {
		return editorUrl;
	}

	public void setEditorUrl(String editorUrl) {
		this.editorUrl = editorUrl;
	}

	public String getVisitUrl() {
		return visitUrl;
	}

	public void setVisitUrl(String visitUrl) {
		this.visitUrl = visitUrl;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getAssetsUrl() {
		return assetsUrl;
	}

	public void setAssetsUrl(String assetsUrl) {
		this.assetsUrl = assetsUrl;
	}

	public Integer getState() {
		return state;
	}

	public void setState(Integer state) {
		this.state = state;
	}

	public Boolean getIsPublish() {
		return isPublish;
	}

	public void setIsPublish(Boolean isPublish) {
		this.isPublish = isPublish;
	}

	public Integer getHomePage() {
		return homePage;
	}

	public void setHomePage(Integer homePage) {
		this.homePage = homePage;
	}

	public String getCss() {
		return css;
	}

	public void setCss(String css) {
		this.css = css;
	}

	public Map<String, Object> getConfig() {
		return config;
	}

	public void setConfig(Map<String, Object> config) {
		this.config = config;
	}

	public String getConstants() {
		return constants;
	}

	public void setConstants(String constants) {
		this.constants = constants;
	}

	public Map<String, Object> getDataHandler() {
		return dataHandler;
	}

	public void setDataHandler(Map<String, Object> dataHandler) {
		this.dataHandler = dataHandler;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLatest() {
		return latest;
	}

	public void setLatest(String latest) {
		this.latest = latest;
	}

	public String getGitGroup() {
		return gitGroup;
	}

	public void setGitGroup(String gitGroup) {
		this.gitGroup = gitGroup;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getIsDemo() {
		return isDemo;
	}

	public void setIsDemo(String isDemo) {
		this.isDemo = isDemo;
	}

	public String getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(String isDefault) {
		this.isDefault = isDefault;
	}

	public String getTemplateType() {
		return templateType;
	}

	public void setTemplateType(String templateType) {
		this.templateType = templateType;
	}

	public Boolean getIsTemplate() {
		return isTemplate;
	}

	public void setIsTemplate(Boolean isTemplate) {
		this.isTemplate = isTemplate;
	}

	public Integer getIndustryId() {
		return industryId;
	}

	public void setIndustryId(Integer industryId) {
		this.industryId = industryId;
	}

	public Integer getSceneId() {
		return sceneId;
	}

	public void setSceneId(Integer sceneId) {
		this.sceneId = sceneId;
	}

	public LocalDateTime getSetTemplateTime() {
		return setTemplateTime;
	}

	public void setSetTemplateTime(LocalDateTime setTemplateTime) {
		this.setTemplateTime = setTemplateTime;
	}

	public String getSetTemplateBy() {
		return setTemplateBy;
	}

	public void setSetTemplateBy(String setTemplateBy) {
		this.setTemplateBy = setTemplateBy;
	}

	public String getSetDefaultBy() {
		return setDefaultBy;
	}

	public void setSetDefaultBy(String setDefaultBy) {
		this.setDefaultBy = setDefaultBy;
	}

	public String getFramework() {
		return framework;
	}

	public void setFramework(String framework) {
		this.framework = framework;
	}

	public List<Map<String, Object>> getGlobalState() {
		return globalState;
	}

	public void setGlobalState(List<Map<String, Object>> globalState) {
		this.globalState = globalState;
	}

	public String getDefaultLang() {
		return defaultLang;
	}

	public void setDefaultLang(String defaultLang) {
		this.defaultLang = defaultLang;
	}

	public Map<String, Object> getExtendConfig() {
		return extendConfig;
	}

	public void setExtendConfig(Map<String, Object> extendConfig) {
		this.extendConfig = extendConfig;
	}

	public String getDataHash() {
		return dataHash;
	}

	public void setDataHash(String dataHash) {
		this.dataHash = dataHash;
	}

	public String getCanAssociate() {
		return canAssociate;
	}

	public void setCanAssociate(String canAssociate) {
		this.canAssociate = canAssociate;
	}

	public Map<String, Object> getDataSourceGlobal() {
		return dataSourceGlobal;
	}

	public void setDataSourceGlobal(Map<String, Object> dataSourceGlobal) {
		this.dataSourceGlobal = dataSourceGlobal;
	}

	public List<BusinessCategory> getIndustry() {
		return industry;
	}

	public void setIndustry(List<BusinessCategory> industry) {
		this.industry = industry;
	}

	public List<BusinessCategory> getScene() {
		return scene;
	}

	public void setScene(List<BusinessCategory> scene) {
		this.scene = scene;
	}
}
