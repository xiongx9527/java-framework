package org.jeecg.modules.jmreport.c.a;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;

/**
 * 覆盖 JmReportClassLoader，加载并解密加密类
 */
public class a extends ClassLoader {

    public a() {
        super(a.class.getClassLoader());
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name == null || !name.contains("@!@")) {
            return null;
        }
        String[] parts = name.split("@!@");
        String decodedName = new String(Base64.decodeBase64(parts[0]), StandardCharsets.UTF_8);
        String fullName = new String(Base64.decodeBase64("b3JnLmplZWNnLm1vZHVsZXMuam1yZXBvcnQu"), StandardCharsets.UTF_8)
                + decodedName;

        Class<?> loaded = findLoadedClass(fullName);
        if (loaded != null) {
            return loaded;
        }

        String templatePath = new String(Base64.decodeBase64(parts[1]), StandardCharsets.UTF_8);
        byte[] decrypted = org.jeecg.modules.jmreport.c.b.a.a(templatePath,
                org.jeecg.modules.jmreport.c.b.a.b(new String(Base64.decodeBase64(
                        "am1yZXBvcnQuZGVzcmVwb3J0LmNsc3BzX3BlbQ=="), StandardCharsets.UTF_8)));

        if (decrypted == null || decrypted.length == 0) {
            throw new ClassNotFoundException();
        }

        return defineClass(fullName, decrypted, 0, decrypted.length);
    }
}
