package com.xiongx9527.fw.plus.ai.trace.exporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 测试用 HttpSender 桩：记录每次发送，便于断言报文，不发起真实网络。 */
public final class CapturingHttpSender implements HttpSender {
    public static final class Record {
        public final String url; public final Map<String, String> headers; public final String body;
        Record(String url, Map<String, String> headers, String body) { this.url = url; this.headers = headers; this.body = body; }
    }
    private final List<Record> records = new ArrayList<Record>();
    @Override
    public void post(String url, Map<String, String> headers, String body) throws IOException {
        records.add(new Record(url, headers, body));
    }
    public List<Record> records() { return records; }
    public Record last() { return records.isEmpty() ? null : records.get(records.size() - 1); }
}
