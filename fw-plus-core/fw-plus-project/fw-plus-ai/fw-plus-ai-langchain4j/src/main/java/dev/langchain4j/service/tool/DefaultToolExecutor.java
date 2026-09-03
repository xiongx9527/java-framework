package dev.langchain4j.service.tool;

import com.google.gson.Gson;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.exception.ToolArgumentsException;
import dev.langchain4j.exception.ToolExecutionException;
import dev.langchain4j.internal.Json;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.invocation.LangChain4jManaged;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static dev.langchain4j.internal.Utils.allConcreteMethods;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.Utils.isNotNullOrBlank;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.service.tool.ToolExecutionRequestUtil.argumentsAsMap;

public class DefaultToolExecutor implements ToolExecutor {

    private static final Gson GSON = new Gson();

    private final Object object;
    private final Method originalMethod;
    private final Method methodToInvoke;
    private final boolean wrapToolArgumentsExceptions;
    private final boolean propagateToolExecutionExceptions;

    public DefaultToolExecutor(Builder builder) {
        object = ensureNotNull(builder.object, "object");
        originalMethod = ensureNotNull(builder.originalMethod, "originalMethod");
        methodToInvoke = ensureNotNull(builder.methodToInvoke, "methodToInvoke");
        wrapToolArgumentsExceptions = getOrDefault(builder.wrapToolArgumentsExceptions, Boolean.FALSE);
        propagateToolExecutionExceptions = getOrDefault(builder.propagateToolExecutionExceptions, Boolean.FALSE);
    }

    public DefaultToolExecutor(Object object, Method method) {
        this.object = ensureNotNull(object, "object");
        originalMethod = ensureNotNull(method, "method");
        methodToInvoke = originalMethod;
        wrapToolArgumentsExceptions = false;
        propagateToolExecutionExceptions = false;
    }

    public DefaultToolExecutor(Object object, ToolExecutionRequest request) {
        this.object = ensureNotNull(object, "object");
        ensureNotNull(request, "toolExecutionRequest");
        originalMethod = findMethod(object, request);
        methodToInvoke = originalMethod;
        wrapToolArgumentsExceptions = false;
        propagateToolExecutionExceptions = false;
    }

    public DefaultToolExecutor(Object object, Method originalMethod, Method methodToInvoke) {
        this.object = ensureNotNull(object, "object");
        this.originalMethod = ensureNotNull(originalMethod, "originalMethod");
        this.methodToInvoke = ensureNotNull(methodToInvoke, "methodToInvoke");
        wrapToolArgumentsExceptions = false;
        propagateToolExecutionExceptions = false;
    }

    public Method originalMethod() { return originalMethod; }

    Method findMethod(Object target, ToolExecutionRequest request) {
        for (Method method : allConcreteMethods(target.getClass())) {
            if (method.getName().equals(request.name())) return method;
        }
        throw new IllegalArgumentException(String.format(
                "Method '%s' is not found in object '%s'", request.name(), target.getClass().getName()));
    }

    @Override
    public ToolExecutionResult executeWithContext(ToolExecutionRequest request, InvocationContext context) {
        Object[] arguments = prepareArguments(request, context);
        try {
            return invoke(arguments);
        } catch (IllegalAccessException first) {
            try {
                methodToInvoke.setAccessible(true);
                return invoke(arguments);
            } catch (IllegalAccessException second) {
                throw new RuntimeException(second);
            } catch (InvocationTargetException second) {
                return handleExecutionException(second.getCause());
            }
        } catch (InvocationTargetException exception) {
            return handleExecutionException(exception.getCause());
        }
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        return executeWithContext(
                request, InvocationContext.builder().chatMemoryId(memoryId).build()).resultText();
    }

    private Object[] prepareArguments(ToolExecutionRequest request, InvocationContext context) {
        try {
            return prepareArguments(originalMethod, request.name(), argumentsAsMap(request.arguments()), context);
        } catch (RuntimeException exception) {
            if (wrapToolArgumentsExceptions) throw new ToolArgumentsException(exception);
            throw exception;
        }
    }

    private ToolExecutionResult invoke(Object[] arguments)
            throws IllegalAccessException, InvocationTargetException {
        final Object result = methodToInvoke.invoke(object, arguments);
        List<Content> contents = toContents(result);
        if (contents != null) {
            return ToolExecutionResult.builder().result(result).resultContents(contents).build();
        }
        return ToolExecutionResult.builder()
                .result(result)
                .resultTextSupplier(() -> toText(result))
                .build();
    }

    private ToolExecutionResult handleExecutionException(Throwable cause) {
        if (propagateToolExecutionExceptions) throw new ToolExecutionException(cause);
        String message = cause.getMessage() == null ? cause.getClass().getName() : cause.getMessage();
        return ToolExecutionResult.builder().isError(true).resultText(message).build();
    }

    private static List<Content> toContents(Object result) {
        if (result instanceof Image) {
            return java.util.Collections.<Content>singletonList(ImageContent.from((Image) result));
        }
        if (result instanceof Content) return java.util.Collections.singletonList((Content) result);
        if (result instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) result;
            if (!collection.isEmpty() && collection.iterator().next() instanceof Content) {
                List<Content> contents = new ArrayList<Content>(collection.size());
                for (Object value : collection) contents.add((Content) value);
                return contents;
            }
        }
        return result instanceof Content[] ? Arrays.asList((Content[]) result) : null;
    }

    private String toText(Object result) {
        Class<?> returnType = methodToInvoke.getReturnType();
        if (returnType == void.class) return "Success";
        if (returnType == String.class) return result == null ? "null" : (String) result;
        return Json.toJson(result);
    }

    static Object[] prepareArguments(Method method, Map<String, Object> argumentsMap, Object memoryId) {
        return prepareArguments(method, method.getName(), argumentsMap,
                InvocationContext.builder().chatMemoryId(memoryId).build());
    }

    static Object[] prepareArguments(
            Method method, String toolName, Map<String, Object> argumentsMap, InvocationContext context) {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(ToolMemoryId.class)) {
                arguments[i] = context == null ? null : context.chatMemoryId();
                continue;
            }
            if (InvocationParameters.class.isAssignableFrom(parameter.getType())) {
                arguments[i] = context == null ? null : context.invocationParameters();
                continue;
            }
            if (parameter.getType() == InvocationContext.class) {
                arguments[i] = context;
                continue;
            }
            if (LangChain4jManaged.class.isAssignableFrom(parameter.getType())) {
                arguments[i] = context == null ? null : context.managedParameters().get(parameter.getType());
                continue;
            }
            String name = getName(parameter);
            Object argument = argumentsMap.get(name);
            Class<?> parameterClass = parameter.getType();
            Type parameterType = parameter.getParameterizedType();
            if (parameterClass == Optional.class) {
                arguments[i] = createOptional(argument, name, parameterType);
            } else if (argument != null) {
                arguments[i] = coerceArgument(argument, name, parameterClass, parameterType);
            } else {
                P annotation = parameter.getAnnotation(P.class);
                if (annotation != null && !P.NO_DEFAULT.equals(annotation.defaultValue())) {
                    arguments[i] = parseDefaultValue(
                            annotation.defaultValue(), name, parameterClass, parameterType);
                } else if (parameterClass.isPrimitive()) {
                    throw new IllegalArgumentException(String.format(
                            "Required parameter \"%s\" of tool \"%s\" is missing", name, toolName));
                }
            }
        }
        return arguments;
    }

    private static String getName(Parameter parameter) {
        P annotation = parameter.getAnnotation(P.class);
        return annotation != null && isNotNullOrBlank(annotation.name())
                ? annotation.name() : parameter.getName();
    }

    private static Optional<?> createOptional(Object argument, String name, Type type) {
        if (argument == null) return Optional.empty();
        Type actualType = ((ParameterizedType) type).getActualTypeArguments()[0];
        Class<?> actualClass = actualType instanceof Class<?>
                ? (Class<?>) actualType : (Class<?>) ((ParameterizedType) actualType).getRawType();
        return Optional.of(coerceArgument(argument, name, actualClass, actualType));
    }

    static Object parseDefaultValue(String value, String name, Class<?> parameterClass, Type type) {
        if (parameterClass == String.class || parameterClass.isEnum() || parameterClass == UUID.class) {
            return coerceArgument(value, name, parameterClass, type);
        }
        Object parsed;
        try {
            parsed = GSON.fromJson(value, Object.class);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(String.format(
                    "Cannot parse @P(defaultValue = \"%s\") for parameter \"%s\" of type %s: %s",
                    value, name, parameterClass.getName(), exception.getMessage()), exception);
        }
        if (parsed == null) {
            throw new IllegalArgumentException(String.format(
                    "@P(defaultValue = \"%s\") parses to null for parameter \"%s\" of type %s",
                    value, name, parameterClass.getName()));
        }
        return coerceArgument(parsed, name, parameterClass, type);
    }

    static Object coerceArgument(Object argument, String name, Class<?> parameterClass) {
        return coerceArgument(argument, name, parameterClass, parameterClass);
    }

    static Object coerceArgument(Object argument, String name, Class<?> parameterClass, Type type) {
        if (parameterClass == String.class) return argument.toString();
        if (parameterClass.isEnum()) {
            try {
                @SuppressWarnings({"rawtypes", "unchecked"}) Class<Enum> enumClass = (Class<Enum>) parameterClass;
                try {
                    return Enum.valueOf(enumClass, Objects.requireNonNull(argument).toString());
                } catch (IllegalArgumentException exception) {
                    return Enum.valueOf(enumClass,
                            Objects.requireNonNull(argument).toString().toUpperCase(Locale.ROOT));
                }
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException(String.format(
                        "Argument \"%s\" is not a valid enum value for %s: <%s>",
                        name, parameterClass.getName(), argument), exception);
            }
        }
        if (parameterClass == Boolean.class || parameterClass == boolean.class) {
            if (argument instanceof Boolean) return argument;
            throw conversionError(argument, name, parameterClass);
        }
        if (parameterClass == Double.class || parameterClass == double.class) {
            return getDoubleValue(argument, name, parameterClass);
        }
        if (parameterClass == Float.class || parameterClass == float.class) {
            double value = getDoubleValue(argument, name, parameterClass);
            checkBounds(value, name, parameterClass, -Float.MAX_VALUE, Float.MAX_VALUE);
            return Float.valueOf((float) value);
        }
        if (parameterClass == BigDecimal.class) return getBigDecimalValue(argument, name, parameterClass);
        if (parameterClass == Integer.class || parameterClass == int.class) {
            return Integer.valueOf((int) getBoundedLongValue(
                    argument, name, parameterClass, Integer.MIN_VALUE, Integer.MAX_VALUE));
        }
        if (parameterClass == Long.class || parameterClass == long.class) {
            return Long.valueOf(getBoundedLongValue(
                    argument, name, parameterClass, Long.MIN_VALUE, Long.MAX_VALUE));
        }
        if (parameterClass == Short.class || parameterClass == short.class) {
            return Short.valueOf((short) getBoundedLongValue(
                    argument, name, parameterClass, Short.MIN_VALUE, Short.MAX_VALUE));
        }
        if (parameterClass == Byte.class || parameterClass == byte.class) {
            return Byte.valueOf((byte) getBoundedLongValue(
                    argument, name, parameterClass, Byte.MIN_VALUE, Byte.MAX_VALUE));
        }
        if (parameterClass == BigInteger.class) return getBigIntegerValue(argument, name, parameterClass);
        if (parameterClass == UUID.class) return UUID.fromString(argument.toString());
        try {
            return GSON.fromJson(GSON.toJson(argument), type);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" is not convertable to %s: <%s>",
                    name, parameterClass.getName(), argument), exception);
        }
    }

    private static double getDoubleValue(Object argument, String name, Class<?> type) {
        if (argument instanceof String) {
            try { return Double.parseDouble(argument.toString()); }
            catch (NumberFormatException ignored) { }
        }
        if (!(argument instanceof Number)) throw conversionError(argument, name, type);
        return ((Number) argument).doubleValue();
    }

    private static void checkBounds(double value, String name, Class<?> type, double min, double max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" is out of range for %s: <%s>", name, type.getName(), value));
        }
    }

    public static long getBoundedLongValue(
            Object argument, String name, Class<?> type, long min, long max) {
        BigInteger value = getBigIntegerValue(argument, name, type);
        if (value.compareTo(BigInteger.valueOf(min)) < 0 || value.compareTo(BigInteger.valueOf(max)) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" is out of range for %s: <%s>", name, type.getName(), argument));
        }
        return value.longValue();
    }

    private static BigInteger getBigIntegerValue(Object argument, String name, Class<?> type) {
        try { return getBigDecimalValue(argument, name, type).toBigIntegerExact(); }
        catch (ArithmeticException exception) {
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" has non-integer value for %s: <%s>",
                    name, type.getName(), argument), exception);
        }
    }

    private static BigDecimal getBigDecimalValue(Object argument, String name, Class<?> type) {
        try {
            if (argument instanceof BigDecimal) return (BigDecimal) argument;
            if (argument instanceof BigInteger) return new BigDecimal((BigInteger) argument);
            if (argument instanceof Number) return new BigDecimal(argument.toString());
            if (argument instanceof String) return new BigDecimal(argument.toString().trim());
        } catch (NumberFormatException ignored) { }
        throw conversionError(argument, name, type);
    }

    private static IllegalArgumentException conversionError(Object argument, String name, Class<?> type) {
        return new IllegalArgumentException(String.format(
                "Argument \"%s\" is not convertable to %s, got %s: <%s>",
                name, type.getName(), argument == null ? "null" : argument.getClass().getName(), argument));
    }

    static boolean hasNoFractionalPart(Double value) {
        return value != null && value.equals(Math.floor(value.doubleValue()));
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Object object;
        private Method originalMethod;
        private Method methodToInvoke;
        private Boolean wrapToolArgumentsExceptions;
        private Boolean propagateToolExecutionExceptions;
        public Builder object(Object value) { object = value; return this; }
        public Builder originalMethod(Method value) { originalMethod = value; return this; }
        public Builder methodToInvoke(Method value) { methodToInvoke = value; return this; }
        public Builder wrapToolArgumentsExceptions(Boolean value) {
            wrapToolArgumentsExceptions = value;
            return this;
        }
        public Builder propagateToolExecutionExceptions(Boolean value) {
            propagateToolExecutionExceptions = value;
            return this;
        }
        public DefaultToolExecutor build() { return new DefaultToolExecutor(this); }
    }
}
