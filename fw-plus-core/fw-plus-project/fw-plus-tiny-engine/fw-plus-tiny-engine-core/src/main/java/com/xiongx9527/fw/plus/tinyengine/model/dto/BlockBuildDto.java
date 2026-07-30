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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The type BlockBuildDto.
 *
 * @since 2024-12-19
 */
public class BlockBuildDto {
    private BlockDto block;
    @JsonProperty("deploy_info")
    private String deployInfo;
    @JsonProperty("is_compile")
    private Boolean isCompile;
    private Boolean needToSave;
    private String version;

	public BlockDto getBlock() {
		return block;
	}

	public void setBlock(BlockDto block) {
		this.block = block;
	}

	public String getDeployInfo() {
		return deployInfo;
	}

	public void setDeployInfo(String deployInfo) {
		this.deployInfo = deployInfo;
	}

	public Boolean getIsCompile() {
		return isCompile;
	}

	public void setIsCompile(Boolean isCompile) {
		this.isCompile = isCompile;
	}

	public Boolean getNeedToSave() {
		return needToSave;
	}

	public void setNeedToSave(Boolean needToSave) {
		this.needToSave = needToSave;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
