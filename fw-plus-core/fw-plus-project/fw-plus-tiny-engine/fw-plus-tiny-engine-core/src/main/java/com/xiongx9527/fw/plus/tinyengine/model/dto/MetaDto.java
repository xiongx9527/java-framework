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

import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AppExtension;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Datasource;
import com.xiongx9527.fw.plus.tinyengine.model.entity.MaterialHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;

import java.util.List;

/**
 * The type Meta dto.
 *
 * @since 2024-10-20
 */
public class MetaDto {
    private App app;
    private List<I18nEntryDto> i18n;
    private List<Datasource> source;
    private List<AppExtension> extension;
    private List<Page> pages;
    private List<BlockHistory> blockHistories;
    private MaterialHistory materialHistory;

	public App getApp() {
		return app;
	}

	public void setApp(App app) {
		this.app = app;
	}

	public List<I18nEntryDto> getI18n() {
		return i18n;
	}

	public void setI18n(List<I18nEntryDto> i18n) {
		this.i18n = i18n;
	}

	public List<Datasource> getSource() {
		return source;
	}

	public void setSource(List<Datasource> source) {
		this.source = source;
	}

	public List<AppExtension> getExtension() {
		return extension;
	}

	public void setExtension(List<AppExtension> extension) {
		this.extension = extension;
	}

	public List<Page> getPages() {
		return pages;
	}

	public void setPages(List<Page> pages) {
		this.pages = pages;
	}

	public List<BlockHistory> getBlockHistories() {
		return blockHistories;
	}

	public void setBlockHistories(List<BlockHistory> blockHistories) {
		this.blockHistories = blockHistories;
	}

	public MaterialHistory getMaterialHistory() {
		return materialHistory;
	}

	public void setMaterialHistory(MaterialHistory materialHistory) {
		this.materialHistory = materialHistory;
	}
}
