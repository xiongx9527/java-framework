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

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xiongx9527.fw.plus.tinyengine.common.model.BaseEntity;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 区块分组dto
 * </p>
 *
 * @author zhangjuncao
 * @since 2024-10-17
 */
@TableName("t_block_group")
public class BlockGroupDto extends BaseEntity {
    private static final long serialVersionUID = 1L;
private String name;
@JsonProperty("app_id")
    private Integer appId;
@JsonProperty("platform_id")
    private Integer platformId;
private String description;

    @JsonProperty("app")
    private App app;

    @TableField(exist = false)
private List<BlockDto> blocks = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAppId() {
		return appId;
	}

	public void setAppId(Integer appId) {
		this.appId = appId;
	}

	public Integer getPlatformId() {
		return platformId;
	}

	public void setPlatformId(Integer platformId) {
		this.platformId = platformId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public App getApp() {
		return app;
	}

	public void setApp(App app) {
		this.app = app;
	}

	public List<BlockDto> getBlocks() {
		return blocks;
	}

	public void setBlocks(List<BlockDto> blocks) {
		this.blocks = blocks;
	}
}