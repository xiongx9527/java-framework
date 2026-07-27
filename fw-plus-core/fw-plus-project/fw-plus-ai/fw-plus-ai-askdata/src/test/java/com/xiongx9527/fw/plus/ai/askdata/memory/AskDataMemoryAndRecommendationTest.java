package com.xiongx9527.fw.plus.ai.askdata.memory;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AskDataMemoryAndRecommendationTest {

	@Test
	void 应为单个问题生成单步骤计划() throws Exception {
		Object planner = invokeStatic("com.xiongx9527.fw.plus.ai.askdata.agent.AskDataQuestionPlanner", "simple");
		Object plan = invoke(planner, "plan", new Class[] {String.class}, "查询本月销售额");

		assertEquals(Collections.singletonList("查询本月销售额"), questions(plan));
	}

	@Test
	void 应保留显式多问题列表的顺序() throws Exception {
		Object planner = invokeStatic("com.xiongx9527.fw.plus.ai.askdata.agent.AskDataQuestionPlanner", "simple");
		Object plan = invoke(planner, "plan", new Class[] {List.class}, Arrays.asList("查询销售额", "查询订单量"));

		assertEquals(Arrays.asList("查询销售额", "查询订单量"), questions(plan));
	}

	@Test
	void 应将复合问题拆分为多步骤计划() throws Exception {
		Object planner = invokeStatic("com.xiongx9527.fw.plus.ai.askdata.agent.AskDataQuestionPlanner", "simple");
		Object plan = invoke(planner, "plan", new Class[] {String.class}, "查询销售额；查询订单量");

		assertEquals(Arrays.asList("查询销售额", "查询订单量"), questions(plan));
	}

	@Test
	void 长期记忆必须按项目操作人数据源和语义版本过滤() throws Exception {
		Object memory = invokeStatic("com.xiongx9527.fw.plus.ai.askdata.memory.AskDataLongTermMemory", "inMemory");
		AskDataProjectContext matched = new AskDataProjectContext("project-a", "operator-a");
		remember(memory, entry(matched, "ds-a", "v1", "matched"));
		remember(memory, entry(new AskDataProjectContext("project-a", "operator-b"), "ds-a", "v1", "other-operator"));
		remember(memory, entry(matched, "ds-b", "v1", "other-datasource"));
		remember(memory, entry(matched, "ds-a", "v2", "other-version"));
		remember(memory, entry(new AskDataProjectContext("project-b", "operator-a"), "ds-a", "v1", "other-project"));

		List<?> entries = (List<?>) invoke(memory, "find",
				new Class[] {AskDataProjectContext.class, String.class, String.class}, matched, "ds-a", "v1");

		assertEquals(1, entries.size());
		assertEquals("matched", invoke(entries.get(0), "getContent", new Class[0]));
	}

	@Test
	void 推荐必须按项目和操作人隔离且仅在策略允许时返回通用建议() throws Exception {
		Object recommendations = invokeStatic("com.xiongx9527.fw.plus.ai.askdata.memory.AskDataRecommendationService", "inMemory");
		AskDataProjectContext operatorA = new AskDataProjectContext("project-a", "operator-a");
		AskDataProjectContext operatorB = new AskDataProjectContext("project-a", "operator-b");
		invoke(recommendations, "record", new Class[] {AskDataProjectContext.class, String.class}, operatorA, "operator-a-preference");
		invoke(recommendations, "record", new Class[] {AskDataProjectContext.class, String.class}, operatorB, "operator-b-preference");
		invoke(recommendations, "registerGeneric", new Class[] {String.class, String.class}, "project-a", "generic");

		assertEquals(Collections.singletonList("operator-a-preference"),
				invoke(recommendations, "recommend", new Class[] {AskDataProjectContext.class}, operatorA));
		assertEquals(Collections.singletonList("operator-b-preference"),
				invoke(recommendations, "recommend", new Class[] {AskDataProjectContext.class}, operatorB));
		assertTrue(((List<?>) invoke(recommendations, "recommend",
				new Class[] {String.class, String.class, boolean.class}, "project-a", null, false)).isEmpty());
		assertEquals(Collections.singletonList("generic"), invoke(recommendations, "recommend",
				new Class[] {String.class, String.class, boolean.class}, "project-a", null, true));
	}

	@Test
	void 空白操作人仅在策略允许时回退通用建议() throws Exception {
		Object recommendations = invokeStatic("com.xiongx9527.fw.plus.ai.askdata.memory.AskDataRecommendationService", "inMemory");
		invoke(recommendations, "registerGeneric", new Class[] {String.class, String.class}, "project-a", "generic");

		assertTrue(((List<?>) invoke(recommendations, "recommend",
				new Class[] {String.class, String.class, boolean.class}, "project-a", " ", false)).isEmpty());
		assertEquals(Collections.singletonList("generic"), invoke(recommendations, "recommend",
				new Class[] {String.class, String.class, boolean.class}, "project-a", " ", true));
		assertTrue(((List<?>) invoke(recommendations, "recommend",
				new Class[] {String.class, String.class, boolean.class}, "project-a", "\u3000", false)).isEmpty());
		assertEquals(Collections.singletonList("generic"), invoke(recommendations, "recommend",
				new Class[] {String.class, String.class, boolean.class}, "project-a", "\u3000", true));
	}

	private static Object entry(AskDataProjectContext context, String datasourceKey, String semanticVersion, String content)
			throws Exception {
		Class<?> entryType = Class.forName("com.xiongx9527.fw.plus.ai.askdata.memory.AskDataMemoryEntry");
		Constructor<?> constructor = entryType.getConstructor(AskDataProjectContext.class, String.class, String.class, String.class);
		return constructor.newInstance(context, datasourceKey, semanticVersion, content);
	}

	private static void remember(Object memory, Object entry) throws Exception {
		Class<?> entryType = Class.forName("com.xiongx9527.fw.plus.ai.askdata.memory.AskDataMemoryEntry");
		invoke(memory, "remember", new Class[] {entryType}, entry);
	}

	@SuppressWarnings("unchecked")
	private static List<String> questions(Object plan) throws Exception {
		return (List<String>) invoke(plan, "getQuestions", new Class[0]);
	}

	private static Object invokeStatic(String typeName, String methodName) throws Exception {
		Class<?> type = Class.forName(typeName);
		return type.getMethod(methodName).invoke(null);
	}

	private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... arguments) throws Exception {
		Method method = target.getClass().getMethod(methodName, parameterTypes);
		return method.invoke(target, arguments);
	}
}
