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

package com.xiongx9527.fw.plus.tinyengine.model.dto;

import java.util.List;

/**
 * The type Method dto.
 *
 * @since 2025-07-17
 */
public class MethodDto {
    private String name;
    private String nameEn;
    private List<RequestParameter> requestParameters;
    private List<ResponseParameter> responseParameters;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNameEn() {
		return nameEn;
	}

	public void setNameEn(String nameEn) {
		this.nameEn = nameEn;
	}

	public List<RequestParameter> getRequestParameters() {
		return requestParameters;
	}

	public void setRequestParameters(List<RequestParameter> requestParameters) {
		this.requestParameters = requestParameters;
	}

	public List<ResponseParameter> getResponseParameters() {
		return responseParameters;
	}

	public void setResponseParameters(List<ResponseParameter> responseParameters) {
		this.responseParameters = responseParameters;
	}
}
