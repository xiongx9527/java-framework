package com.xiongx9527.fw.plus.ai.trace.exporter;

/** 把 epoch 毫秒转成 ISO-8601 即时时间字符串（如 2026-01-01T00:00:00.123Z）。 */
public final class Iso8601 {
    private Iso8601() {}
    public static String fromEpochMillis(long millis) {
        if (millis <= 0) millis = System.currentTimeMillis();
        return java.time.Instant.ofEpochMilli(millis).toString();
    }
}
