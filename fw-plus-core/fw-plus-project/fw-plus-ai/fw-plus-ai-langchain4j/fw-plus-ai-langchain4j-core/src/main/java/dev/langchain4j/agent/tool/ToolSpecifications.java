package dev.langchain4j.agent.tool;

import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.invocation.LangChain4jManaged;
import dev.langchain4j.model.output.structured.Description;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static dev.langchain4j.agent.tool.JsonSchemaProperty.ARRAY;
import static dev.langchain4j.agent.tool.JsonSchemaProperty.BOOLEAN;
import static dev.langchain4j.agent.tool.JsonSchemaProperty.INTEGER;
import static dev.langchain4j.agent.tool.JsonSchemaProperty.NUMBER;
import static dev.langchain4j.agent.tool.JsonSchemaProperty.OBJECT;
import static dev.langchain4j.agent.tool.JsonSchemaProperty.STRING;
import static dev.langchain4j.agent.tool.JsonSchemaProperty.description;
import static dev.langchain4j.agent.tool.JsonSchemaProperty.enums;
import static dev.langchain4j.agent.tool.JsonSchemaProperty.from;
import static dev.langchain4j.agent.tool.JsonSchemaProperty.items;
import static dev.langchain4j.agent.tool.JsonSchemaProperty.objectItems;
import static dev.langchain4j.internal.TypeUtils.isJsonBoolean;
import static dev.langchain4j.internal.TypeUtils.isJsonInteger;
import static dev.langchain4j.internal.TypeUtils.isJsonNumber;
import static dev.langchain4j.internal.Utils.allConcreteMethods;
import static dev.langchain4j.internal.Utils.isNotNullOrBlank;
import static dev.langchain4j.internal.Utils.isNullOrBlank;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/** Utility methods for creating {@link ToolSpecification}s from annotated methods. */
public final class ToolSpecifications {
    private ToolSpecifications() { }

    public static List<ToolSpecification> toolSpecificationsFrom(Class<?> classWithTools) {
        List<ToolSpecification> specifications = allConcreteMethods(classWithTools).stream()
                .filter(method -> method.isAnnotationPresent(Tool.class))
                .map(ToolSpecifications::toolSpecificationFrom)
                .collect(toList());
        validateSpecifications(specifications);
        return specifications;
    }

    public static List<ToolSpecification> toolSpecificationsFrom(Object objectWithTools) {
        return toolSpecificationsFrom(objectWithTools.getClass());
    }

    public static void validateSpecifications(List<ToolSpecification> specifications) {
        Set<String> names = new HashSet<String>();
        for (ToolSpecification specification : specifications) {
            if (!names.add(specification.name())) {
                throw new IllegalArgumentException(format(
                        "Tool names must be unique. The tool '%s' appears several times", specification.name()));
            }
        }
    }

    public static ToolSpecification toolSpecificationFrom(Method method) {
        Tool annotation = method.getAnnotation(Tool.class);
        String name = isNullOrBlank(annotation.name()) ? method.getName() : annotation.name();
        String description = String.join("\n", annotation.value());
        ToolSpecification.Builder builder = ToolSpecification.builder().name(name).description(description);

        for (Parameter parameter : method.getParameters()) {
            if (isFrameworkParameter(parameter)) continue;
            P p = parameter.getAnnotation(P.class);
            boolean hasDefault = p != null && !P.NO_DEFAULT.equals(p.defaultValue());
            boolean required = !Optional.class.equals(parameter.getType())
                    && !hasDefault
                    && (p == null || p.required());
            String parameterName = p != null && isNotNullOrBlank(p.name())
                    ? p.name() : parameter.getName();
            if (required) {
                builder.addParameter(parameterName, toJsonSchemaProperties(parameter));
            } else {
                builder.addOptionalParameter(parameterName, toJsonSchemaProperties(parameter));
            }
        }
        return builder.build();
    }

    private static boolean isFrameworkParameter(Parameter parameter) {
        return parameter.isAnnotationPresent(ToolMemoryId.class)
                || InvocationParameters.class.isAssignableFrom(parameter.getType())
                || LangChain4jManaged.class.isAssignableFrom(parameter.getType())
                || parameter.getType() == InvocationContext.class;
    }

    static Iterable<JsonSchemaProperty> toJsonSchemaProperties(Parameter parameter) {
        Class<?> type = parameter.getType();
        P annotation = parameter.getAnnotation(P.class);
        JsonSchemaProperty parameterDescription = descriptionFrom(annotation, parameter);
        Iterable<JsonSchemaProperty> simpleType = toJsonSchemaProperties(type, parameterDescription);
        if (simpleType != null) return simpleType;
        if (Collection.class.isAssignableFrom(type)) {
            return removeNulls(ARRAY, arrayTypeFrom(parameter.getParameterizedType()), parameterDescription);
        }
        return removeNulls(OBJECT, schema(type), parameterDescription);
    }

    private static JsonSchemaProperty descriptionFrom(P annotation, Parameter parameter) {
        if (annotation == null) return null;
        boolean hasValue = isNotNullOrBlank(annotation.value());
        boolean hasDescription = isNotNullOrBlank(annotation.description());
        if (hasValue && hasDescription) {
            throw new IllegalArgumentException(format(
                    "Parameter '%s' has both 'value' and 'description' set in @P. Use one or the other, but not both.",
                    parameter.getName()));
        }
        if (hasDescription) return description(annotation.description());
        return hasValue ? description(annotation.value()) : null;
    }

    static JsonSchemaProperty schema(Class<?> structured) {
        return schema(structured, new HashMap<Class<?>, JsonSchemaProperty>());
    }

    private static JsonSchemaProperty schema(
            Class<?> structured, HashMap<Class<?>, JsonSchemaProperty> visited) {
        if (visited.containsKey(structured)) return visited.get(structured);
        visited.put(structured, null);
        Map<String, Object> properties = new HashMap<String, Object>();
        for (Field field : structured.getDeclaredFields()) {
            String name = field.getName();
            if (name.equals("this$0") || java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            Iterable<JsonSchemaProperty> schemaProperties = toJsonSchemaProperties(field, visited);
            Map<Object, Object> objectMap = new HashMap<Object, Object>();
            for (JsonSchemaProperty property : schemaProperties) {
                objectMap.put(property.key(), property.value());
            }
            properties.put(name, objectMap);
        }
        JsonSchemaProperty result = from("properties", properties);
        visited.put(structured, result);
        return result;
    }

    private static Iterable<JsonSchemaProperty> toJsonSchemaProperties(
            Field field, HashMap<Class<?>, JsonSchemaProperty> visited) {
        Class<?> type = field.getType();
        Description annotation = field.getAnnotation(Description.class);
        JsonSchemaProperty fieldDescription = annotation == null
                ? null : description(String.join(" ", annotation.value()));
        Iterable<JsonSchemaProperty> simpleType = toJsonSchemaProperties(type, fieldDescription);
        if (simpleType != null) return simpleType;
        if (Collection.class.isAssignableFrom(type)) {
            Type generic = field.getGenericType();
            Type item = generic instanceof ParameterizedType
                    ? ((ParameterizedType) generic).getActualTypeArguments()[0] : Object.class;
            return removeNulls(ARRAY, arrayTypeFrom(item), fieldDescription);
        }
        return removeNulls(OBJECT, schema(type, visited), fieldDescription);
    }

    private static Iterable<JsonSchemaProperty> toJsonSchemaProperties(
            Class<?> type, JsonSchemaProperty propertyDescription) {
        if (type == String.class) return removeNulls(STRING, propertyDescription);
        if (isJsonBoolean(type)) return removeNulls(BOOLEAN, propertyDescription);
        if (isJsonInteger(type)) return removeNulls(INTEGER, propertyDescription);
        if (isJsonNumber(type)) return removeNulls(NUMBER, propertyDescription);
        if (type.isArray()) return removeNulls(ARRAY, arrayTypeFrom(type.getComponentType()), propertyDescription);
        if (type.isEnum()) return removeNulls(STRING, enums(type), propertyDescription);
        return null;
    }

    private static JsonSchemaProperty arrayTypeFrom(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
            if (arguments.length == 1) return arrayTypeFrom(arguments[0]);
        }
        if (type instanceof Class<?>) return arrayTypeFrom((Class<?>) type);
        return items(OBJECT);
    }

    private static JsonSchemaProperty arrayTypeFrom(Class<?> type) {
        if (type == String.class) return items(STRING);
        if (isJsonBoolean(type)) return items(BOOLEAN);
        if (isJsonInteger(type)) return items(INTEGER);
        if (isJsonNumber(type)) return items(NUMBER);
        return objectItems(schema(type));
    }

    static Iterable<JsonSchemaProperty> removeNulls(JsonSchemaProperty... properties) {
        return stream(properties).filter(Objects::nonNull).collect(toList());
    }
}
