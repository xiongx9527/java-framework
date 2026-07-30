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
 * The type I18n file dto.
 *
 * @since 2024-10-20
 */
public class FileInfo {
    private String name;
    private String content;
    private Boolean isDirectory;

    /**
     * Instantiates a new i18n file  body dto.
     *
     * @param name the name
     * @param isDirectory the isDirectory
     * @param content the content
     */
    public FileInfo(String name, String content, boolean isDirectory) {
        this.name = name;
        this.content = content;
        this.isDirectory = isDirectory;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Boolean getIsDirectory() {
		return isDirectory;
	}

	public void setIsDirectory(Boolean isDirectory) {
		this.isDirectory = isDirectory;
	}
}