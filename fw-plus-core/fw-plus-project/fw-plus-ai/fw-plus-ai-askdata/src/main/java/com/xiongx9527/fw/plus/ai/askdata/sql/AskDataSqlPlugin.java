package com.xiongx9527.fw.plus.ai.askdata.sql;

import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityDescriptor;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityProvider;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType;

/**
 * SQL 管线插件的公共能力契约。
 */
public interface AskDataSqlPlugin extends AskDataCapabilityProvider {

	String getKey();

	int getOrder();

	AskDataSqlPhase getPhase();

	/**
	 * 默认把插件注册为当前 Core 版本的集群兼容 SQL 能力；有独立版本约束的插件可覆盖此描述符。
	 */
	@Override
	default AskDataCapabilityDescriptor getDescriptor() {
		return new AskDataCapabilityDescriptor(getKey(), AskDataCapabilityType.SQL_PLUGIN, "0.1.1-SNAPSHOT",
				"0.1.1-SNAPSHOT", "0.1.1-SNAPSHOT", 0, true);
	}
}
