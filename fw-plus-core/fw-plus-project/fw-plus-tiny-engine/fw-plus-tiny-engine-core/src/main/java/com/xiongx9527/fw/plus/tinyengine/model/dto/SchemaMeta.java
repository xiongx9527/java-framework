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
import java.util.Map;

/**
 * SchemaMeta
 *
 * @since 2024-10-20
 */
public class SchemaMeta {
    private String appId;
    private String branch;
    private String creator;
    private String description;
    private String name;
    private String gitGroup;
    private List<Map<String, Object>> globalState;
    private String projectName;
    private Integer tenant;
    private String gmtCreate;
    private String gmtModified;
    private String isDemo;

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getGitGroup() {
		return gitGroup;
	}

	public void setGitGroup(String gitGroup) {
		this.gitGroup = gitGroup;
	}

	public List<Map<String, Object>> getGlobalState() {
		return globalState;
	}

	public void setGlobalState(List<Map<String, Object>> globalState) {
		this.globalState = globalState;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public Integer getTenant() {
		return tenant;
	}

	public void setTenant(Integer tenant) {
		this.tenant = tenant;
	}

	public String getGmtCreate() {
		return gmtCreate;
	}

	public void setGmtCreate(String gmtCreate) {
		this.gmtCreate = gmtCreate;
	}

	public String getGmtModified() {
		return gmtModified;
	}

	public void setGmtModified(String gmtModified) {
		this.gmtModified = gmtModified;
	}

	public String getIsDemo() {
		return isDemo;
	}

	public void setIsDemo(String isDemo) {
		this.isDemo = isDemo;
	}
}
