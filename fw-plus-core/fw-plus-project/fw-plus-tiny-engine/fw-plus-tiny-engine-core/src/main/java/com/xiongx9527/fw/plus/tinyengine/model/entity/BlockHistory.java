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

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xiongx9527.fw.plus.tinyengine.common.model.HistoryEntity;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonArrayTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonObjectTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockVersionDto;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 区块历史表
 * </p>
 *
 * @author lu -yg
 * @since 2024-10-17
 */
@TableName(value = "t_block_history", autoResultMap = true)
public class BlockHistory extends HistoryEntity {
@TableId(value = "id", type = IdType.AUTO)
    private Integer id;
private String message;
private String label;
private String name;
private String framework;

    @TableField(typeHandler = JsonObjectTypeHandler.class)
private Map<String, Object> content;
@TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> assets;
@TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> buildInfo;
private String screenshot;
private String path;
private String description;
private String tags;
private Boolean isOfficial;

    @JsonProperty("public")
private Integer isPublic;
private Boolean isDefault;
private Boolean isTinyReserved;
private String mode;
private Integer platformId;
private Integer appId;
private String npmName;
@TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> i18n;
@TableField(typeHandler = JsonArrayTypeHandler.class)
    private List<BlockVersionDto> contentBlocks;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFramework() {
		return framework;
	}

	public void setFramework(String framework) {
		this.framework = framework;
	}

	public Map<String, Object> getContent() {
		return content;
	}

	public void setContent(Map<String, Object> content) {
		this.content = content;
	}

	public Map<String, Object> getAssets() {
		return assets;
	}

	public void setAssets(Map<String, Object> assets) {
		this.assets = assets;
	}

	public Map<String, Object> getBuildInfo() {
		return buildInfo;
	}

	public void setBuildInfo(Map<String, Object> buildInfo) {
		this.buildInfo = buildInfo;
	}

	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public Boolean getIsOfficial() {
		return isOfficial;
	}

	public void setIsOfficial(Boolean isOfficial) {
		this.isOfficial = isOfficial;
	}

	public Integer getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(Integer isPublic) {
		this.isPublic = isPublic;
	}

	public Boolean getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(Boolean isDefault) {
		this.isDefault = isDefault;
	}

	public Boolean getIsTinyReserved() {
		return isTinyReserved;
	}

	public void setIsTinyReserved(Boolean isTinyReserved) {
		this.isTinyReserved = isTinyReserved;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public Integer getPlatformId() {
		return platformId;
	}

	public void setPlatformId(Integer platformId) {
		this.platformId = platformId;
	}

	public Integer getAppId() {
		return appId;
	}

	public void setAppId(Integer appId) {
		this.appId = appId;
	}

	public String getNpmName() {
		return npmName;
	}

	public void setNpmName(String npmName) {
		this.npmName = npmName;
	}

	public Map<String, Object> getI18n() {
		return i18n;
	}

	public void setI18n(Map<String, Object> i18n) {
		this.i18n = i18n;
	}

	public List<BlockVersionDto> getContentBlocks() {
		return contentBlocks;
	}

	public void setContentBlocks(List<BlockVersionDto> contentBlocks) {
		this.contentBlocks = contentBlocks;
	}
}
