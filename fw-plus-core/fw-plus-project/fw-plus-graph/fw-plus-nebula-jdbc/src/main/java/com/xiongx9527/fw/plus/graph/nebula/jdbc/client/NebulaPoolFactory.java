package com.xiongx9527.fw.plus.graph.nebula.jdbc.client;

import java.sql.SQLException;

public class NebulaPoolFactory {
    private static final String NEBULA_JAVA_3_POOL = "com.vesoft.nebula.client.graph.net.NebulaPool";
    private static final String NEBULA_JAVA_5_POOL = "com.vesoft.nebula.driver.graph.net.NebulaPool";

    public NebulaNativeClient create() throws SQLException {
        boolean hasNebulaJava3 = hasClass(NEBULA_JAVA_3_POOL);
        boolean hasNebulaJava5 = hasClass(NEBULA_JAVA_5_POOL);
        if (hasNebulaJava3 && hasNebulaJava5) {
            throw new SQLException("com.vesoft:client and com.vesoft:driver must not appear on the same classpath.");
        }
        if (hasNebulaJava5) {
            return new NebulaJava5NativeClient();
        }
        if (hasNebulaJava3) {
            return new NebulaJava3NativeClient();
        }
        throw new SQLException("No supported Nebula Java SDK found. Add either com.vesoft:client or com.vesoft:driver.");
    }

    boolean hasClass(String className) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
