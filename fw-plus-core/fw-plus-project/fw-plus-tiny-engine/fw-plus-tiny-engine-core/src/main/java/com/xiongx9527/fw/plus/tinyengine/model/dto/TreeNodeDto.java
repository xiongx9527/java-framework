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
 * TreeNodeDto
 *
 * @since 2024-10-20
 */
public class TreeNodeDto {
    private List<Integer> pids;
    private Integer level;
    private TreeNodeCollection collection;

	public List<Integer> getPids() {
		return pids;
	}

	public void setPids(List<Integer> pids) {
		this.pids = pids;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public TreeNodeCollection getCollection() {
		return collection;
	}

	public void setCollection(TreeNodeCollection collection) {
		this.collection = collection;
	}
}
