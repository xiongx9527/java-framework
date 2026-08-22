package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler;

import java.io.Closeable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CompiledGeneration implements Closeable {

    private final long version;
    private final GenerationClassLoader classLoader;
    private final Map<String, Class<?>> classes;

    CompiledGeneration(long version, GenerationClassLoader classLoader, Map<String, Class<?>> classes) {
        this.version = version;
        this.classLoader = classLoader;
        this.classes = Collections.unmodifiableMap(new LinkedHashMap<>(classes));
    }

    public long getVersion() {
        return version;
    }

    public GenerationClassLoader getClassLoader() {
        return classLoader;
    }

    public Map<String, Class<?>> getClasses() {
        return classes;
    }

    @Override
    public void close() {
        classLoader.close();
    }
}
