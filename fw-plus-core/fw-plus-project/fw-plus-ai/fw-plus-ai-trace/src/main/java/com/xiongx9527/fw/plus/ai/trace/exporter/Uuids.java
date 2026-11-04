package com.xiongx9527.fw.plus.ai.trace.exporter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** 观测后端（尤其 LangSmith）要求 id 为合法 UUID；把任意字符串规整成稳定 UUID。 */
public final class Uuids {
    private Uuids() {}
    public static String normalize(String id) {
        if (id == null) return UUID.randomUUID().toString();
        try { UUID.fromString(id); return id; }
        catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)).toString();
        }
    }
}
