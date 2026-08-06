package com.xiongx9527.fw.plus.ai.tinyflow.local.retrieval;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowMetadataCondition;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TinyflowMetadataPredicateTest {

	private final TinyflowMetadataPredicate predicate = new TinyflowMetadataPredicate();

	@Test
	public void shouldEvaluateTrueFalseNullEmptyAndNullExpectedForEveryDifyOperator() {
		for (Object[] arguments : allDifyOperators()) {
			String operator = (String) arguments[0];
			Object trueActual = arguments[1];
			Object expectedValue = arguments[2];
			Object falseActual = arguments[3];
			boolean nullActual = ((Boolean) arguments[4]).booleanValue();
			boolean emptyActual = ((Boolean) arguments[5]).booleanValue();
			boolean nullExpected = ((Boolean) arguments[6]).booleanValue();

			assertEquals(operator + " true", true,
					predicate.test(valueMetadata(trueActual), condition("value", operator, expectedValue)));
			assertEquals(operator + " false", false,
					predicate.test(valueMetadata(falseActual), condition("value", operator, expectedValue)));
			assertEquals(operator + " null actual", nullActual,
					predicate.test(valueMetadata(null), condition("value", operator, expectedValue)));
			assertEquals(operator + " empty actual", emptyActual,
					predicate.test(valueMetadata(""), condition("value", operator, expectedValue)));
			assertEquals(operator + " null expected", nullExpected,
					predicate.test(valueMetadata(trueActual), condition("value", operator, null)));
		}
	}

	static List<Object[]> allDifyOperators() {
		return Arrays.asList(
				new Object[] { "contains", "TinyFlow Guide", "Flow", "Tiny Guide", false, false, false },
				new Object[] { "not contains", "TinyFlow Guide", "Java", "Java Guide", false, false, false },
				new Object[] { "start with", "TinyFlow Guide", "Tiny", "Flow Tiny", false, false, false },
				new Object[] { "end with", "TinyFlow Guide", "Guide", "Guide Book", false, false, false },
				new Object[] { "is", "manual", "manual", "faq", false, false, false },
				new Object[] { "is not", "manual", "faq", "faq", false, false, false },
				new Object[] { "in", "manual", Arrays.asList("faq", "manual"), "article", false, false, false },
				new Object[] { "not in", "manual", Arrays.asList("faq", "article"), "faq", false, false, false },
				new Object[] { "empty", "", "ignored", "value", true, true, true },
				new Object[] { "not empty", "value", "ignored", "", false, false, true },
				new Object[] { "=", Integer.valueOf(12), "12", Integer.valueOf(13), false, false, false },
				new Object[] { "≠", Integer.valueOf(12), Integer.valueOf(8), Integer.valueOf(8), false, false, false },
				new Object[] { ">", Integer.valueOf(12), "11.5", "11.5", false, false, false },
				new Object[] { "<", Integer.valueOf(12), Integer.valueOf(13), Integer.valueOf(13), false, false, false },
				new Object[] { "≥", "2026-08-05T10:15:30Z", "2026-08-05", "2026-08-04", false, false, false },
				new Object[] { "≤", "2026-08-05T10:15:30Z", "2026-08-05T23:59:59Z", "2026-08-06", false, false, false },
				new Object[] { "before", "2026-08-05T10:15:30Z", "2026-08-06T00:00:00Z", "2026-08-06", false, false, false },
				new Object[] { "after", "2026-08-05T10:15:30Z", "2026-08-04", "2026-08-04", false, false, false });
	}

	@Test
	public void shouldTreatCollectionMapAndArrayShapesAsEmpty() {
		for (Object empty : Arrays.asList(Collections.emptyList(), Collections.emptyMap(), new Object[0])) {
			assertTrue(predicate.test(valueMetadata(empty), condition("value", "empty", null)));
			assertFalse(predicate.test(valueMetadata(empty), condition("value", "not empty", null)));
			assertFalse(predicate.test(valueMetadata(empty), condition("value", "is not", "value")));
			assertFalse(predicate.test(valueMetadata(empty), condition("value", "not contains", "value")));
			assertFalse(predicate.test(valueMetadata(empty),
					condition("value", "not in", Collections.singletonList("value"))));
		}
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

	private static Map<String, Object> valueMetadata(Object value) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("value", value);
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
