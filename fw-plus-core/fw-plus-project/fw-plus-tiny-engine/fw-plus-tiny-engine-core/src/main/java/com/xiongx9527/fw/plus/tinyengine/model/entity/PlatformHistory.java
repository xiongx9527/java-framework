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

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiongx9527.fw.plus.tinyengine.common.model.BaseEntity;

/**
 * 设计器历史表
 *
 * @since 2025-05-09
 */
@TableName("t_platform_history")
public class PlatformHistory extends BaseEntity {
private Integer refId;
private String name;
private String publishUrl;
private String description;
private String version;
private Integer subCount;
private Integer materialHistoryId;
private String imageUrl;
private String materialPkgName;
private String  materialVersion;
private String vscodeUrl;

	public Integer getRefId() {
		return refId;
	}

	public void setRefId(Integer refId) {
		this.refId = refId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPublishUrl() {
		return publishUrl;
	}

	public void setPublishUrl(String publishUrl) {
		this.publishUrl = publishUrl;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Integer getSubCount() {
		return subCount;
	}

	public void setSubCount(Integer subCount) {
		this.subCount = subCount;
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

	public String getMaterialPkgName() {
		return materialPkgName;
	}

	public void setMaterialPkgName(String materialPkgName) {
		this.materialPkgName = materialPkgName;
	}

	public String getMaterialVersion() {
		return materialVersion;
	}

	public void setMaterialVersion(String materialVersion) {
		this.materialVersion = materialVersion;
	}

	public String getVscodeUrl() {
		return vscodeUrl;
	}

	public void setVscodeUrl(String vscodeUrl) {
		this.vscodeUrl = vscodeUrl;
	}
}
