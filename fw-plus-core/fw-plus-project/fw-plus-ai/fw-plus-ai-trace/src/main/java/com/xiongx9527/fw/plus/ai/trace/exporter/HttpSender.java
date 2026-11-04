package com.xiongx9527.fw.plus.ai.trace.exporter;

import java.io.IOException;
import java.util.Map;

/** 最小化的出站 HTTP 客户端契约：便于在测试中替换为捕获桩，避免真实网络。 */
public interface HttpSender {
    void post(String url, Map<String, String> headers, String body) throws IOException;
}
