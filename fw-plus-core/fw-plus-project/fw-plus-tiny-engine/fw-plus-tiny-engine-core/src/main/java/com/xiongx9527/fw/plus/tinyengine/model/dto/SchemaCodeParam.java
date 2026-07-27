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

import java.util.Map;

/**
 * The type Schema code param.
 *
 * @since 2024-10-20
 */
public class SchemaCodeParam {
    private Integer app;
    private Map<String, Object> pageInfo;
    private Integer platform;

	public Integer getApp() {
		return app;
	}

	public void setApp(Integer app) {
		this.app = app;
	}

	public Map<String, Object> getPageInfo() {
		return pageInfo;
	}

	public void setPageInfo(Map<String, Object> pageInfo) {
		this.pageInfo = pageInfo;
	}

	public Integer getPlatform() {
		return platform;
	}

	public void setPlatform(Integer platform) {
		this.platform = platform;
	}
}
