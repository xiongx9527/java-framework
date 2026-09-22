package com.xiongx9527.fw.plus.ai.mcp;

import com.xiongx9527.fw.plus.ai.mcp.annotation.McpToolParam;
import com.xiongx9527.fw.plus.ai.mcp.server.McpSchemaGenerator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * inputSchema 生成与参数绑定单元测试
 *
 * <p>spec 7.2：基本类型、String、枚举、JavaBean、List/Map 均有确定映射规则；
 * 不支持的参数类型启动期快速失败。
 */
public class McpSchemaGeneratorTest {

	public enum Color { RED, GREEN, BLUE }

	public static class Bean {
		public String name;
		public int age;
		public List<String> tags;
	}

	public void noArgs() {
	}

	public void primitiveArgs(
			@McpToolParam(name = "count", description = "number of items", required = true) int count,
			String label,
			Color color,
			Bean bean,
			Map<String, Object> metadata) {
	}

	public void unsupportedArg(java.time.Instant instant) {
	}

	@Test
	public void shouldGenerateEmptySchemaForNoArgs() throws Exception {
		Method method = McpSchemaGeneratorTest.class.getMethod("noArgs");
		Map<String, Object> schema = McpSchemaGenerator.generateForMethod(method);
		assertThat(schema.get("type")).isEqualTo("object");
		assertThat((Map<?, ?>) schema.get("properties")).isEmpty();
	}

	@Test
	public void shouldGenerateSchemaForPrimitiveArgs() throws Exception {
		Method method = McpSchemaGeneratorTest.class.getMethod("primitiveArgs",
				int.class, String.class, Color.class, Bean.class, Map.class);
		Map<String, Object> schema = McpSchemaGenerator.generateForMethod(method);
		assertThat(schema.get("type")).isEqualTo("object");
		Map<?, ?> properties = (Map<?, ?>) schema.get("properties");
		assertThat(properties.containsKey("count")).isTrue();
		Map<?, ?> countProp = (Map<?, ?>) properties.get("count");
		assertThat(countProp.get("type")).isEqualTo("integer");
		assertThat(countProp.get("description")).isEqualTo("number of items");
		assertThat(properties.containsKey("label")).isTrue();
		assertThat(((Map<?, ?>) properties.get("label")).get("type")).isEqualTo("string");
		assertThat(properties.containsKey("color")).isTrue();
		Map<?, ?> colorProp = (Map<?, ?>) properties.get("color");
		assertThat(colorProp.get("type")).isEqualTo("string");
		@SuppressWarnings("unchecked")
		List<String> enumValues = (List<String>) colorProp.get("enum");
		assertThat(enumValues).contains("RED", "GREEN", "BLUE");
		assertThat(properties.containsKey("bean")).isTrue();
		Map<?, ?> beanProp = (Map<?, ?>) properties.get("bean");
		assertThat(beanProp.get("type")).isEqualTo("object");
		Map<?, ?> beanProps = (Map<?, ?>) beanProp.get("properties");
		assertThat(beanProps.containsKey("name")).isTrue();
		assertThat(beanProps.containsKey("age")).isTrue();
		assertThat(beanProps.containsKey("tags")).isTrue();
		assertThat(((Map<?, ?>) beanProps.get("tags")).get("type")).isEqualTo("array");
		assertThat(properties.containsKey("metadata")).isTrue();
		assertThat(((Map<?, ?>) properties.get("metadata")).get("type")).isEqualTo("object");
		@SuppressWarnings("unchecked")
		List<String> required = (List<String>) schema.get("required");
		assertThat(required).contains("count");
	}

	@Test
	public void shouldFailFastOnUnsupportedType() throws Exception {
		Method method = McpSchemaGeneratorTest.class.getMethod("unsupportedArg", java.time.Instant.class);
		assertThatThrownBy(() -> McpSchemaGenerator.generateForMethod(method))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unsupported parameter type");
	}

	@Test
	public void shouldBindArgumentsToBean() throws Exception {
		Method method = McpSchemaGeneratorTest.class.getMethod("primitiveArgs",
				int.class, String.class, Color.class, Bean.class, Map.class);
		Map<String, Object> args = new java.util.LinkedHashMap<>();
		args.put("count", 42);
		args.put("label", "hello");
		args.put("color", "GREEN");
		Map<String, Object> beanMap = new java.util.LinkedHashMap<>();
		beanMap.put("name", "Alice");
		beanMap.put("age", 30);
		beanMap.put("tags", java.util.Arrays.asList("a", "b"));
		args.put("bean", beanMap);
		Map<String, Object> meta = new java.util.LinkedHashMap<>();
		meta.put("key", "value");
		args.put("metadata", meta);
		Object[] bound = McpSchemaGenerator.bindArguments(method, args);
		assertThat(bound).hasSize(5);
		assertThat(bound[0]).isEqualTo(42);
		assertThat(bound[1]).isEqualTo("hello");
		assertThat(bound[2]).isEqualTo(Color.GREEN);
		assertThat(bound[3]).isInstanceOf(Bean.class);
		Bean b = (Bean) bound[3];
		assertThat(b.name).isEqualTo("Alice");
		assertThat(b.age).isEqualTo(30);
		assertThat(b.tags).contains("a", "b");
		assertThat(bound[4]).isInstanceOf(Map.class);
	}
}
