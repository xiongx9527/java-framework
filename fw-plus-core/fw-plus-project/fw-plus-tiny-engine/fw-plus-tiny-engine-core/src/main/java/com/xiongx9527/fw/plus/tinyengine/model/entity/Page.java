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

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xiongx9527.fw.plus.tinyengine.common.model.BaseEntity;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonArrayTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonObjectTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockVersionDto;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 页面表
 * </p>
 *
 * @author zhangjuncao
 * @since 2024-10-17
 */
@TableName(value = "t_page", autoResultMap = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Page extends BaseEntity {
private String name;
private Integer app;
private String route;
@JsonProperty("page_content")
    @TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> pageContent;
private Boolean isBody;
private String parentId;
private String group;
private Integer depth;
private Boolean isPage;
private String occupierBy;
private Boolean isDefault;
@TableField(typeHandler = JsonArrayTypeHandler.class)
    private List<BlockVersionDto> contentBlocks;
private String latestVersion;
private Integer latestHistoryId;
private User occupier;

    @TableField(exist = false)
    private Boolean isHome;

    @TableField(exist = false)
    private Map<String, List<String>> assets;

    @TableField(exist = false)
    private String message;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getApp() {
		return app;
	}

	public void setApp(Integer app) {
		this.app = app;
	}

	public String getRoute() {
		return route;
	}

	public void setRoute(String route) {
		this.route = route;
	}

	public Map<String, Object> getPageContent() {
		return pageContent;
	}

	public void setPageContent(Map<String, Object> pageContent) {
		this.pageContent = pageContent;
	}

	public Boolean getIsBody() {
		return isBody;
	}

	public void setIsBody(Boolean isBody) {
		this.isBody = isBody;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public Integer getDepth() {
		return depth;
	}

	public void setDepth(Integer depth) {
		this.depth = depth;
	}

	public Boolean getIsPage() {
		return isPage;
	}

	public void setIsPage(Boolean isPage) {
		this.isPage = isPage;
	}

	public String getOccupierBy() {
		return occupierBy;
	}

	public void setOccupierBy(String occupierBy) {
		this.occupierBy = occupierBy;
	}

	public Boolean getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(Boolean isDefault) {
		this.isDefault = isDefault;
	}

	public List<BlockVersionDto> getContentBlocks() {
		return contentBlocks;
	}

	public void setContentBlocks(List<BlockVersionDto> contentBlocks) {
		this.contentBlocks = contentBlocks;
	}

	public String getLatestVersion() {
		return latestVersion;
	}

	public void setLatestVersion(String latestVersion) {
		this.latestVersion = latestVersion;
	}

	public Integer getLatestHistoryId() {
		return latestHistoryId;
	}

	public void setLatestHistoryId(Integer latestHistoryId) {
		this.latestHistoryId = latestHistoryId;
	}

	public User getOccupier() {
		return occupier;
	}

	public void setOccupier(User occupier) {
		this.occupier = occupier;
	}

	public Boolean getIsHome() {
		return isHome;
	}

	public void setIsHome(Boolean isHome) {
		this.isHome = isHome;
	}

	public Map<String, List<String>> getAssets() {
		return assets;
	}

	public void setAssets(Map<String, List<String>> assets) {
		this.assets = assets;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
