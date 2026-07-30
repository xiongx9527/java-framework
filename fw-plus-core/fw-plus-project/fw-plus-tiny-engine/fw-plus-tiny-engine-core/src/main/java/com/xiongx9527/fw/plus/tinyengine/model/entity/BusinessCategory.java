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

package com.xiongx9527.fw.plus.tinyengine.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiongx9527.fw.plus.tinyengine.common.model.BaseEntity;

/**
 * <p>
 * 业务类型表，全局
 * </p>
 *
 * @author zhangjuncao
 * @since 2024-10-17
 */
@TableName("t_business_category")
public class BusinessCategory extends BaseEntity {
private String code;
private String name;
private String businessGroup;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBusinessGroup() {
		return businessGroup;
	}

	public void setBusinessGroup(String businessGroup) {
		this.businessGroup = businessGroup;
	}
}
