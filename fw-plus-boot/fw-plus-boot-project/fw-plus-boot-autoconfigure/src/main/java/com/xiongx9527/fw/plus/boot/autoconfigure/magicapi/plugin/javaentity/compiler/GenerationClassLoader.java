package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler;

import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GenerationClassLoader extends ClassLoader implements Closeable {

    private final Map<String, byte[]> byteCodes;

    GenerationClassLoader(ClassLoader parent, Map<String, byte[]> byteCodes) {
        super(parent);
        this.byteCodes = new LinkedHashMap<>(byteCodes);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> loaded = findLoadedClass(name);
        if (loaded == null && byteCodes.containsKey(name)) {
            loaded = findClass(name);
        }
        if (loaded == null) {
            loaded = super.loadClass(name, false);
        }
        if (resolve) {
            resolveClass(loaded);
        }
        return loaded;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] byteCode = byteCodes.get(name);
        if (byteCode == null) {
            throw new ClassNotFoundException(name);
        }
        return defineClass(name, byteCode, 0, byteCode.length);
    }

    @Override
    public void close() {
        byteCodes.clear();
    }
}
