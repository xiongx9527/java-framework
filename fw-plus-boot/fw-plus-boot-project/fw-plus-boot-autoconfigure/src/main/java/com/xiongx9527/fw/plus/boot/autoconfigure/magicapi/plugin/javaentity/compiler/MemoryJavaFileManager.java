package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class MemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    private final Map<String, ByteCodeFileObject> outputs = new LinkedHashMap<>();

    MemoryJavaFileManager(JavaFileManager fileManager) {
        super(fileManager);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                JavaFileObject.Kind kind, FileObject sibling) {
        ByteCodeFileObject output = new ByteCodeFileObject(className, kind);
        outputs.put(className, output);
        return output;
    }

    Map<String, byte[]> getByteCodes() {
        Map<String, byte[]> byteCodes = new LinkedHashMap<>();
        for (Map.Entry<String, ByteCodeFileObject> entry : outputs.entrySet()) {
            byteCodes.put(entry.getKey(), entry.getValue().toByteArray());
        }
        return Collections.unmodifiableMap(byteCodes);
    }

    private static final class ByteCodeFileObject extends SimpleJavaFileObject {

        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        private ByteCodeFileObject(String className, Kind kind) {
            super(URI.create("bytes:///" + className.replace('.', '/') + kind.extension), kind);
        }

        @Override
        public OutputStream openOutputStream() {
            return output;
        }

        private byte[] toByteArray() {
            return output.toByteArray();
        }
    }
}
