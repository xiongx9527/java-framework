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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 区块表
 * </p>
 *
 * @author zhangjuncao
 * @since 2024-10-17
 */
@TableName("t_block")
public class Block extends BaseEntity {
private String label;
@JsonProperty("name_cn")
    private String name;
private String framework;

    @TableField(typeHandler = JsonObjectTypeHandler.class)
private Map<String, Object> content;

    @TableField(typeHandler = JsonObjectTypeHandler.class)
private Map<String, Object> assets;

    @JsonProperty("last_build_info")
    @TableField(typeHandler = JsonObjectTypeHandler.class)
private Map<String, Object> lastBuildInfo;
private String description;
@TableField(typeHandler = JsonArrayTypeHandler.class)
    private List<String> tags;
@JsonProperty("version")
    private String latestVersion;
@JsonProperty("current_history")
    private Integer latestHistoryId;
private String screenshot;
private String path;
@JsonProperty("occupier")
    private String occupierBy;
@JsonProperty("is_official")
    private Boolean isOfficial;
@JsonProperty("public")
    @TableField("public")
    private Integer publicStatus;
@JsonProperty("is_default")
    private Boolean isDefault;
@JsonProperty("tiny_reserved")
    @TableField("tiny_reserved")
    private Boolean isTinyReserved;
@JsonProperty("npm_name")
    private String npmName;
@TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Map<String, String>> i18n;
@JsonProperty("platform_id")
    private Integer platformId;
@JsonProperty("created_app")
    private Integer appId;
@JsonProperty("content_blocks")
    private String contentBlocks;

    @TableField(exist = false)
    @JsonProperty("public_scope_tenants")
    private List<Object> publicScopeTenants = new ArrayList<>();

    @TableField(exist = false)
    @JsonProperty("histories_length")
    private Integer historiesLength = 0;

    @TableField(exist = false)
    @JsonProperty("is_published")
    private Boolean isPublished;

    @TableField(exist = false)
    @JsonProperty("current_version")
    private String currentVersion;

    public Block(Map<String, Object> content, String label) {
        super();
    }

    public Block() {
        super();
    }

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFramework() {
		return framework;
	}

	public void setFramework(String framework) {
		this.framework = framework;
	}

	public Map<String, Object> getContent() {
		return content;
	}

	public void setContent(Map<String, Object> content) {
		this.content = content;
	}

	public Map<String, Object> getAssets() {
		return assets;
	}

	public void setAssets(Map<String, Object> assets) {
		this.assets = assets;
	}

	public Map<String, Object> getLastBuildInfo() {
		return lastBuildInfo;
	}

	public void setLastBuildInfo(Map<String, Object> lastBuildInfo) {
		this.lastBuildInfo = lastBuildInfo;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getLatestVersion() {
		return latestVersion;
	}

	public void setLatestVersion(String latestVersion) {
		this.latestVersion = latestVersion;
	}

	public Integer getLatestHistoryId() {
		return latestHistoryId;
	}

	public void setLatestHistoryId(Integer latestHistoryId) {
		this.latestHistoryId = latestHistoryId;
	}

	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getOccupierBy() {
		return occupierBy;
	}

	public void setOccupierBy(String occupierBy) {
		this.occupierBy = occupierBy;
	}

	public Boolean getIsOfficial() {
		return isOfficial;
	}

	public void setIsOfficial(Boolean isOfficial) {
		this.isOfficial = isOfficial;
	}

	public Integer getPublicStatus() {
		return publicStatus;
	}

	public void setPublicStatus(Integer publicStatus) {
		this.publicStatus = publicStatus;
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

	public String getNpmName() {
		return npmName;
	}

	public void setNpmName(String npmName) {
		this.npmName = npmName;
	}

	public Map<String, Map<String, String>> getI18n() {
		return i18n;
	}

	public void setI18n(Map<String, Map<String, String>> i18n) {
		this.i18n = i18n;
	}

	public Integer getPlatformId() {
		return platformId;
	}

	public void setPlatformId(Integer platformId) {
		this.platformId = platformId;
	}

	public Integer getAppId() {
		return appId;
	}

	public void setAppId(Integer appId) {
		this.appId = appId;
	}

	public String getContentBlocks() {
		return contentBlocks;
	}

	public void setContentBlocks(String contentBlocks) {
		this.contentBlocks = contentBlocks;
	}

	public List<Object> getPublicScopeTenants() {
		return publicScopeTenants;
	}

	public void setPublicScopeTenants(List<Object> publicScopeTenants) {
		this.publicScopeTenants = publicScopeTenants;
	}

	public Integer getHistoriesLength() {
		return historiesLength;
	}

	public void setHistoriesLength(Integer historiesLength) {
		this.historiesLength = historiesLength;
	}

	public Boolean getIsPublished() {
		return isPublished;
	}

	public void setIsPublished(Boolean isPublished) {
		this.isPublished = isPublished;
	}

	public String getCurrentVersion() {
		return currentVersion;
	}

	public void setCurrentVersion(String currentVersion) {
		this.currentVersion = currentVersion;
	}
}
