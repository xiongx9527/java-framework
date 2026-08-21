package com.xiongx9527.fw.plus.tinyengine.common;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonArrayTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonObjectTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.common.util.TinyEngineFileSecurityUtils;
import com.xiongx9527.fw.plus.tinyengine.common.util.TinyEngineHashUtils;
import com.xiongx9527.fw.plus.tinyengine.common.util.TinyEngineJsonUtils;
import com.xiongx9527.fw.plus.tinyengine.common.util.TinyEngineSchemaUtils;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppAiModelSettings;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TinyEngineUtilityTest {

	@Test
	void jsonUtilsShouldRoundTripObjectAndArray() {
		JSONObject object = new JSONObject();
		object.put("name", "home");
		object.put("enabled", true);
		JSONArray array = new JSONArray();
		array.add("first");
		array.add(object);

		assertEquals(object, TinyEngineJsonUtils.parseObject(TinyEngineJsonUtils.toJson(object)));
		assertEquals(array, TinyEngineJsonUtils.parseArray(TinyEngineJsonUtils.toJson(array)));
	}

	@Test
	void jsonUtilsShouldPreserveUnknownAiModelSettingsAsTopLevelProperties() {
		AppAiModelSettings settings = new AppAiModelSettings();
		settings.setDefaultModel("qwen-max");
		settings.setAdditionalProperty("temperature", 0.7D);
		Map<String, Object> extendConfig = new HashMap<String, Object>();
		extendConfig.put("aiModelSettings", settings);

		JSONObject persisted = TinyEngineJsonUtils.parseObject(TinyEngineJsonUtils.toJson(extendConfig));
		JSONObject persistedSettings = persisted.getJSONObject("aiModelSettings");

		assertEquals("qwen-max", persistedSettings.getString("defaultModel"));
		assertEquals(0.7D, persistedSettings.getDoubleValue("temperature"), 0.0D);
		assertFalse(persistedSettings.containsKey("additionalProperties"));
		AppAiModelSettings restored = AppAiModelSettings.copyOf(persistedSettings);
		assertEquals(0.7D, ((Number) restored.getAdditionalProperties().get("temperature")).doubleValue(), 0.0D);
	}

	@Test
	void schemaUtilsShouldBuildFolderAndPageContentSchemasWithoutMutatingInputs() {
		Map<String, Object> folder = new HashMap<String, Object>();
		folder.put("name", "components");
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("componentName", "Page");
		Map<String, Object> page = new HashMap<String, Object>();
		page.put("name", "index");
		page.put("page_content", content);

		Map<String, Object> folderSchema = TinyEngineSchemaUtils.getFolderSchema(folder);
		Map<String, Object> pageSchema = TinyEngineSchemaUtils.getSchemaBase(page);

		assertEquals("Folder", folderSchema.get("componentName"));
		assertEquals("components", folderSchema.get("name"));
		assertEquals("index", pageSchema.get("fileName"));
		assertFalse(content.containsKey("fileName"));
	}

	@Test
	void hashUtilsShouldBeDeterministicAcrossMapInsertionOrder() {
		Map<String, Object> first = new HashMap<String, Object>();
		first.put("name", "home");
		first.put("version", 1);
		Map<String, Object> second = new HashMap<String, Object>();
		second.put("version", 1);
		second.put("name", "home");

		assertEquals(TinyEngineHashUtils.calculateMD5Hash(first), TinyEngineHashUtils.calculateMD5Hash(second));
		assertNotEquals(TinyEngineHashUtils.calculateMD5Hash(first), TinyEngineHashUtils.calculateMD5Hash(null));
	}

	@Test
	void hashUtilsShouldBeDeterministicAcrossNestedMapInsertionOrder() {
		Map<String, Object> firstNested = new HashMap<String, Object>();
		firstNested.put("Aa", "first");
		firstNested.put("BB", "second");
		Map<String, Object> first = new HashMap<String, Object>();
		first.put("metadata", firstNested);

		Map<String, Object> secondNested = new HashMap<String, Object>();
		secondNested.put("BB", "second");
		secondNested.put("Aa", "first");
		Map<String, Object> second = new HashMap<String, Object>();
		second.put("metadata", secondNested);

		assertEquals(TinyEngineHashUtils.calculateMD5Hash(first), TinyEngineHashUtils.calculateMD5Hash(second));
	}

	@Test
	void fileSecurityUtilsShouldRejectPathTraversal() {
		assertTrue(TinyEngineFileSecurityUtils.checkPathHasCrossDir("../secret.json"));
		assertTrue(TinyEngineFileSecurityUtils.checkPathHasCrossDir("pages/../../secret.json"));
		assertTrue(TinyEngineFileSecurityUtils.checkPathHasCrossDir("%2e%2e%2fsecret.json"));
		assertTrue(TinyEngineFileSecurityUtils.checkPathHasCrossDir("%252e%252e%252fsecret.json"));
		assertTrue(TinyEngineFileSecurityUtils.checkPathHasCrossDir("pages/%2E%2E/secret.json"));
		assertTrue(TinyEngineFileSecurityUtils.checkPathHasCrossDir("pages%5c..%5csecret.json"));
		assertTrue(TinyEngineFileSecurityUtils.checkPathHasCrossDir("/tmp/secret.json"));
		assertTrue(TinyEngineFileSecurityUtils.checkPathHasCrossDir("C:\\temp\\secret.json"));
		assertTrue(TinyEngineFileSecurityUtils.checkPathHasCrossDir("\\\\server\\share\\secret.json"));
		assertTrue(TinyEngineFileSecurityUtils.checkPathHasCrossDir("safe/evil%00.png"));
		assertFalse(TinyEngineFileSecurityUtils.checkPathHasCrossDir("pages/home.json"));
		assertThrows(IllegalArgumentException.class, () -> TinyEngineFileSecurityUtils.validateRelativePath("../secret.json"));
	}

	@Test
	void objectTypeHandlerShouldRoundTripAndTreatNullOrBlankJsonAsEmptyObject() throws Exception {
		JsonObjectTypeHandler handler = new JsonObjectTypeHandler();
		JSONObject expected = new JSONObject();
		expected.put("name", "home");
		StringCapture capture = new StringCapture();

		handler.setNonNullParameter(capture.preparedStatement(), 1, expected, JdbcType.VARCHAR);

		assertEquals(expected, handler.getNullableResult(resultSet(capture.value), "payload"));
		assertEquals(new JSONObject(), handler.getNullableResult(resultSet(null), "payload"));
		assertEquals(new JSONObject(), handler.getNullableResult(resultSet("  "), "payload"));
	}

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	void objectTypeHandlerShouldAcceptOrdinaryHashMap() throws Exception {
		JsonObjectTypeHandler handler = new JsonObjectTypeHandler();
		Map<String, Object> expected = new HashMap<String, Object>();
		expected.put("name", "home");
		StringCapture capture = new StringCapture();

		((org.apache.ibatis.type.BaseTypeHandler) handler).setNonNullParameter(capture.preparedStatement(), 1, expected,
				JdbcType.VARCHAR);

		assertEquals(expected, TinyEngineJsonUtils.parseObject(capture.value));
	}

	@Test
	void arrayTypeHandlerShouldRoundTripAndTreatNullOrBlankJsonAsEmptyArray() throws Exception {
		JsonArrayTypeHandler handler = new JsonArrayTypeHandler();
		JSONArray expected = new JSONArray();
		expected.add("home");
		StringCapture capture = new StringCapture();

		handler.setNonNullParameter(capture.preparedStatement(), 1, expected, JdbcType.VARCHAR);

		assertEquals(expected, handler.getNullableResult(resultSet(capture.value), "payload"));
		assertEquals(new JSONArray(), handler.getNullableResult(resultSet(null), "payload"));
		assertEquals(new JSONArray(), handler.getNullableResult(resultSet("  "), "payload"));
	}

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	void arrayTypeHandlerShouldAcceptOrdinaryArrayList() throws Exception {
		JsonArrayTypeHandler handler = new JsonArrayTypeHandler();
		List<String> expected = new ArrayList<String>();
		expected.add("home");
		StringCapture capture = new StringCapture();

		((org.apache.ibatis.type.BaseTypeHandler) handler).setNonNullParameter(capture.preparedStatement(), 1, expected,
				JdbcType.VARCHAR);

		assertEquals(expected, TinyEngineJsonUtils.parseArray(capture.value));
	}

	private ResultSet resultSet(final String value) {
		return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {ResultSet.class},
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) {
						if ("getString".equals(method.getName())) {
							return value;
						}
						return defaultValue(method.getReturnType());
					}
				});
	}

	private Object defaultValue(Class<?> type) {
		if (!type.isPrimitive()) {
			return null;
		}
		if (boolean.class.equals(type)) {
			return false;
		}
		if (char.class.equals(type)) {
			return '\0';
		}
		return 0;
	}

	private static class StringCapture {
		private String value;

		private PreparedStatement preparedStatement() {
			return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] {PreparedStatement.class}, new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) {
							if ("setString".equals(method.getName())) {
								value = (String) args[1];
							}
							if (method.getReturnType().isPrimitive()) {
								if (boolean.class.equals(method.getReturnType())) {
									return false;
								}
								return 0;
							}
							return null;
						}
					});
		}
	}
}
