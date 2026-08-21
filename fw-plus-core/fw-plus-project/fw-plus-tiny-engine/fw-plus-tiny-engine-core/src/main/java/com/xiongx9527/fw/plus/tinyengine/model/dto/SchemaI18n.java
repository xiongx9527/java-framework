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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * SchemaI18n
 *
 * @since 2024-10-20
 */
public class SchemaI18n {
    @JsonProperty("en_US")
    private Map<String, String> enUs;
    @JsonProperty("zh_CN")
    private Map<String, String> zhCn;

	public Map<String, String> getEnUs() {
		return enUs;
	}

	public void setEnUs(Map<String, String> enUs) {
		this.enUs = enUs;
	}

	public Map<String, String> getZhCn() {
		return zhCn;
	}

	public void setZhCn(Map<String, String> zhCn) {
		this.zhCn = zhCn;
	}
}
