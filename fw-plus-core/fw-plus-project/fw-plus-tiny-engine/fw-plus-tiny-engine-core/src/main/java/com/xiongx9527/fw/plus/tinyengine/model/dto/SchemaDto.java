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
 * SchemaDto
 *
 * @since 2024-10-20
 */
public class SchemaDto {
    private List<SchemaUtils> bridge;
    private List<Map<String, Object>> componentsMap;
    private List<ComponentTree> componentsTree;
    private Map<String, Object> config;
    private String constants;
    private String css;
    private Map<String, Object> dataSource;
    private SchemaI18n i18n;
    private SchemaMeta meta;
    private List<SchemaUtils> utils;
    private String version;
    private List<PackagesDto> packages;

	public List<SchemaUtils> getBridge() {
		return bridge;
	}

	public void setBridge(List<SchemaUtils> bridge) {
		this.bridge = bridge;
	}

	public List<Map<String, Object>> getComponentsMap() {
		return componentsMap;
	}

	public void setComponentsMap(List<Map<String, Object>> componentsMap) {
		this.componentsMap = componentsMap;
	}

	public List<ComponentTree> getComponentsTree() {
		return componentsTree;
	}

	public void setComponentsTree(List<ComponentTree> componentsTree) {
		this.componentsTree = componentsTree;
	}

	public Map<String, Object> getConfig() {
		return config;
	}

	public void setConfig(Map<String, Object> config) {
		this.config = config;
	}

	public String getConstants() {
		return constants;
	}

	public void setConstants(String constants) {
		this.constants = constants;
	}

	public String getCss() {
		return css;
	}

	public void setCss(String css) {
		this.css = css;
	}

	public Map<String, Object> getDataSource() {
		return dataSource;
	}

	public void setDataSource(Map<String, Object> dataSource) {
		this.dataSource = dataSource;
	}

	public SchemaI18n getI18n() {
		return i18n;
	}

	public void setI18n(SchemaI18n i18n) {
		this.i18n = i18n;
	}

	public SchemaMeta getMeta() {
		return meta;
	}

	public void setMeta(SchemaMeta meta) {
		this.meta = meta;
	}

	public List<SchemaUtils> getUtils() {
		return utils;
	}

	public void setUtils(List<SchemaUtils> utils) {
		this.utils = utils;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<PackagesDto> getPackages() {
		return packages;
	}

	public void setPackages(List<PackagesDto> packages) {
		this.packages = packages;
	}
}
