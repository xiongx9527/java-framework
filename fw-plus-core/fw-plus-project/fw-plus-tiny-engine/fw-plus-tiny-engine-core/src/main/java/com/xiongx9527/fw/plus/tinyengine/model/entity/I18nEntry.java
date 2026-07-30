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

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xiongx9527.fw.plus.tinyengine.common.model.BaseEntity;

/**
 * <p>
 * 国际化语言配置表
 * </p>
 *
 * @author lu -yg
 * @since 2024-10-17
 */
@TableName("t_i18n_entry")
public class I18nEntry extends BaseEntity {
private String key;
private String content;
private Integer host;
@JsonProperty("host_type")
    private String hostType;
private Integer lang;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Integer getHost() {
		return host;
	}

	public void setHost(Integer host) {
		this.host = host;
	}

	public String getHostType() {
		return hostType;
	}

	public void setHostType(String hostType) {
		this.hostType = hostType;
	}

	public Integer getLang() {
		return lang;
	}

	public void setLang(Integer lang) {
		this.lang = lang;
	}
}
