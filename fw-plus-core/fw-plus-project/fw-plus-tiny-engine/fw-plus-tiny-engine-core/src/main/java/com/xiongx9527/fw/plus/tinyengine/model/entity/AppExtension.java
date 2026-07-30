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
 * 应用扩展
 * </p>
 *
 * @author lu -yg
 * @since 2024-10-17
 */
@TableName(value = "t_app_extension", autoResultMap = true)
public class AppExtension extends BaseEntity {
private Integer platformId;
private String name;
private String type;
@TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> content;
private Integer app;
private String category;

	public Integer getPlatformId() {
		return platformId;
	}

	public void setPlatformId(Integer platformId) {
		this.platformId = platformId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, Object> getContent() {
		return content;
	}

	public void setContent(Map<String, Object> content) {
		this.content = content;
	}

	public Integer getApp() {
		return app;
	}

	public void setApp(Integer app) {
		this.app = app;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
}
