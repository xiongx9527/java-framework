package com.xiongx9527.fw.plus.ai.tinyflow.local.retrieval;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowMetadataCondition;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TinyflowMetadataPredicateTest {

	private final TinyflowMetadataPredicate predicate = new TinyflowMetadataPredicate();

	@Test
	public void shouldEvaluateEveryDifyMetadataOperator() {
		for (Object[] arguments : allDifyOperators()) {
			String operator = (String) arguments[0];
			String field = (String) arguments[1];
			Object expectedValue = arguments[2];
			boolean expected = ((Boolean) arguments[3]).booleanValue();
			assertResult(expected, predicate.test(metadata(), condition(field, operator, expectedValue)));
		}
	}

	static List<Object[]> allDifyOperators() {
		return Arrays.asList(
				new Object[] { "contains", "title", "Flow", Boolean.TRUE },
				new Object[] { "not contains", "title", "Java", Boolean.TRUE },
				new Object[] { "start with", "title", "Tiny", Boolean.TRUE },
				new Object[] { "end with", "title", "Guide", Boolean.TRUE },
				new Object[] { "is", "category", "manual", Boolean.TRUE },
				new Object[] { "is not", "category", "faq", Boolean.TRUE },
				new Object[] { "in", "category", Arrays.asList("faq", "manual"), Boolean.TRUE },
				new Object[] { "not in", "category", Arrays.asList("faq", "article"), Boolean.TRUE },
				new Object[] { "empty", "empty", null, Boolean.TRUE },
				new Object[] { "not empty", "title", null, Boolean.TRUE },
				new Object[] { "=", "page", "12", Boolean.TRUE },
				new Object[] { "≠", "page", Integer.valueOf(8), Boolean.TRUE },
				new Object[] { ">", "page", "11.5", Boolean.TRUE },
				new Object[] { "<", "page", Integer.valueOf(13), Boolean.TRUE },
				new Object[] { "≥", "published", "2026-08-05", Boolean.TRUE },
				new Object[] { "≤", "published", "2026-08-05T23:59:59Z", Boolean.TRUE },
				new Object[] { "before", "published", "2026-08-06T00:00:00Z", Boolean.TRUE },
				new Object[] { "after", "published", "2026-08-04", Boolean.TRUE });
	}

	@Test
	public void shouldApplyAndOrLogicWithDefinedEmptyConditionSemantics() {
		List<TinyflowMetadataCondition> conditions = Arrays.asList(
				condition("category", "is", "manual"),
				condition("page", ">", Integer.valueOf(20)));

		assertFalse(predicate.test(metadata(), conditions, "and"));
		assertTrue(predicate.test(metadata(), conditions, "or"));
		assertTrue(predicate.test(metadata(), Collections.<TinyflowMetadataCondition>emptyList(), "and"));
		assertTrue(predicate.test(metadata(), Collections.<TinyflowMetadataCondition>emptyList(), "or"));
	}

	@Test
	public void shouldReturnFalseForNullOrInvalidTypedComparisons() {
		assertFalse(predicate.test(metadata(), condition("missing", ">", Integer.valueOf(1))));
		assertFalse(predicate.test(metadata(), condition("missing", "is not", "value")));
		assertFalse(predicate.test(metadata(), condition("missing", "≠", "value")));
		assertFalse(predicate.test(metadata(), condition("empty", "not in", Arrays.asList("a", "b"))));
		assertFalse(predicate.test(metadata(), condition("page", ">", "not-a-number")));
		assertFalse(predicate.test(metadata(), condition("published", "before", "not-a-date")));
		assertFalse(predicate.test(metadata(), condition("title", "contains", null)));
	}

	@Test
	public void shouldRejectUnknownOperatorAndLogic() {
		try {
			predicate.test(metadata(), condition("page", "between", Integer.valueOf(1)));
			fail("Expected unknown operator to be rejected");
		} catch (IllegalArgumentException expected) {
			// expected
		}
		try {
			predicate.test(metadata(), Collections.singletonList(
					condition("page", "=", Integer.valueOf(12))), "xor");
			fail("Expected unknown logic to be rejected");
		} catch (IllegalArgumentException expected) {
			// expected
		}
	}

	private static Map<String, Object> metadata() {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("title", "TinyFlow Guide");
		metadata.put("category", "manual");
		metadata.put("page", Integer.valueOf(12));
		metadata.put("published", "2026-08-05T10:15:30Z");
		metadata.put("empty", "");
		return metadata;
	}

	private static TinyflowMetadataCondition condition(String field, String operator, Object value) {
		return TinyflowMetadataCondition.builder().field(field).operator(operator).value(value).build();
	}

	private static void assertResult(boolean expected, boolean actual) {
		if (expected) {
			assertTrue(actual);
		} else {
			assertFalse(actual);
		}
	}
}
