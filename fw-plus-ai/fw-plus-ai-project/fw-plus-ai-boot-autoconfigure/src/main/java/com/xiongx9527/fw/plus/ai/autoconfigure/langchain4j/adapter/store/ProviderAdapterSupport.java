package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ProviderAdapterSupport {

    private ProviderAdapterSupport() {
    }

    static boolean classPresent(String className) {
        try {
            Class.forName(className, false, classLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static Class<?> loadClass(String className) throws ClassNotFoundException {
        return Class.forName(className, true, classLoader());
    }

    static String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toLowerCase(Locale.ENGLISH);
    }

    static boolean matches(LangChain4jEmbeddingStoreProviderSpec.Condition condition,
                           AiEmbeddingStoreDefinition definition) {
        switch (condition.getKind()) {
            case ALWAYS:
                return true;
            case PRESENT:
                return present(property(definition, condition.getProperty()));
            case ABSENT:
                return !present(property(definition, condition.getProperty()));
            case DEFINED:
                return property(definition, condition.getProperty()) != null;
            case UNDEFINED:
                return property(definition, condition.getProperty()) == null;
            case ALL:
                for (LangChain4jEmbeddingStoreProviderSpec.Condition child : condition.getChildren()) {
                    if (!matches(child, definition)) {
                        return false;
                    }
                }
                return true;
            case ANY:
                for (LangChain4jEmbeddingStoreProviderSpec.Condition child : condition.getChildren()) {
                    if (matches(child, definition)) {
                        return true;
                    }
                }
                return false;
            default:
                throw new IllegalArgumentException("Unsupported condition: " + condition.getKind());
        }
    }

    static void validateRequirements(LangChain4jEmbeddingStoreProviderSpec spec,
                                     AiEmbeddingStoreDefinition definition) {
        for (LangChain4jEmbeddingStoreProviderSpec.Requirement requirement : spec.getRequirements()) {
            if (!matches(requirement.getCondition(), definition)) {
                continue;
            }
            if (requirement.isAny()) {
                boolean found = false;
                for (String property : requirement.getProperties()) {
                    if (present(property(definition, property))) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException("One of properties must be configured: "
                            + requirement.getProperties());
                }
            } else {
                for (String property : requirement.getProperties()) {
                    if (!present(property(definition, property))) {
                        throw new IllegalArgumentException(property + " must not be blank");
                    }
                }
            }
        }
    }

    static Object property(AiEmbeddingStoreDefinition definition, String name) {
        if (definition == null) {
            return null;
        }
        if ("key".equals(name)) {
            return definition.getKey();
        }
        if ("framework".equals(name)) {
            return definition.getFramework();
        }
        if ("provider".equals(name)) {
            return definition.getProvider();
        }
        if ("type".equals(name)) {
            return definition.getType();
        }
        if ("enabled".equals(name)) {
            return definition.isEnabled();
        }
        return definition.getProperties() == null ? null : definition.getProperties().get(name);
    }

    static Object invokeStaticNoArg(Class<?> type, String methodName) throws Exception {
        List<Method> matches = new ArrayList<Method>();
        for (Method method : type.getMethods()) {
            if (methodName.equals(method.getName()) && method.getParameterTypes().length == 0
                    && Modifier.isStatic(method.getModifiers())) {
                matches.add(method);
            }
        }
        if (matches.size() != 1) {
            throw new IllegalArgumentException("Expected one static " + methodName + " method on "
                    + type.getName() + ", found=" + matches.size());
        }
        return matches.get(0).invoke(null);
    }

    static Object invokeNoArg(Object target, String methodName) throws Exception {
        List<Method> matches = new ArrayList<Method>();
        for (Method method : target.getClass().getMethods()) {
            if (methodName.equals(method.getName()) && method.getParameterTypes().length == 0
                    && !Modifier.isStatic(method.getModifiers())) {
                matches.add(method);
            }
        }
        if (matches.size() != 1) {
            throw new IllegalArgumentException("Expected one " + methodName + " method on "
                    + target.getClass().getName() + ", found=" + matches.size());
        }
        return matches.get(0).invoke(target);
    }

    static Object invoke(Object target, String methodName,
                         List<LangChain4jEmbeddingStoreProviderSpec.Value> values,
                         AiEmbeddingStoreDefinition definition,
                         ApplicationContext applicationContext) throws Exception {
        List<ExecutableMatch<Method>> matches = new ArrayList<ExecutableMatch<Method>>();
        for (Method method : target.getClass().getMethods()) {
            if (!methodName.equals(method.getName()) || Modifier.isStatic(method.getModifiers())
                    || method.getParameterTypes().length != values.size()) {
                continue;
            }
            ResolvedArguments resolved = resolveArguments(values, method.getParameterTypes(),
                    definition, applicationContext);
            if (resolved != null) {
                matches.add(new ExecutableMatch<Method>(method, resolved, method.getParameterTypes()));
            }
        }
        ExecutableMatch<Method> match = uniqueBest(matches, "method " + methodName
                + " on " + target.getClass().getName());
        Object result = match.executable.invoke(target, match.arguments.values);
        return result == null ? target : result;
    }

    static Object construct(Class<?> type,
                            List<LangChain4jEmbeddingStoreProviderSpec.Value> values,
                            AiEmbeddingStoreDefinition definition,
                            ApplicationContext applicationContext) throws Exception {
        List<ExecutableMatch<Constructor<?>>> matches = new ArrayList<ExecutableMatch<Constructor<?>>>();
        for (Constructor<?> constructor : type.getConstructors()) {
            if (constructor.getParameterTypes().length != values.size()) {
                continue;
            }
            ResolvedArguments resolved = resolveArguments(values, constructor.getParameterTypes(),
                    definition, applicationContext);
            if (resolved != null) {
                matches.add(new ExecutableMatch<Constructor<?>>(constructor, resolved, constructor.getParameterTypes()));
            }
        }
        ExecutableMatch<Constructor<?>> match = uniqueBest(matches, "constructor on " + type.getName());
        return match.executable.newInstance(match.arguments.values);
    }

    static Object constructRaw(Class<?> type, List<?> rawValues,
                               ApplicationContext applicationContext) throws Exception {
        List<ExecutableMatch<Constructor<?>>> matches = new ArrayList<ExecutableMatch<Constructor<?>>>();
        for (Constructor<?> constructor : type.getConstructors()) {
            if (constructor.getParameterTypes().length != rawValues.size()) {
                continue;
            }
            ResolvedArguments resolved = resolveRawArguments(rawValues, constructor.getParameterTypes(),
                    applicationContext);
            if (resolved != null) {
                matches.add(new ExecutableMatch<Constructor<?>>(constructor, resolved, constructor.getParameterTypes()));
            }
        }
        ExecutableMatch<Constructor<?>> match = uniqueBest(matches, "constructor on " + type.getName());
        return match.executable.newInstance(match.arguments.values);
    }

    static AiEmbeddingStoreException exception(AiEmbeddingStoreDefinition definition,
                                               String detail, Throwable cause) {
        return new AiEmbeddingStoreException("Failed to create LangChain4j embedding store: "
                + context(definition) + ", detail=" + detail
                + (cause == null ? "" : ", causeType=" + cause.getClass().getName()), cause);
    }

    static String context(AiEmbeddingStoreDefinition definition) {
        if (definition == null) {
            return "key=unknown, framework=unknown, provider=unknown, type=unknown";
        }
        return "key=" + safe(definition.getKey())
                + ", framework=" + safe(definition.getFramework())
                + ", provider=" + safe(definition.getProvider())
                + ", type=" + safe(definition.getType());
    }

    private static ResolvedArguments resolveArguments(
            List<LangChain4jEmbeddingStoreProviderSpec.Value> values,
            Class<?>[] targetTypes,
            AiEmbeddingStoreDefinition definition,
            ApplicationContext applicationContext) {
        Object[] resolved = new Object[values.size()];
        int score = 0;
        for (int i = 0; i < values.size(); i++) {
            Conversion conversion = resolve(values.get(i), targetTypes[i], definition, applicationContext);
            if (conversion == null) {
                return null;
            }
            resolved[i] = conversion.value;
            score += conversion.score;
        }
        return new ResolvedArguments(resolved, score);
    }

    private static ResolvedArguments resolveRawArguments(List<?> values, Class<?>[] targetTypes,
                                                         ApplicationContext applicationContext) {
        Object[] resolved = new Object[values.size()];
        int score = 0;
        for (int i = 0; i < values.size(); i++) {
            Conversion conversion = convert(values.get(i), targetTypes[i], applicationContext, false);
            if (conversion == null) {
                return null;
            }
            resolved[i] = conversion.value;
            score += conversion.score;
        }
        return new ResolvedArguments(resolved, score);
    }

    private static Conversion resolve(LangChain4jEmbeddingStoreProviderSpec.Value value,
                                      Class<?> targetType,
                                      AiEmbeddingStoreDefinition definition,
                                      ApplicationContext applicationContext) {
        if (value.getKind() == LangChain4jEmbeddingStoreProviderSpec.ValueKind.CONSTANT) {
            return convert(value.getConstant(), targetType, applicationContext, false);
        }
        Object raw = null;
        for (String source : value.getSources()) {
            Object candidate = property(definition, source);
            if (candidate != null) {
                raw = candidate;
                break;
            }
        }
        if (value.getKind() == LangChain4jEmbeddingStoreProviderSpec.ValueKind.BEAN) {
            return convert(raw, targetType, applicationContext, true);
        }
        return convert(raw, targetType, applicationContext, false);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Conversion convert(Object value, Class<?> targetType,
                                      ApplicationContext applicationContext,
                                      boolean beanReference) {
        Class<?> wrapper = wrapperType(targetType);
        if (value == null) {
            return targetType.isPrimitive() ? null : new Conversion(null, 400);
        }
        if (wrapper.isInstance(value)) {
            return new Conversion(value, typeDistance(value.getClass(), wrapper));
        }
        if (beanReference) {
            if (!(value instanceof String) || applicationContext == null) {
                return null;
            }
            try {
                return new Conversion(applicationContext.getBean(String.valueOf(value), targetType), 300);
            } catch (RuntimeException e) {
                return null;
            }
        }
        if (String.class.equals(wrapper)) {
            return new Conversion(String.valueOf(value), 200);
        }
        try {
            if (Integer.class.equals(wrapper)) {
                return new Conversion(value instanceof Number
                        ? Integer.valueOf(((Number) value).intValue())
                        : Integer.valueOf(String.valueOf(value)), value instanceof Number ? 101 : 103);
            }
            if (Long.class.equals(wrapper)) {
                return new Conversion(value instanceof Number
                        ? Long.valueOf(((Number) value).longValue())
                        : Long.valueOf(String.valueOf(value)), value instanceof Number ? 101 : 103);
            }
            if (Double.class.equals(wrapper)) {
                return new Conversion(value instanceof Number
                        ? Double.valueOf(((Number) value).doubleValue())
                        : Double.valueOf(String.valueOf(value)), value instanceof Number ? 101 : 103);
            }
            if (Float.class.equals(wrapper)) {
                return new Conversion(value instanceof Number
                        ? Float.valueOf(((Number) value).floatValue())
                        : Float.valueOf(String.valueOf(value)), value instanceof Number ? 101 : 103);
            }
            if (Short.class.equals(wrapper)) {
                return new Conversion(value instanceof Number
                        ? Short.valueOf(((Number) value).shortValue())
                        : Short.valueOf(String.valueOf(value)), value instanceof Number ? 101 : 103);
            }
            if (Byte.class.equals(wrapper)) {
                return new Conversion(value instanceof Number
                        ? Byte.valueOf(((Number) value).byteValue())
                        : Byte.valueOf(String.valueOf(value)), value instanceof Number ? 101 : 103);
            }
            if (Boolean.class.equals(wrapper)) {
                if (value instanceof Boolean) {
                    return new Conversion(value, 0);
                }
                return new Conversion(Boolean.valueOf(String.valueOf(value)), 103);
            }
            if (Duration.class.equals(wrapper)) {
                return new Conversion(value instanceof Number
                        ? Duration.ofMillis(((Number) value).longValue())
                        : Duration.parse(String.valueOf(value)), 103);
            }
            if (Enum.class.isAssignableFrom(wrapper)) {
                String enumName = String.valueOf(value).trim().replace('-', '_').toUpperCase(Locale.ENGLISH);
                return new Conversion(Enum.valueOf((Class<Enum>) wrapper, enumName), 103);
            }
            if (Collection.class.isAssignableFrom(wrapper)) {
                if (value instanceof Collection) {
                    return new Conversion(new ArrayList<Object>((Collection<?>) value), 101);
                }
                String text = String.valueOf(value);
                if (text.trim().length() == 0) {
                    return new Conversion(Collections.emptyList(), 103);
                }
                List<String> values = new ArrayList<String>();
                for (String part : text.split(",")) {
                    values.add(part.trim());
                }
                return new Conversion(values, 103);
            }
            if (applicationContext != null && value instanceof String) {
                try {
                    return new Conversion(applicationContext.getBean(String.valueOf(value), targetType), 305);
                } catch (RuntimeException ignored) {
                    return null;
                }
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private static int typeDistance(Class<?> source, Class<?> target) {
        if (source.equals(target)) {
            return 0;
        }
        ArrayDeque<Class<?>> queue = new ArrayDeque<Class<?>>();
        Map<Class<?>, Integer> distances = new LinkedHashMap<Class<?>, Integer>();
        queue.add(source);
        distances.put(source, Integer.valueOf(0));
        while (!queue.isEmpty()) {
            Class<?> current = queue.removeFirst();
            int nextDistance = distances.get(current).intValue() + 1;
            Class<?> parent = current.getSuperclass();
            if (parent != null && !distances.containsKey(parent)) {
                if (parent.equals(target)) {
                    return nextDistance;
                }
                distances.put(parent, Integer.valueOf(nextDistance));
                queue.addLast(parent);
            }
            for (Class<?> contract : current.getInterfaces()) {
                if (distances.containsKey(contract)) {
                    continue;
                }
                if (contract.equals(target)) {
                    return nextDistance;
                }
                distances.put(contract, Integer.valueOf(nextDistance));
                queue.addLast(contract);
            }
        }
        return 50;
    }

    private static <T> ExecutableMatch<T> uniqueBest(List<ExecutableMatch<T>> matches, String description) {
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No matching " + description);
        }
        Collections.sort(matches, new Comparator<ExecutableMatch<T>>() {
            @Override
            public int compare(ExecutableMatch<T> first, ExecutableMatch<T> second) {
                return Integer.compare(first.arguments.score, second.arguments.score);
            }
        });
        ExecutableMatch<T> best = matches.get(0);
        List<ExecutableMatch<T>> tied = new ArrayList<ExecutableMatch<T>>();
        for (ExecutableMatch<T> match : matches) {
            if (match.arguments.score == best.arguments.score) {
                tied.add(match);
            }
        }
        if (tied.size() == 1) {
            return best;
        }
        ExecutableMatch<T> mostSpecific = null;
        for (ExecutableMatch<T> candidate : tied) {
            boolean moreSpecificThanAll = true;
            for (ExecutableMatch<T> other : tied) {
                if (candidate != other && !moreSpecific(candidate.parameterTypes, other.parameterTypes)) {
                    moreSpecificThanAll = false;
                    break;
                }
            }
            if (moreSpecificThanAll) {
                if (mostSpecific != null) {
                    throw new IllegalArgumentException("Ambiguous " + description + ": matches=" + tied.size());
                }
                mostSpecific = candidate;
            }
        }
        if (mostSpecific == null) {
            throw new IllegalArgumentException("Ambiguous " + description + ": matches=" + tied.size());
        }
        return mostSpecific;
    }

    private static boolean moreSpecific(Class<?>[] candidate, Class<?>[] other) {
        boolean strict = false;
        for (int index = 0; index < candidate.length; index++) {
            Class<?> candidateType = wrapperType(candidate[index]);
            Class<?> otherType = wrapperType(other[index]);
            if (!otherType.isAssignableFrom(candidateType)) {
                return false;
            }
            if (!candidateType.equals(otherType)) {
                strict = true;
            }
        }
        return strict;
    }

    private static boolean present(Object value) {
        return value != null && (!(value instanceof String) || ((String) value).trim().length() > 0);
    }

    private static ClassLoader classLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader == null ? ProviderAdapterSupport.class.getClassLoader() : loader;
    }

    private static Class<?> wrapperType(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (Integer.TYPE.equals(type)) return Integer.class;
        if (Long.TYPE.equals(type)) return Long.class;
        if (Double.TYPE.equals(type)) return Double.class;
        if (Float.TYPE.equals(type)) return Float.class;
        if (Short.TYPE.equals(type)) return Short.class;
        if (Byte.TYPE.equals(type)) return Byte.class;
        if (Boolean.TYPE.equals(type)) return Boolean.class;
        if (Character.TYPE.equals(type)) return Character.class;
        return type;
    }

    private static String safe(Object value) {
        return value == null ? "unknown" : String.valueOf(value);
    }

    private static final class Conversion {
        private final Object value;
        private final int score;
        private Conversion(Object value, int score) {
            this.value = value;
            this.score = score;
        }
    }

    private static final class ResolvedArguments {
        private final Object[] values;
        private final int score;
        private ResolvedArguments(Object[] values, int score) {
            this.values = values;
            this.score = score;
        }
    }

    private static final class ExecutableMatch<T> {
        private final T executable;
        private final ResolvedArguments arguments;
        private final Class<?>[] parameterTypes;
        private ExecutableMatch(T executable, ResolvedArguments arguments, Class<?>[] parameterTypes) {
            this.executable = executable;
            this.arguments = arguments;
            this.parameterTypes = parameterTypes.clone();
        }
    }
}
