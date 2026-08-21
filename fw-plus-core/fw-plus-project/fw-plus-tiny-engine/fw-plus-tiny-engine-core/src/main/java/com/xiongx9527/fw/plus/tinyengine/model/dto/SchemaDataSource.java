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

import com.xiongx9527.fw.plus.tinyengine.model.entity.Datasource;

import java.util.List;
import java.util.Map;

/**
 * SchemaDataSource
 *
 * @since 2024-10-20
 */
public class SchemaDataSource {
    private Map<String, Object> dataHandler;
    private List<Datasource> list;

	public Map<String, Object> getDataHandler() {
		return dataHandler;
	}

	public void setDataHandler(Map<String, Object> dataHandler) {
		this.dataHandler = dataHandler;
	}

	public List<Datasource> getList() {
		return list;
	}

	public void setList(List<Datasource> list) {
		this.list = list;
	}
}
