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

/**
 * 页面历史数据
 *
 * @since 2024-10-20
 */
public class PublishedPageVo {
    private Integer refId;

    /**
     * 名称
     */
    private String name;

    /**
     * 版本
     */
    private List<PageHistoryVo> historyVersions;

    /**
     * 关联app表Id
     */
    private Integer appId;

    /**
     * 页面路由
     */
    private String route;

    /**
     * 父文件夹id
     */
    private String parentId;

    private String group;

    /**
     * 是否为页面：分为页面和文件夹
     */
    private Boolean isPage;

    /**
     * 页面id
     *
     * @return page id
     */
    public Integer getPageId() {
        return this.getRefId();
    }

	public Integer getRefId() {
		return refId;
	}

	public void setRefId(Integer refId) {
		this.refId = refId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<PageHistoryVo> getHistoryVersions() {
		return historyVersions;
	}

	public void setHistoryVersions(List<PageHistoryVo> historyVersions) {
		this.historyVersions = historyVersions;
	}

	public Integer getAppId() {
		return appId;
	}

	public void setAppId(Integer appId) {
		this.appId = appId;
	}

	public String getRoute() {
		return route;
	}

	public void setRoute(String route) {
		this.route = route;
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

	public Boolean getIsPage() {
		return isPage;
	}

	public void setIsPage(Boolean isPage) {
		this.isPage = isPage;
	}
}
