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
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonArrayTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.model.dto.MethodDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.ParametersDto;
import java.util.List;

/**
 * 模型表
 *
 * @since 2025-07-17
 */
@TableName("t_model")
public class Model extends BaseEntity {
private Integer appId;
private Integer platformId;
private String nameCn;
private String nameEn;
private String version;
private String modelUrl;
@TableField(typeHandler = JsonArrayTypeHandler.class)
    private List<ParametersDto> parameters;
@TableField(typeHandler = JsonArrayTypeHandler.class)
    private  List<MethodDto> method;
private String description;


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

	public String getNameCn() {
		return nameCn;
	}

	public void setNameCn(String nameCn) {
		this.nameCn = nameCn;
	}

	public String getNameEn() {
		return nameEn;
	}

	public void setNameEn(String nameEn) {
		this.nameEn = nameEn;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getModelUrl() {
		return modelUrl;
	}

	public void setModelUrl(String modelUrl) {
		this.modelUrl = modelUrl;
	}

	public List<ParametersDto> getParameters() {
		return parameters;
	}

	public void setParameters(List<ParametersDto> parameters) {
		this.parameters = parameters;
	}

	public List<MethodDto> getMethod() {
		return method;
	}

	public void setMethod(List<MethodDto> method) {
		this.method = method;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
