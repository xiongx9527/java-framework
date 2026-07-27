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
 * <p>
 * i18n文件结果dto
 * </p>
 *
 * @author zhangjuncao
 * @since 2024-10-17
 */
public class FileResult {
    private Integer insertNum;
    private Integer updateNum;

	public Integer getInsertNum() {
		return insertNum;
	}

	public void setInsertNum(Integer insertNum) {
		this.insertNum = insertNum;
	}

	public Integer getUpdateNum() {
		return updateNum;
	}

	public void setUpdateNum(Integer updateNum) {
		this.updateNum = updateNum;
	}
}
