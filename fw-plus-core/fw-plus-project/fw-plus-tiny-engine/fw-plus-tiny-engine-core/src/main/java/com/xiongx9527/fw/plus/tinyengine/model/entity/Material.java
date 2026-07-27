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
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonObjectTypeHandler;

import java.util.Map;

/**
 * <p>
 * 物料包表
 * </p>
 *
 * @author zhangjuncao
 * @since 2024-10-17
 */
@TableName("t_material")
public class Material extends BaseEntity {
private String name;
private String npmName;
private String framework;
@TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> assetsUrl;
private String imageUrl;
private Boolean isPublish;
private String latestVersion;
private Integer latestHistoryId;
@JsonProperty("public")
    private Integer publicStatus;
@TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> lastBuildInfo;
private String description;
private Boolean isOfficial;
private Boolean isDefault;
private Boolean isTinyReserved;
private Integer componentLibraryId;
private Integer materialCategoryId;
private Integer materialSize;
private String tgzUrl;
private String unzipTgzRootPathUrl;
private String unzipTgzFiles;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNpmName() {
		return npmName;
	}

	public void setNpmName(String npmName) {
		this.npmName = npmName;
	}

	public String getFramework() {
		return framework;
	}

	public void setFramework(String framework) {
		this.framework = framework;
	}

	public Map<String, Object> getAssetsUrl() {
		return assetsUrl;
	}

	public void setAssetsUrl(Map<String, Object> assetsUrl) {
		this.assetsUrl = assetsUrl;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public Boolean getIsPublish() {
		return isPublish;
	}

	public void setIsPublish(Boolean isPublish) {
		this.isPublish = isPublish;
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

	public Integer getPublicStatus() {
		return publicStatus;
	}

	public void setPublicStatus(Integer publicStatus) {
		this.publicStatus = publicStatus;
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

	public Integer getComponentLibraryId() {
		return componentLibraryId;
	}

	public void setComponentLibraryId(Integer componentLibraryId) {
		this.componentLibraryId = componentLibraryId;
	}

	public Integer getMaterialCategoryId() {
		return materialCategoryId;
	}

	public void setMaterialCategoryId(Integer materialCategoryId) {
		this.materialCategoryId = materialCategoryId;
	}

	public Integer getMaterialSize() {
		return materialSize;
	}

	public void setMaterialSize(Integer materialSize) {
		this.materialSize = materialSize;
	}

	public String getTgzUrl() {
		return tgzUrl;
	}

	public void setTgzUrl(String tgzUrl) {
		this.tgzUrl = tgzUrl;
	}

	public String getUnzipTgzRootPathUrl() {
		return unzipTgzRootPathUrl;
	}

	public void setUnzipTgzRootPathUrl(String unzipTgzRootPathUrl) {
		this.unzipTgzRootPathUrl = unzipTgzRootPathUrl;
	}

	public String getUnzipTgzFiles() {
		return unzipTgzFiles;
	}

	public void setUnzipTgzFiles(String unzipTgzFiles) {
		this.unzipTgzFiles = unzipTgzFiles;
	}
}
