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
 * Snippet
 *
 * @since 2024-11-13
 */
public class Snippet {
    private Map<String, String> name;
    private String icon;
    private String screenshot;
    private String snippetName;
    private Map<String, Object> schema;

	public Map<String, String> getName() {
		return name;
	}

	public void setName(Map<String, String> name) {
		this.name = name;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}

	public String getSnippetName() {
		return snippetName;
	}

	public void setSnippetName(String snippetName) {
		this.snippetName = snippetName;
	}

	public Map<String, Object> getSchema() {
		return schema;
	}

	public void setSchema(Map<String, Object> schema) {
		this.schema = schema;
	}
}
