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

import com.xiongx9527.fw.plus.tinyengine.model.entity.Component;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ComponentLibrary;

import java.util.List;

/**
 * BundleResultDto
 *
 * @since 2025-04-02
 */
public class BundleResultDto {
    private List<ComponentLibrary> packageList;
    private List<Component> componentList;

	public List<ComponentLibrary> getPackageList() {
		return packageList;
	}

	public void setPackageList(List<ComponentLibrary> packageList) {
		this.packageList = packageList;
	}

	public List<Component> getComponentList() {
		return componentList;
	}

	public void setComponentList(List<Component> componentList) {
		this.componentList = componentList;
	}
}
