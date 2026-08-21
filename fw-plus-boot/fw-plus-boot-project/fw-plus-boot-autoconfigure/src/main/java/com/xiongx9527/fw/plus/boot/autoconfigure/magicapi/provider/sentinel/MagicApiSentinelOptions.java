package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.ParamConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.model.ApiInfo;

@Slf4j
public class MagicApiSentinelOptions {

	private final boolean hotDataEnabled;
	private final int hotDataGrade;
	private final double hotDataCount;
	private final long hotDataDuration;
	private final boolean fallbackEnabled;
	private final double fallbackCount;

	public MagicApiSentinelOptions(boolean hotDataEnabled, int hotDataGrade, double hotDataCount, long hotDataDuration,
								   boolean fallbackEnabled, double fallbackCount) {
		this.hotDataEnabled = hotDataEnabled;
		this.hotDataGrade = hotDataGrade;
		this.hotDataCount = hotDataCount;
		this.hotDataDuration = hotDataDuration;
		this.fallbackEnabled = fallbackEnabled;
		this.fallbackCount = fallbackCount;
	}

	public static MagicApiSentinelOptions from(ApiInfo info) {
		return new MagicApiSentinelOptions(
				ParamConfig.isEnabled(info, Options.HOT_DATA_ENABLE.getValue()),
				parseGrade(ParamConfig.get(info, Options.HOT_DATA_GRADE.getValue())),
				parseDouble(info, Options.HOT_DATA_COUNT),
				parseLong(info, Options.HOT_DATA_DURATION),
				ParamConfig.isEnabled(info, Options.FALLBACK_ENABLE.getValue()),
				parseDouble(info, Options.FALLBACK_COUNT));
	}

	public boolean isHotDataEffective() {
		return hotDataEnabled && hotDataCount > 0;
	}

	public boolean isFallbackEffective() {
		return fallbackEnabled && fallbackCount > 0;
	}

	public boolean isHotDataEnabled() {
		return hotDataEnabled;
	}

	public int getHotDataGrade() {
		return hotDataGrade;
	}

	public double getHotDataCount() {
		return hotDataCount;
	}

	public long getHotDataDuration() {
		return hotDataDuration;
	}

	public boolean isFallbackEnabled() {
		return fallbackEnabled;
	}

	public double getFallbackCount() {
		return fallbackCount;
	}

	private static int parseGrade(String value) {
		if ("THREAD".equalsIgnoreCase(StringUtils.trimToEmpty(value))) {
			return RuleConstant.FLOW_GRADE_THREAD;
		}
		return RuleConstant.FLOW_GRADE_QPS;
	}

	private static double parseDouble(ApiInfo info, Options option) {
		String value = ParamConfig.get(info, option.getValue());
		String fallback = StringUtils.defaultIfBlank(option.getDefaultValue(), "0");
		try {
			return Double.parseDouble(StringUtils.defaultIfBlank(value, fallback));
		} catch (Exception e) {
			log.warn("invalid magic-api sentinel option {}={}, use default={}", option.getValue(), value, fallback);
			return Double.parseDouble(fallback);
		}
	}

	private static long parseLong(ApiInfo info, Options option) {
		String value = ParamConfig.get(info, option.getValue());
		String fallback = StringUtils.defaultIfBlank(option.getDefaultValue(), "0");
		try {
			return Long.parseLong(StringUtils.defaultIfBlank(value, fallback));
		} catch (Exception e) {
			log.warn("invalid magic-api sentinel option {}={}, use default={}", option.getValue(), value, fallback);
			return Long.parseLong(fallback);
		}
	}
}
