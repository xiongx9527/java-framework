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
 * 设计器表
 * </p>
 *
 * @author zhangjuncao
 * @since 2024-10-17
 */
@TableName(value = "t_platform", autoResultMap = true)
public class Platform extends BaseEntity {
private String name;
@JsonProperty("published")
    private Boolean isPublish;
@TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> lastBuildInfo;
private String description;
private String latestVersion;
private Integer latestHistoryId;
private Integer materialHistoryId;
private String imageUrl;
@TableField(typeHandler = JsonArrayTypeHandler.class)
    private List<Integer> sortPlugins;
@TableField(typeHandler = JsonArrayTypeHandler.class)
    private List<Integer> sortToolbar;
private Boolean isDefault;
    private String prettierOpts;
private String setDefaultBy;
@TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> appExtendConfig;
private String dataHash;
private Integer businessCategoryId;
private Integer themeId;
private String platformUrl;
private String vscodeUrl;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getIsPublish() {
		return isPublish;
	}

	public void setIsPublish(Boolean isPublish) {
		this.isPublish = isPublish;
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

	public Integer getMaterialHistoryId() {
		return materialHistoryId;
	}

	public void setMaterialHistoryId(Integer materialHistoryId) {
		this.materialHistoryId = materialHistoryId;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public List<Integer> getSortPlugins() {
		return sortPlugins;
	}

	public void setSortPlugins(List<Integer> sortPlugins) {
		this.sortPlugins = sortPlugins;
	}

	public List<Integer> getSortToolbar() {
		return sortToolbar;
	}

	public void setSortToolbar(List<Integer> sortToolbar) {
		this.sortToolbar = sortToolbar;
	}

	public Boolean getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(Boolean isDefault) {
		this.isDefault = isDefault;
	}

	public String getPrettierOpts() {
		return prettierOpts;
	}

	public void setPrettierOpts(String prettierOpts) {
		this.prettierOpts = prettierOpts;
	}

	public String getSetDefaultBy() {
		return setDefaultBy;
	}

	public void setSetDefaultBy(String setDefaultBy) {
		this.setDefaultBy = setDefaultBy;
	}

	public Map<String, Object> getAppExtendConfig() {
		return appExtendConfig;
	}

	public void setAppExtendConfig(Map<String, Object> appExtendConfig) {
		this.appExtendConfig = appExtendConfig;
	}

	public String getDataHash() {
		return dataHash;
	}

	public void setDataHash(String dataHash) {
		this.dataHash = dataHash;
	}

	public Integer getBusinessCategoryId() {
		return businessCategoryId;
	}

	public void setBusinessCategoryId(Integer businessCategoryId) {
		this.businessCategoryId = businessCategoryId;
	}

	public Integer getThemeId() {
		return themeId;
	}

	public void setThemeId(Integer themeId) {
		this.themeId = themeId;
	}

	public String getPlatformUrl() {
		return platformUrl;
	}

	public void setPlatformUrl(String platformUrl) {
		this.platformUrl = platformUrl;
	}

	public String getVscodeUrl() {
		return vscodeUrl;
	}

	public void setVscodeUrl(String vscodeUrl) {
		this.vscodeUrl = vscodeUrl;
	}
}
