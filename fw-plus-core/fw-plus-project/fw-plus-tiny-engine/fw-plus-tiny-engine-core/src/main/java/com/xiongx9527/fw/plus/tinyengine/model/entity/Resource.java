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
import com.xiongx9527.fw.plus.tinyengine.common.model.BaseEntity;
/**
 *
 * 资源表
 *
 * @since 2025-09-03
 */
@TableName("t_resource")
public class Resource extends BaseEntity {
private Integer appId;
private Integer platformId;

    @TableField(exist = false)
private Integer resourceGroupId;
private String name;
private String thumbnailName;
private String resourceUrl;
private String thumbnailUrl;
private String hash;
private String category;
private String description;
private String thumbnailData;
private String resourceData;
private Integer publicStatus;
private Boolean isDefault;

	public Integer getAppId() {
		return appId;
	}

	public void setAppId(Integer appId) {
		this.appId = appId;
	}

	public Integer getPlatformId() {
		return platformId;
	}

	public void setPlatformId(Integer platformId) {
		this.platformId = platformId;
	}

	public Integer getResourceGroupId() {
		return resourceGroupId;
	}

	public void setResourceGroupId(Integer resourceGroupId) {
		this.resourceGroupId = resourceGroupId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getThumbnailName() {
		return thumbnailName;
	}

	public void setThumbnailName(String thumbnailName) {
		this.thumbnailName = thumbnailName;
	}

	public String getResourceUrl() {
		return resourceUrl;
	}

	public void setResourceUrl(String resourceUrl) {
		this.resourceUrl = resourceUrl;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getThumbnailData() {
		return thumbnailData;
	}

	public void setThumbnailData(String thumbnailData) {
		this.thumbnailData = thumbnailData;
	}

	public String getResourceData() {
		return resourceData;
	}

	public void setResourceData(String resourceData) {
		this.resourceData = resourceData;
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
}
