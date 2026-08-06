package com.xiongx9527.fw.plus.ai.tinyflow.local.retrieval;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowMetadataCondition;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TinyflowMetadataPredicate {

	public boolean test(Map<String, Object> metadata, List<TinyflowMetadataCondition> conditions, String logic) {
		String normalizedLogic = normalize(logic);
		if (!"and".equals(normalizedLogic) && !"or".equals(normalizedLogic)) {
			throw new IllegalArgumentException("Unsupported metadata logic: " + logic);
		}
		if (conditions == null || conditions.isEmpty()) {
			return true;
		}
		for (TinyflowMetadataCondition condition : conditions) {
			boolean matched = test(metadata, condition);
			if ("and".equals(normalizedLogic) && !matched) {
				return false;
			}
			if ("or".equals(normalizedLogic) && matched) {
				return true;
			}
		}
		return "and".equals(normalizedLogic);
	}

	public boolean test(Map<String, Object> metadata, TinyflowMetadataCondition condition) {
		if (condition == null) {
			throw new IllegalArgumentException("condition must not be null");
		}
		String operator = normalize(condition.getOperator());
		validateOperator(operator);
		Object actual = metadata == null ? null : metadata.get(condition.getField());
		Object expected = condition.getValue();

		if ("empty".equals(operator)) {
			return isEmpty(actual);
		}
		if ("not empty".equals(operator)) {
			return !isEmpty(actual);
		}
		if (isEmpty(actual) || expected == null) {
			return false;
		}
		if ("is".equals(operator) || "=".equals(operator)) {
			return equalValues(actual, expected);
		}
		if ("is not".equals(operator) || "≠".equals(operator)) {
			return !equalValues(actual, expected);
		}
		if ("contains".equals(operator) || "not contains".equals(operator)) {
			boolean contains = contains(actual, expected);
			return "contains".equals(operator) ? contains : !contains;
		}
		if ("start with".equals(operator)) {
			return String.valueOf(actual).startsWith(String.valueOf(expected));
		}
		if ("end with".equals(operator)) {
			return String.valueOf(actual).endsWith(String.valueOf(expected));
		}
		if ("in".equals(operator) || "not in".equals(operator)) {
			Boolean member = memberOf(actual, expected);
			return member != null && ("in".equals(operator) ? member.booleanValue() : !member.booleanValue());
		}
		if ("before".equals(operator) || "after".equals(operator)) {
			Integer comparison = compareDates(actual, expected);
			return comparison != null && ("before".equals(operator)
					? comparison.intValue() < 0 : comparison.intValue() > 0);
		}
		Integer comparison = compareNumbersOrDates(actual, expected);
		if (comparison == null) {
			return false;
		}
		if (">".equals(operator)) {
			return comparison.intValue() > 0;
		}
		if ("<".equals(operator)) {
			return comparison.intValue() < 0;
		}
		if ("≥".equals(operator)) {
			return comparison.intValue() >= 0;
		}
		return comparison.intValue() <= 0;
	}

	private void validateOperator(String operator) {
		if ("contains".equals(operator) || "not contains".equals(operator)
				|| "start with".equals(operator) || "end with".equals(operator)
				|| "is".equals(operator) || "is not".equals(operator)
				|| "in".equals(operator) || "not in".equals(operator)
				|| "empty".equals(operator) || "not empty".equals(operator)
				|| "=".equals(operator) || "≠".equals(operator)
				|| ">".equals(operator) || "<".equals(operator)
				|| "≥".equals(operator) || "≤".equals(operator)
				|| "before".equals(operator) || "after".equals(operator)) {
			return;
		}
		throw new IllegalArgumentException("Unsupported metadata operator: " + operator);
	}

	private boolean contains(Object actual, Object expected) {
		if (actual instanceof Collection<?>) {
			for (Object value : (Collection<?>) actual) {
				if (equalValues(value, expected)) {
					return true;
				}
			}
			return false;
		}
		return String.valueOf(actual).contains(String.valueOf(expected));
	}

	private Boolean memberOf(Object actual, Object expected) {
		if (expected instanceof Collection<?>) {
			for (Object value : (Collection<?>) expected) {
				if (equalValues(actual, value)) {
					return Boolean.TRUE;
				}
			}
			return Boolean.FALSE;
		}
		if (expected.getClass().isArray()) {
			for (int index = 0; index < Array.getLength(expected); index++) {
				if (equalValues(actual, Array.get(expected, index))) {
					return Boolean.TRUE;
				}
			}
			return Boolean.FALSE;
		}
		return null;
	}

	private boolean equalValues(Object actual, Object expected) {
		if (actual == null || expected == null) {
			return actual == expected;
		}
		BigDecimal leftNumber = number(actual);
		BigDecimal rightNumber = number(expected);
		if (leftNumber != null && rightNumber != null) {
			return leftNumber.compareTo(rightNumber) == 0;
		}
		return actual.equals(expected) || String.valueOf(actual).equals(String.valueOf(expected));
	}

	private Integer compareNumbersOrDates(Object actual, Object expected) {
		BigDecimal leftNumber = number(actual);
		BigDecimal rightNumber = number(expected);
		if (leftNumber != null && rightNumber != null) {
			return Integer.valueOf(leftNumber.compareTo(rightNumber));
		}
		return compareDates(actual, expected);
	}

	private Integer compareDates(Object actual, Object expected) {
		Instant left = instant(actual);
		Instant right = instant(expected);
		return left == null || right == null ? null : Integer.valueOf(left.compareTo(right));
	}

	private BigDecimal number(Object value) {
		if (!(value instanceof Number) && !(value instanceof CharSequence)) {
			return null;
		}
		try {
			return new BigDecimal(String.valueOf(value).trim());
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private Instant instant(Object value) {
		if (value instanceof Instant) {
			return (Instant) value;
		}
		if (!(value instanceof CharSequence)) {
			return null;
		}
		String text = String.valueOf(value).trim();
		try {
			return Instant.parse(text);
		} catch (RuntimeException ignored) {
			// Try the remaining ISO-8601 forms below.
		}
		try {
			return OffsetDateTime.parse(text).toInstant();
		} catch (RuntimeException ignored) {
			// Try the remaining ISO-8601 forms below.
		}
		try {
			return ZonedDateTime.parse(text).toInstant();
		} catch (RuntimeException ignored) {
			// Try the remaining ISO-8601 forms below.
		}
		try {
			return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
		} catch (RuntimeException ignored) {
			// Try an ISO local date last.
		}
		try {
			return LocalDate.parse(text).atStartOfDay().toInstant(ZoneOffset.UTC);
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	private boolean isEmpty(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof CharSequence) {
			return ((CharSequence) value).length() == 0;
		}
		if (value instanceof Collection<?>) {
			return ((Collection<?>) value).isEmpty();
		}
		if (value instanceof Map<?, ?>) {
			return ((Map<?, ?>) value).isEmpty();
		}
		return value.getClass().isArray() && Array.getLength(value) == 0;
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
