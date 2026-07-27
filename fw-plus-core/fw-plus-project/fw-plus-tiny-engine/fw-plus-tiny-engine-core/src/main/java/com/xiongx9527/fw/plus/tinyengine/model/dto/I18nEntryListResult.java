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

import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nLang;

import java.util.List;

/**
 * The type 18 n entry list result.
 *
 * @since 2024-10-20
 */
public class I18nEntryListResult {
    private SchemaI18n messages;
    private List<I18nLang> i18nLangsList;

	public SchemaI18n getMessages() {
		return messages;
	}

	public void setMessages(SchemaI18n messages) {
		this.messages = messages;
	}

	public List<I18nLang> getI18nLangsList() {
		return i18nLangsList;
	}

	public void setI18nLangsList(List<I18nLang> i18nLangsList) {
		this.i18nLangsList = i18nLangsList;
	}
}
