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
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonObjectTypeHandler;

import java.util.Map;

/**
 * <p>
 * 页面模板表
 * </p>
 *
 * @author zhangjuncao
 * @since 2024-10-17
 */
@TableName("t_page_template")
public class PageTemplate extends BaseEntity {
private String name;
@TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> pageContent;
private String framework;
private Integer published;
private Integer publicStatus;
private String type;
private String status;
private Integer isPreset;
private String imageUrl;
private String description;
private Integer platformId;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, Object> getPageContent() {
		return pageContent;
	}

	public void setPageContent(Map<String, Object> pageContent) {
		this.pageContent = pageContent;
	}

	public String getFramework() {
		return framework;
	}

	public void setFramework(String framework) {
		this.framework = framework;
	}

	public Integer getPublished() {
		return published;
	}

	public void setPublished(Integer published) {
		this.published = published;
	}

	public Integer getPublicStatus() {
		return publicStatus;
	}

	public void setPublicStatus(Integer publicStatus) {
		this.publicStatus = publicStatus;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getIsPreset() {
		return isPreset;
	}

	public void setIsPreset(Integer isPreset) {
		this.isPreset = isPreset;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getPlatformId() {
		return platformId;
	}

	public void setPlatformId(Integer platformId) {
		this.platformId = platformId;
	}
}
