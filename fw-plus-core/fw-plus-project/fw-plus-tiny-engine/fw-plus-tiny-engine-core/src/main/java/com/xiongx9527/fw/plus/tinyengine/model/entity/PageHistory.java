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

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonArrayTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonObjectTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockVersionDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 页面历史
 * </p>
 *
 * @author lu -yg
 * @since 2024-10-17
 */
@TableName(value = "t_page_history", resultMap = "PageHistoryMap", autoResultMap = true)
public class PageHistory {
@TableId(value = "id", type = IdType.AUTO)
    private Integer id;
private String name;
@TableField(value = "ref_id")
    private Integer page;
private String version;
@TableField(value = "app_id")
    private Integer app;
private String route;
@JsonProperty("page_content")
    @TableField(typeHandler = JsonObjectTypeHandler.class)
    private Map<String, Object> pageContent;
private Boolean isBody;
private String parentId;

    @TableField(value = "`group`")
    private String group;
private Integer depth;
private Boolean isPage;
private Boolean isDefault;
private String message;
private Boolean isHome;
private Boolean isPublished;
@TableField(typeHandler = JsonArrayTypeHandler.class)
    private List<BlockVersionDto> contentBlocks;


    @TableField(fill = FieldFill.INSERT)
private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
private String lastUpdatedBy;

    @TableField(fill = FieldFill.INSERT)
@JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("updated_at")
private LocalDateTime lastUpdatedTime;

    @TableField(fill = FieldFill.INSERT)
private String tenantId;

    @TableField(fill = FieldFill.INSERT)
private String renterId;
private String siteId;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getPage() {
		return page;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
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

	public Boolean getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(Boolean isDefault) {
		this.isDefault = isDefault;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Boolean getIsHome() {
		return isHome;
	}

	public void setIsHome(Boolean isHome) {
		this.isHome = isHome;
	}

	public Boolean getIsPublished() {
		return isPublished;
	}

	public void setIsPublished(Boolean isPublished) {
		this.isPublished = isPublished;
	}

	public List<BlockVersionDto> getContentBlocks() {
		return contentBlocks;
	}

	public void setContentBlocks(List<BlockVersionDto> contentBlocks) {
		this.contentBlocks = contentBlocks;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getLastUpdatedBy() {
		return lastUpdatedBy;
	}

	public void setLastUpdatedBy(String lastUpdatedBy) {
		this.lastUpdatedBy = lastUpdatedBy;
	}

	public LocalDateTime getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(LocalDateTime createdTime) {
		this.createdTime = createdTime;
	}

	public LocalDateTime getLastUpdatedTime() {
		return lastUpdatedTime;
	}

	public void setLastUpdatedTime(LocalDateTime lastUpdatedTime) {
		this.lastUpdatedTime = lastUpdatedTime;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getRenterId() {
		return renterId;
	}

	public void setRenterId(String renterId) {
		this.renterId = renterId;
	}

	public String getSiteId() {
		return siteId;
	}

	public void setSiteId(String siteId) {
		this.siteId = siteId;
	}
}
