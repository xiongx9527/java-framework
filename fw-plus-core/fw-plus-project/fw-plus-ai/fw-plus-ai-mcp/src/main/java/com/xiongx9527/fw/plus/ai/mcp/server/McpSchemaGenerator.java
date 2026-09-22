package com.xiongx9527.fw.plus.ai.mcp.server;

import com.xiongx9527.fw.plus.ai.mcp.annotation.McpToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具 inputSchema 生成器
 *
 * <p>对齐 spec 7.2：根据 @McpTool 方法参数自动生成 JSON Schema 2020-12。
 * 支持基本类型、String、枚举、JavaBean、List/Map，不支持类型启动期快速失败并给出明确错误。
 *
 * <p>参数识别规则：
 * <ul>
 *   <li>标注了 {@link McpToolParam} 的参数：使用其 name/description/required</li>
 *   <li>未标注的参数：按参数名（javac -parameters 编译可用）或 arg0/arg1... 兜底</li>
 *   <li>第一个参数为 Map 或 JavaBean 时，整体作为 schema 的 properties 来源</li>
 *   <li>零参数方法：返回空 properties 的对象 schema</li>
 * </ul>
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public final class McpSchemaGenerator {

	private static final Logger logger = LoggerFactory.getLogger(McpSchemaGenerator.class);

	private McpSchemaGenerator() {
	}

	/**
	 * 根据方法参数生成 inputSchema。
	 *
	 * <p>规则：
	 * - 零参数：返回空 properties 的 object schema
	 * - 单 Map 参数：Map 的每个 entry 作为 properties（运行时动态，启动期取不到字段，回退为空）
	 * - 单 POJO 参数：把 POJO 字段拆分为 properties
	 * - 多参数：每个参数一个 property
	 */
	public static Map<String, Object> generateForMethod(Method method) {
		if (method == null) {
			throw new IllegalArgumentException("method must not be null");
		}
		Parameter[] params = method.getParameters();
		if (params.length == 0) {
			return emptyObjectSchema();
		}
		if (params.length == 1) {
			Parameter p = params[0];
			Class<?> type = p.getType();
			if (Map.class.isAssignableFrom(type)) {
				// Map 类型在编译期取不到字段信息，回退为空对象 schema
				return emptyObjectSchema();
			}
			if (isBeanType(type)) {
				return beanToSchema(type);
			}
			// 单基本类型/String/枚举/Collection 参数
			Map<String, Object> schema = new LinkedHashMap<>();
			Map<String, Object> properties = new LinkedHashMap<>();
			String propName = resolveParamName(p, 0);
			Map<String, Object> propSchema = generatePropertySchema(type, p);
			properties.put(propName, propSchema);
			schema.put("type", "object");
			schema.put("properties", properties);
			List<String> required = new ArrayList<>();
			boolean isRequired = isRequired(p);
			if (isRequired) {
				required.add(propName);
			}
			if (!required.isEmpty()) {
				schema.put("required", required);
			}
			return schema;
		}
		// 多参数：每个参数一个 property
		return multiParamsToSchema(params);
	}

	private static Map<String, Object> multiParamsToSchema(Parameter[] params) {
		Map<String, Object> schema = new LinkedHashMap<>();
		Map<String, Object> properties = new LinkedHashMap<>();
		List<String> required = new ArrayList<>();
		for (int i = 0; i < params.length; i++) {
			Parameter p = params[i];
			String propName = resolveParamName(p, i);
			properties.put(propName, generatePropertySchema(p.getType(), p));
			if (isRequired(p)) {
				required.add(propName);
			}
		}
		schema.put("type", "object");
		schema.put("properties", properties);
		if (!required.isEmpty()) {
			schema.put("required", required);
		}
		return schema;
	}

	/**
	 * 为单个参数生成 JSON Schema 片段
	 */
	private static Map<String, Object> generatePropertySchema(Class<?> type, Parameter parameter) {
		Map<String, Object> schema = new LinkedHashMap<>();
		String description = resolveParamDescription(parameter);
		if (description != null) {
			schema.put("description", description);
		}
		if (type == String.class) {
			schema.put("type", "string");
		} else if (type == boolean.class || type == Boolean.class) {
			schema.put("type", "boolean");
		} else if (type == byte.class || type == Byte.class
				|| type == short.class || type == Short.class
				|| type == int.class || type == Integer.class
				|| type == long.class || type == Long.class) {
			schema.put("type", "integer");
		} else if (type == float.class || type == Float.class
				|| type == double.class || type == Double.class) {
			schema.put("type", "number");
		} else if (type.isEnum()) {
			schema.put("type", "string");
			schema.put("enum", enumValues(type));
		} else if (Collection.class.isAssignableFrom(type) || type.isArray()) {
			schema.put("type", "array");
			schema.put("items", inlineItemsSchema(parameter));
		} else if (Map.class.isAssignableFrom(type)) {
			// 启动期 Map 泛型擦除，回退为通用 object
			schema.put("type", "object");
		} else if (isBeanType(type)) {
			schema.put("type", "object");
			schema.put("properties", beanProperties(type));
			List<String> beanRequired = beanRequiredFields(type);
			if (!beanRequired.isEmpty()) {
				schema.put("required", beanRequired);
			}
		} else {
			throw new IllegalArgumentException(
					"Unsupported parameter type [" + type.getName() + "] for @McpTool method; "
							+ "only primitive/Boxed/String/enum/JavaBean/List/Map are supported");
		}
		return schema;
	}

	private static Map<String, Object> inlineItemsSchema(Parameter parameter) {
		// 优先解析泛型元素类型
		Type genericType = parameter.getParameterizedType();
		if (genericType instanceof ParameterizedType) {
			Type[] args = ((ParameterizedType) genericType).getActualTypeArguments();
			if (args != null && args.length > 0 && args[0] instanceof Class) {
				Class<?> elementType = (Class<?>) args[0];
				if (elementType == String.class) {
					return inline("string");
				}
				if (elementType == Integer.class || elementType == int.class) {
					return inline("integer");
				}
				if (elementType == Boolean.class || elementType == boolean.class) {
					return inline("boolean");
				}
				if (elementType == Double.class || elementType == double.class
						|| elementType == Float.class || elementType == float.class) {
					return inline("number");
				}
				if (elementType.isEnum()) {
					Map<String, Object> s = inline("string");
					s.put("enum", enumValues(elementType));
					return s;
				}
				if (isBeanType(elementType)) {
					return beanToSchema(elementType);
				}
			}
		}
		// 兜底：字符串
		return inline("string");
	}

	private static Map<String, Object> inline(String type) {
		Map<String, Object> s = new LinkedHashMap<>();
		s.put("type", type);
		return s;
	}

	private static Map<String, Object> beanToSchema(Class<?> beanType) {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		schema.put("properties", beanProperties(beanType));
		List<String> required = beanRequiredFields(beanType);
		if (!required.isEmpty()) {
			schema.put("required", required);
		}
		return schema;
	}

	private static Map<String, Object> beanProperties(Class<?> beanType) {
		Map<String, Object> properties = new LinkedHashMap<>();
		for (Field field : beanType.getDeclaredFields()) {
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (field.isSynthetic()) {
				continue;
			}
			Map<String, Object> fieldSchema = new LinkedHashMap<>();
			Class<?> ft = field.getType();
			if (ft == String.class) {
				fieldSchema.put("type", "string");
			} else if (ft == boolean.class || ft == Boolean.class) {
				fieldSchema.put("type", "boolean");
			} else if (ft == byte.class || ft == Byte.class
					|| ft == short.class || ft == Short.class
					|| ft == int.class || ft == Integer.class
					|| ft == long.class || ft == Long.class) {
				fieldSchema.put("type", "integer");
			} else if (ft == float.class || ft == Float.class
					|| ft == double.class || ft == Double.class) {
				fieldSchema.put("type", "number");
			} else if (ft.isEnum()) {
				fieldSchema.put("type", "string");
				fieldSchema.put("enum", enumValues(ft));
			} else if (Collection.class.isAssignableFrom(ft)) {
				fieldSchema.put("type", "array");
				fieldSchema.put("items", inline("string"));
			} else if (ft.isArray()) {
				fieldSchema.put("type", "array");
				Class<?> ct = ft.getComponentType();
				fieldSchema.put("items", ct != null && ct.isPrimitive() ? inline(primitiveName(ct)) : inline("string"));
			} else if (Map.class.isAssignableFrom(ft)) {
				fieldSchema.put("type", "object");
			} else if (isBeanType(ft)) {
				fieldSchema.putAll(beanToSchema(ft));
			} else {
				// 不支持的字段类型启动期快速失败
				throw new IllegalArgumentException(
						"Unsupported field type [" + ft.getName() + "] on bean [" + beanType.getName()
								+ "] field [" + field.getName() + "]; "
								+ "only primitive/Boxed/String/enum/JavaBean/List/Map are supported");
			}
			properties.put(field.getName(), fieldSchema);
		}
		return properties;
	}

	private static List<String> beanRequiredFields(Class<?> beanType) {
		List<String> required = new ArrayList<>();
		for (Field field : beanType.getDeclaredFields()) {
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (field.isSynthetic()) {
				continue;
			}
			// 基本类型默认必填
			if (field.getType().isPrimitive()) {
				required.add(field.getName());
			}
		}
		return required;
	}

	private static List<String> enumValues(Class<?> enumType) {
		Object[] constants = enumType.getEnumConstants();
		List<String> values = new ArrayList<>();
		if (constants != null) {
			for (Object c : constants) {
				values.add(c.toString());
			}
		}
		return values;
	}

	private static boolean isBeanType(Class<?> type) {
		if (type == null || type.isPrimitive() || type.isArray() || type.isEnum()) {
			return false;
		}
		if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
			return false;
		}
		if (type.getName().startsWith("java.")) {
			return false;
		}
		if (type.getName().startsWith("javax.")) {
			return false;
		}
		// 用户自定义 POJO 即认为是 bean
		return true;
	}

	private static String primitiveName(Class<?> primitive) {
		if (primitive == int.class) return "integer";
		if (primitive == long.class) return "integer";
		if (primitive == short.class) return "integer";
		if (primitive == byte.class) return "integer";
		if (primitive == boolean.class) return "boolean";
		if (primitive == float.class) return "number";
		if (primitive == double.class) return "number";
		return "string";
	}

	private static String resolveParamName(Parameter p, int index) {
		McpToolParam annotation = p.getAnnotation(McpToolParam.class);
		if (annotation != null && !annotation.name().isEmpty()) {
			return annotation.name();
		}
		String name = p.isNamePresent() ? p.getName() : null;
		if (name == null || name.isEmpty()) {
			return "arg" + index;
		}
		return name;
	}

	private static String resolveParamDescription(Parameter p) {
		McpToolParam annotation = p.getAnnotation(McpToolParam.class);
		if (annotation != null && !annotation.description().isEmpty()) {
			return annotation.description();
		}
		return null;
	}

	private static boolean isRequired(Parameter p) {
		McpToolParam annotation = p.getAnnotation(McpToolParam.class);
		if (annotation != null && annotation.required()) {
			return true;
		}
		// 基本类型默认必填
		return p.getType().isPrimitive();
	}

	private static Map<String, Object> emptyObjectSchema() {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		schema.put("properties", new LinkedHashMap<>());
		return schema;
	}

	/**
	 * 将 JSON-RPC arguments Map 绑定为方法实参数组。
	 *
	 * <p>规则：
	 * - 零参数：返回空数组
	 * - 单 Map 参数：直接传入 Map
	 * - 单 POJO 参数：把 Map 反序列化为 POJO（逐字段设值）
	 * - 单基本/String/枚举/Collection 参数：取 Map 中对应 prop 的值并做类型转换
	 * - 多参数：按 prop 名逐个绑定
	 */
	public static Object[] bindArguments(Method method, Map<String, Object> arguments) {
		if (method == null) {
			return new Object[0];
		}
		Parameter[] params = method.getParameters();
		if (params.length == 0) {
			return new Object[0];
		}
		Map<String, Object> args = arguments != null ? arguments : new LinkedHashMap<>();
		if (params.length == 1) {
			Parameter p = params[0];
			Class<?> type = p.getType();
			if (Map.class.isAssignableFrom(type)) {
				return new Object[] { args };
			}
			if (isBeanType(type)) {
				return new Object[] { mapToBean(args, type) };
			}
			String propName = resolveParamName(p, 0);
			Object value = args.get(propName);
			return new Object[] { coerce(value, type) };
		}
		Object[] result = new Object[params.length];
		for (int i = 0; i < params.length; i++) {
			Parameter p = params[i];
			Class<?> type = p.getType();
			String propName = resolveParamName(p, i);
			Object value = args.get(propName);
			if (value == null && !args.containsKey(propName)) {
				result[i] = defaultValue(type);
				continue;
			}
			if (isBeanType(type)) {
				@SuppressWarnings("unchecked")
				Map<String, Object> nested = (Map<String, Object>) value;
				result[i] = mapToBean(nested, type);
			} else {
				result[i] = coerce(value, type);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static Object mapToBean(Map<String, Object> map, Class<?> beanType) {
		if (map == null) {
			return defaultValue(beanType);
		}
		try {
			Object instance = beanType.getDeclaredConstructor().newInstance();
			for (Field field : beanType.getDeclaredFields()) {
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				if (field.isSynthetic()) {
					continue;
				}
				if (!map.containsKey(field.getName())) {
					continue;
				}
				Object value = map.get(field.getName());
				if (value == null) {
					continue;
				}
				field.setAccessible(true);
				Class<?> ft = field.getType();
				if (ft == String.class) {
					field.set(instance, value.toString());
				} else if (ft == int.class || ft == Integer.class) {
					field.set(instance, toInt(value));
				} else if (ft == long.class || ft == Long.class) {
					field.set(instance, toLong(value));
				} else if (ft == boolean.class || ft == Boolean.class) {
					field.set(instance, toBoolean(value));
				} else if (ft == double.class || ft == Double.class) {
					field.set(instance, toDouble(value));
				} else if (ft == float.class || ft == Float.class) {
					field.set(instance, toFloat(value));
				} else if (ft == short.class || ft == Short.class) {
					field.set(instance, (short) toInt(value));
				} else if (ft == byte.class || ft == Byte.class) {
					field.set(instance, (byte) toInt(value));
				} else if (ft.isEnum()) {
					Object[] constants = ft.getEnumConstants();
					if (constants != null) {
						for (Object c : constants) {
							if (c.toString().equals(value.toString())) {
								field.set(instance, c);
								break;
							}
						}
					}
				} else if (List.class.isAssignableFrom(ft) && value instanceof List) {
					field.set(instance, value);
				} else if (Map.class.isAssignableFrom(ft) && value instanceof Map) {
					field.set(instance, value);
				} else if (ft.isArray() && value instanceof Collection) {
					Collection<?> collection = (Collection<?>) value;
					Class<?> component = ft.getComponentType();
					Object array = Array.newInstance(component, collection.size());
					int i = 0;
					for (Object item : collection) {
						Array.set(array, i++, item);
					}
					field.set(instance, array);
				} else if (isBeanType(ft) && value instanceof Map) {
					field.set(instance, mapToBean((Map<String, Object>) value, ft));
				} else {
					field.set(instance, value);
				}
			}
			return instance;
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Failed to bind arguments to bean [" + beanType.getName() + "]: " + e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	private static Object coerce(Object value, Class<?> type) {
		if (value == null) {
			return defaultValue(type);
		}
		if (type.isInstance(value)) {
			return value;
		}
		if (type == String.class) {
			return value.toString();
		}
		if (type == int.class || type == Integer.class) {
			return toInt(value);
		}
		if (type == long.class || type == Long.class) {
			return toLong(value);
		}
		if (type == boolean.class || type == Boolean.class) {
			return toBoolean(value);
		}
		if (type == double.class || type == Double.class) {
			return toDouble(value);
		}
		if (type == float.class || type == Float.class) {
			return toFloat(value);
		}
		if (type == short.class || type == Short.class) {
			return (short) toInt(value);
		}
		if (type == byte.class || type == Byte.class) {
			return (byte) toInt(value);
		}
		if (type.isEnum()) {
			Object[] constants = type.getEnumConstants();
			if (constants != null) {
				for (Object c : constants) {
					if (c.toString().equals(value.toString())) {
						return c;
					}
				}
			}
		}
		if (Collection.class.isAssignableFrom(type)) {
			if (value instanceof Collection) {
				return value;
			}
			if (value.getClass().isArray()) {
				List<Object> list = new ArrayList<>();
				int len = Array.getLength(value);
				for (int i = 0; i < len; i++) {
					list.add(Array.get(value, i));
				}
				return list;
			}
		}
		if (type.isArray()) {
			if (value.getClass().isArray()) {
				int len = Array.getLength(value);
				Class<?> ct = type.getComponentType();
				Object arr = Array.newInstance(ct, len);
				for (int i = 0; i < len; i++) {
					Array.set(arr, i, coerce(Array.get(value, i), ct));
				}
				return arr;
			}
			if (value instanceof Collection) {
				Collection<?> collection = (Collection<?>) value;
				Class<?> ct = type.getComponentType();
				Object arr = Array.newInstance(ct, collection.size());
				int i = 0;
				for (Object item : collection) {
					Array.set(arr, i++, coerce(item, ct));
				}
				return arr;
			}
		}
		return value;
	}

	private static Object defaultValue(Class<?> type) {
		if (type == boolean.class) return false;
		if (type == byte.class) return (byte) 0;
		if (type == short.class) return (short) 0;
		if (type == int.class) return 0;
		if (type == long.class) return 0L;
		if (type == float.class) return 0f;
		if (type == double.class) return 0d;
		if (type == char.class) return '\0';
		return null;
	}

	private static int toInt(Object value) {
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		return Integer.parseInt(value.toString());
	}

	private static long toLong(Object value) {
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		return Long.parseLong(value.toString());
	}

	private static boolean toBoolean(Object value) {
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		return Boolean.parseBoolean(value.toString());
	}

	private static double toDouble(Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		return Double.parseDouble(value.toString());
	}

	private static float toFloat(Object value) {
		if (value instanceof Number) {
			return ((Number) value).floatValue();
		}
		return Float.parseFloat(value.toString());
	}
}
