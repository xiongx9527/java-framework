package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.datascope;

public class DataScopeProperties {
	private boolean enabled = true;
	private Integer scopeWhenMissingConfig;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Integer getScopeWhenMissingConfig() {
		return scopeWhenMissingConfig;
	}

	public void setScopeWhenMissingConfig(Integer scopeWhenMissingConfig) {
		this.scopeWhenMissingConfig = scopeWhenMissingConfig;
	}
}
