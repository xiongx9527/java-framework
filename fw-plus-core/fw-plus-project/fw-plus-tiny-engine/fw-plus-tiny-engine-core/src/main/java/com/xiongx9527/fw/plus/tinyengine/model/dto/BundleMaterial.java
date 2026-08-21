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
 * BundleMaterial
 *
 * @since 2025-04-02
 */
public class BundleMaterial {
    private List<Map<String, Object>> components;
    private List<Child> snippets;
    private List<Map<String, Object>> blocks;
    private List<Map<String, Object>> packages;

	public List<Map<String, Object>> getComponents() {
		return components;
	}

	public void setComponents(List<Map<String, Object>> components) {
		this.components = components;
	}

	public List<Child> getSnippets() {
		return snippets;
	}

	public void setSnippets(List<Child> snippets) {
		this.snippets = snippets;
	}

	public List<Map<String, Object>> getBlocks() {
		return blocks;
	}

	public void setBlocks(List<Map<String, Object>> blocks) {
		this.blocks = blocks;
	}

	public List<Map<String, Object>> getPackages() {
		return packages;
	}

	public void setPackages(List<Map<String, Object>> packages) {
		this.packages = packages;
	}
}
