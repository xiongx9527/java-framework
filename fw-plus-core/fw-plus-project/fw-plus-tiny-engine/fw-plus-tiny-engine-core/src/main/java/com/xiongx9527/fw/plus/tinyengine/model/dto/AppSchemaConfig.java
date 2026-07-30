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
 * AppSchemaConfig
 *
 * @since 2024-10-20
 */
public class AppSchemaConfig {
    private String sdkVersion;
    private String historyMode;
    private String targetRootID;

	public String getSdkVersion() {
		return sdkVersion;
	}

	public void setSdkVersion(String sdkVersion) {
		this.sdkVersion = sdkVersion;
	}

	public String getHistoryMode() {
		return historyMode;
	}

	public void setHistoryMode(String historyMode) {
		this.historyMode = historyMode;
	}

	public String getTargetRootID() {
		return targetRootID;
	}

	public void setTargetRootID(String targetRootID) {
		this.targetRootID = targetRootID;
	}
}
