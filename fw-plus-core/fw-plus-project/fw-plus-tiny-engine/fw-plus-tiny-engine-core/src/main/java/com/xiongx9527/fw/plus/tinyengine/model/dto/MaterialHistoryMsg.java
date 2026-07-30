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

/**
 * The type Material history msg.
 *
 * @since 2024-10-20
 */
public class MaterialHistoryMsg {
private Boolean isUnpkg;
private Integer materialHistoryId;

	public Boolean getIsUnpkg() {
		return isUnpkg;
	}

	public void setIsUnpkg(Boolean isUnpkg) {
		this.isUnpkg = isUnpkg;
	}

	public Integer getMaterialHistoryId() {
		return materialHistoryId;
	}

	public void setMaterialHistoryId(Integer materialHistoryId) {
		this.materialHistoryId = materialHistoryId;
	}
}
