package dev.langchain4j.internal;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** Java 8-compatible utility surface retained from LangChain4j 0.35. */
public final class Utils {
    private Utils() {
    }

    public static <T> T getOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static <T> T getOrDefault(T value, Supplier<T> defaultValueSupplier) {
        return value == null ? defaultValueSupplier.get() : value;
    }

    public static boolean isNotNullOrBlank(String string) { return !isNullOrBlank(string); }

    public static boolean isNotNullOrEmpty(String string) { return !isNullOrEmpty(string); }

    public static boolean areNotNullOrBlank(String... strings) {
        if (strings == null || strings.length == 0) return false;
        for (String string : strings) if (isNullOrBlank(string)) return false;
        return true;
    }

    /** @deprecated use isNullOrEmpty(Collection). */
    @Deprecated
    public static boolean isCollectionEmpty(Collection<?> collection) { return isNullOrEmpty(collection); }

    public static String repeat(String string, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) builder.append(string);
        return builder.toString();
    }

    public static String randomUUID() { return UUID.randomUUID().toString(); }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalArgumentException(exception); }
    }

    public static String generateUUIDFrom(String input) {
        byte[] hash = sha256().digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte value : hash) hex.append(String.format("%02x", value));
        return UUID.nameUUIDFromBytes(hex.toString().getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static String ensureTrailingForwardSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    public static String firstChars(String string, int numberOfChars) {
        if (string == null) return null;
        return string.length() > numberOfChars ? string.substring(0, numberOfChars) : string;
    }

    public static byte[] readBytes(String url) {
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new RuntimeException("Error while reading: " + connection.getResponseCode());
                }
                InputStream input = connection.getInputStream();
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                return output.toByteArray();
            }
            return Files.readAllBytes(Paths.get(new URI(url)));
        } catch (Exception exception) { throw new RuntimeException(exception); }
    }

    public static boolean isNullOrEmpty(Map<?, ?> map) { return map == null || map.isEmpty(); }

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static boolean isNullOrEmpty(Iterable<?> iterable) {
        return iterable == null || !iterable.iterator().hasNext();
    }

    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNullOrBlank(String string) {
        return string == null || string.trim().isEmpty();
    }

    public static <T> List<T> copyIfNotNull(List<T> list) {
        return list == null ? null : new ArrayList<T>(list);
    }

    public static <K, V> Map<K, V> copyIfNotNull(Map<K, V> map) {
        return map == null ? null : new LinkedHashMap<K, V>(map);
    }

    public static <T> List<T> copy(List<? extends T> list) {
        return list == null ? Collections.<T>emptyList()
                : Collections.unmodifiableList(new ArrayList<T>(list));
    }

    public static <K, V> Map<K, V> copy(Map<K, V> map) {
        return map == null ? Collections.<K, V>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<K, V>(map));
    }

    public static <T> Set<T> copy(Set<T> set) {
        return set == null ? Collections.<T>emptySet()
                : Collections.unmodifiableSet(new java.util.LinkedHashSet<T>(set));
    }

    public static String quoted(Object object) {
        return object == null ? "null" : "\"" + String.valueOf(object) + "\"";
    }

    @SafeVarargs
    public static <T> List<T> merge(List<T>... lists) {
        if (lists.length < 2) throw new IllegalArgumentException("lists must have at least 2 elements");
        List<T> result = new ArrayList<T>();
        for (List<T> list : lists) if (list != null) result.addAll(list);
        return result;
    }

    @SafeVarargs
    public static <K, V> Map<K, V> merge(Map<K, V>... maps) {
        if (maps.length < 2) throw new IllegalArgumentException("maps must have at least 2 elements");
        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map<K, V> map : maps) {
            if (map == null) continue;
            for (Map.Entry<K, V> entry : map.entrySet()) {
                if (result.containsKey(entry.getKey())) {
                    throw new IllegalArgumentException("Duplicate key: " + entry.getKey());
                }
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public static Optional<Method> getAnnotatedMethod(
            Method method, Class<? extends Annotation> annotation) {
        if (method.isAnnotationPresent(annotation)) {
            return Optional.of(method);
        }
        if (Proxy.isProxyClass(method.getDeclaringClass())) {
            for (Class<?> interfaceClass : method.getDeclaringClass().getInterfaces()) {
                try {
                    Method interfaceMethod = interfaceClass.getMethod(
                            method.getName(), method.getParameterTypes());
                    if (interfaceMethod.isAnnotationPresent(annotation)) {
                        return Optional.of(interfaceMethod);
                    }
                } catch (NoSuchMethodException ignored) {
                    // Continue searching the remaining proxy interfaces.
                }
            }
        }
        return Optional.empty();
    }

    public static List<Method> allConcreteMethods(Class<?> clazz) {
        return allMethods(clazz, true);
    }

    public static List<Method> allMethods(Class<?> clazz) {
        return allMethods(clazz, false);
    }

    private static List<Method> allMethods(Class<?> clazz, boolean concreteOnly) {
        List<Method> methods = new ArrayList<Method>();
        Set<MethodSignature> seen = new HashSet<MethodSignature>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            collectDeclaredMethods(current, seen, methods);
            current = current.getSuperclass();
        }
        collectInterfaceMethods(clazz, seen, methods, new HashSet<Class<?>>(), concreteOnly);
        return Collections.unmodifiableList(methods);
    }

    private static void collectDeclaredMethods(
            Class<?> clazz, Set<MethodSignature> seen, List<Method> methods) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isBridge() || method.isSynthetic()
                    || Modifier.isAbstract(method.getModifiers())) {
                continue;
            }
            if (seen.add(new MethodSignature(method))) {
                methods.add(method);
            }
        }
    }

    private static void collectInterfaceMethods(
            Class<?> clazz,
            Set<MethodSignature> seen,
            List<Method> methods,
            Set<Class<?>> visited,
            boolean concreteOnly) {
        if (clazz == null) {
            return;
        }
        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            if (!visited.add(interfaceClass)) {
                continue;
            }
            for (Method method : interfaceClass.getDeclaredMethods()) {
                if (method.isBridge() || method.isSynthetic()
                        || (concreteOnly && Modifier.isAbstract(method.getModifiers()))) {
                    continue;
                }
                if (seen.add(new MethodSignature(method))) {
                    methods.add(method);
                }
            }
            collectInterfaceMethods(interfaceClass, seen, methods, visited, concreteOnly);
        }
        collectInterfaceMethods(clazz.getSuperclass(), seen, methods, visited, concreteOnly);
    }

    private static final class MethodSignature {
        private final String name;
        private final List<Class<?>> parameterTypes;

        private MethodSignature(Method method) {
            this.name = method.getName();
            this.parameterTypes = Arrays.asList(method.getParameterTypes());
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof MethodSignature)) {
                return false;
            }
            MethodSignature other = (MethodSignature) object;
            return name.equals(other.name) && parameterTypes.equals(other.parameterTypes);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + parameterTypes.hashCode();
        }
    }
}
