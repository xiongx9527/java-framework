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
import java.util.Map;

/**
 * The type Preview dto.
 *
 * @since 2024-10-20
 */
public class PreviewDto {
    private Map<String, Object> dataSource;
    private List<Map<String, Object>> globalState;
    private SchemaI18n i18n;
    private List<SchemaUtils> utils;

	public Map<String, Object> getDataSource() {
		return dataSource;
	}

	public void setDataSource(Map<String, Object> dataSource) {
		this.dataSource = dataSource;
	}

	public List<Map<String, Object>> getGlobalState() {
		return globalState;
	}

	public void setGlobalState(List<Map<String, Object>> globalState) {
		this.globalState = globalState;
	}

	public SchemaI18n getI18n() {
		return i18n;
	}

	public void setI18n(SchemaI18n i18n) {
		this.i18n = i18n;
	}

	public List<SchemaUtils> getUtils() {
		return utils;
	}

	public void setUtils(List<SchemaUtils> utils) {
		this.utils = utils;
	}
}
