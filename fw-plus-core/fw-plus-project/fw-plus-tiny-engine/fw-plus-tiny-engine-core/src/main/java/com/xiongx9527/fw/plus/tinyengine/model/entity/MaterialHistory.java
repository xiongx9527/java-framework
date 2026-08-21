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
import com.xiongx9527.fw.plus.tinyengine.common.model.HistoryEntity;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonObjectTypeHandler;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 物料历史表
 * </p>
 *
 * @author lu -yg
 * @since 2024-10-17
 */
@TableName(value = "t_material_history", resultMap = "MaterialHistoryMap", autoResultMap = true)
public class MaterialHistory extends HistoryEntity {
@TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> content;
private String name;
private String npmName;
private String framework;

    @JsonProperty("assets_url")
    @TableField(typeHandler = JsonObjectTypeHandler.class)
private Map<String, Object> assetsUrl;
private String imageUrl;

    @JsonProperty("build_info")
    @TableField(typeHandler = JsonObjectTypeHandler.class)
private Map<String, Object> buildInfo;
private String description;
private String materialSize;
private String tgzUrl;
private String unzipTgzRootPathUrl;
private String unzipTgzFiles;
@TableField(exist = false)
private List<Component> components;

	public Map<String, Object> getContent() {
		return content;
	}

	public void setContent(Map<String, Object> content) {
		this.content = content;
	}

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

	public Map<String, Object> getBuildInfo() {
		return buildInfo;
	}

	public void setBuildInfo(Map<String, Object> buildInfo) {
		this.buildInfo = buildInfo;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getMaterialSize() {
		return materialSize;
	}

	public void setMaterialSize(String materialSize) {
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

	public List<Component> getComponents() {
		return components;
	}

	public void setComponents(List<Component> components) {
		this.components = components;
	}
}
