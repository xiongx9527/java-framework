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
 * The type EntriesItem.
 *
 * @since 2024-10-20
 */
public class EntriesItem {
    private Integer lang;
    private Map<String, Object> entries;

	public Integer getLang() {
		return lang;
	}

	public void setLang(Integer lang) {
		this.lang = lang;
	}

	public Map<String, Object> getEntries() {
		return entries;
	}

	public void setEntries(Map<String, Object> entries) {
		this.entries = entries;
	}
}
