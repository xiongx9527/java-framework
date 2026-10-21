package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class DtpDataSourceValidator {

	private static final Set<String> QUEUE_TYPES = new HashSet<>(Arrays.asList(
			"array", "synchronous", "linked", "priority", "delayed", "linkedtransferqueue", "linkedblockingdeque"));

	private static final Set<String> UNBOUNDED_OR_CAPACITY_IGNORED_QUEUE_TYPES = new HashSet<>(Arrays.asList(
			"synchronous", "linkedtransferqueue", "delayed"));

	private static final Set<String> REJECTED_POLICIES = new HashSet<>(Arrays.asList(
			"caller_runs", "abort", "discard", "discard_oldest"));

	private DtpDataSourceValidator() {
	}

	public static void validate(DtpDataSourceInfo info) {
		if (info == null) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.dtp.configRequired"));
		}
		requireText(info.getKey(), I18nMessageResolver.resolveKey("fw.boot.magicapi.dtp.keyRequired"));
		if (info.getCorePoolSize() <= 0) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.dtp.corePoolSizeInvalid"));
		}
		if (info.getMaximumPoolSize() < info.getCorePoolSize()) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.dtp.maximumPoolSizeInvalid"));
		}
		if (info.getKeepAliveTime() < 0) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.dtp.keepAliveTimeInvalid"));
		}
		String queueType = normalizeQueueType(info.getQueueType());
		if (!isSupportedQueueType(queueType)) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.dtp.queueTypeUnsupported", info.getQueueType()));
		}
		if (isBoundedQueue(queueType) && info.getQueueCapacity() <= 0) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.dtp.queueCapacityInvalid"));
		}
		String rejectedPolicy = normalizeRejectedPolicy(info.getRejectedPolicy());
		if (!isSupportedRejectedPolicy(rejectedPolicy)) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.dtp.rejectedPolicyUnsupported", info.getRejectedPolicy()));
		}
	}

	public static boolean isSupportedQueueType(String queueType) {
		return QUEUE_TYPES.contains(normalizeQueueType(queueType));
	}

	public static boolean isSupportedRejectedPolicy(String rejectedPolicy) {
		return REJECTED_POLICIES.contains(normalizeRejectedPolicy(rejectedPolicy));
	}

	public static boolean isBoundedQueue(String queueType) {
		return !UNBOUNDED_OR_CAPACITY_IGNORED_QUEUE_TYPES.contains(normalizeQueueType(queueType));
	}

	public static String normalizeQueueType(String queueType) {
		return StringUtils.defaultIfBlank(queueType, "linked").toLowerCase(Locale.ENGLISH);
	}

	public static String normalizeRejectedPolicy(String rejectedPolicy) {
		return StringUtils.defaultIfBlank(rejectedPolicy, "abort").toLowerCase(Locale.ENGLISH);
	}

	private static void requireText(String value, String message) {
		if (StringUtils.isBlank(value)) {
			throw new IllegalArgumentException(message);
		}
	}
}
