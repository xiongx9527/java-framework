package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.CompiledGeneration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceUnit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DynamicEntityGeneration {

    private final long version;
    private final Map<String, Class<?>> classes;
    private final Map<String, String> resourceIds;
    private final Map<String, String> sourceDigests;
    private final Map<String, String> aliases;

    private DynamicEntityGeneration(long version, Map<String, Class<?>> classes,
                                    Map<String, String> resourceIds, Map<String, String> sourceDigests,
                                    Map<String, String> aliases) {
        this.version = version;
        this.classes = Collections.unmodifiableMap(new LinkedHashMap<>(classes));
        this.resourceIds = Collections.unmodifiableMap(new LinkedHashMap<>(resourceIds));
        this.sourceDigests = Collections.unmodifiableMap(new LinkedHashMap<>(sourceDigests));
        this.aliases = Collections.unmodifiableMap(new LinkedHashMap<>(aliases));
    }

    static DynamicEntityGeneration empty() {
        return new DynamicEntityGeneration(0L, Collections.<String, Class<?>>emptyMap(),
                Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap(),
                Collections.<String, String>emptyMap());
    }

    static DynamicEntityGeneration from(CompiledGeneration generation, List<JavaSourceUnit> units,
                                        Map<String, String> aliases) {
        Map<String, String> resourceIds = new LinkedHashMap<>();
        Map<String, String> sourceDigests = new LinkedHashMap<>();
        for (JavaSourceUnit unit : new ArrayList<>(units)) {
            resourceIds.put(unit.getClassName(), unit.getResourceId());
            sourceDigests.put(unit.getClassName(), digest(unit.getSource()));
        }
        return new DynamicEntityGeneration(generation.getVersion(), generation.getClasses(),
                resourceIds, sourceDigests, aliases);
    }

    long getVersion() {
        return version;
    }

    DynamicEntityDescriptor resolve(String name) {
        String className = classes.containsKey(name) ? name : aliases.get(name);
        Class<?> type = className == null ? null : classes.get(className);
        if (type == null) {
            throw new IllegalArgumentException("动态实体不存在: " + name);
        }
        return new DynamicEntityDescriptor(version, resourceIds.get(className), sourceDigests.get(className), type);
    }

    private static String digest(String source) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }
}
