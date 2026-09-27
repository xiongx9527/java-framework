package dev.langchain4j.invocation;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Mutable, thread-safe parameters available during one invocation. */
public class InvocationParameters {

    private final ConcurrentHashMap<String, Object> map;

    public InvocationParameters() {
        this.map = new ConcurrentHashMap<String, Object>();
    }

    public InvocationParameters(Map<String, Object> map) {
        if (map == null) {
            throw new NullPointerException("map");
        }
        this.map = new ConcurrentHashMap<String, Object>(map);
    }

    public Map<String, Object> asMap() {
        return map;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) map.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        Object value = map.get(key);
        return (T) (value == null ? defaultValue : value);
    }

    public <T> void put(String key, T value) {
        map.put(key, value);
    }

    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        InvocationParameters that = (InvocationParameters) object;
        return Objects.equals(map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(map);
    }

    @Override
    public String toString() {
        return "InvocationParameters{" + "map=" + map + '}';
    }

    public static InvocationParameters from(String key, Object value) {
        return new InvocationParameters(Collections.singletonMap(key, value));
    }

    public static InvocationParameters from(Map<String, Object> map) {
        return new InvocationParameters(map);
    }
}
