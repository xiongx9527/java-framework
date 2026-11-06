package com.xiongx9527.fw.plus.ai.nativeimpl.memory;

import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryAddRequest;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryScope;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemorySearchRequest;
import com.xiongx9527.fw.plus.ai.base.memory.MemoryRecord;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.nativeimpl.support.FakeHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Mem0ServerConversationMemoryPortTest {

    private FakeHttpServer server;
    private Mem0ServerConversationMemoryPort port;

    @BeforeEach
    void setUp() throws Exception {
        server = new FakeHttpServer(request -> {
            String path = request.path;
            if (path.startsWith("/memories/")) {
                // DELETE /memories/{id}
                return FakeHttpServer.json(200, "{}");
            }
            if (path.startsWith("/memories")) {
                // GET /memories?user_id=... 含查询串，用前缀匹配；POST 为写入
                if ("POST".equals(request.method)) {
                    return FakeHttpServer.json(200, FakeHttpServer.body(
                            "{'results':[{'id':'m1','memory':'用户喜欢苹果','event':'ADD','created_at':'2026-01-01'}]}"));
                }
                return FakeHttpServer.json(200, FakeHttpServer.body(
                        "{'results':[{'id':'m1','memory':'用户喜欢苹果','event':'NONE'}]}"));
            }
            if ("/search".equals(path)) {
                return FakeHttpServer.json(200, FakeHttpServer.body(
                        "{'results':[{'id':'m1','memory':'用户喜欢苹果','score':0.91}]}"));
            }
            return FakeHttpServer.json(404, "{}");
        });
        port = new Mem0ServerConversationMemoryPort(server.url(), "test-key");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.close();
    }

    @Test
    void addParsesResultsFromServer() {
        List<MemoryRecord> records = port.add(ConversationMemoryAddRequest.builder()
                .message(ChatMessage.user("我喜欢吃苹果")).userId("u1").build());
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getId()).isEqualTo("m1");
        assertThat(records.get(0).getMemory()).isEqualTo("用户喜欢苹果");
        assertThat(server.lastRequest().header("X-API-Key")).isEqualTo("test-key");
    }

    @Test
    void searchParsesScoredResultsFromServer() {
        List<MemoryRecord> hits = port.search(ConversationMemorySearchRequest.builder()
                .query("苹果").userId("u1").build());
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getScore()).isEqualTo(0.91);
    }

    @Test
    void listParsesResultsFromServer() {
        List<MemoryRecord> list = port.list(ConversationMemoryScope.builder().userId("u1").build());
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo("m1");
    }

    @Test
    void deleteSendsDeleteRequest() {
        port.delete("m1");
        FakeHttpServer.Request req = server.lastRequest();
        assertThat(req.method).isEqualTo("DELETE");
        assertThat(req.path).isEqualTo("/memories/m1");
    }
}
