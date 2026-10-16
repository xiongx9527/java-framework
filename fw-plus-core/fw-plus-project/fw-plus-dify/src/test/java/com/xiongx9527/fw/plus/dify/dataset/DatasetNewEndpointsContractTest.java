package com.xiongx9527.fw.plus.dify.dataset;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.dify.DifyClientBuilder;
import com.xiongx9527.fw.plus.dify.dataset.params.ParamDocument;
import com.xiongx9527.fw.plus.dify.dataset.types.DatasetConsts;
import com.xiongx9527.fw.plus.dify.dataset.types.DocumentResult;
import com.xiongx9527.fw.plus.dify.dataset.types.IndexingStatus;
import com.xiongx9527.fw.plus.dify.dataset.types.ProcessRule;
import com.xiongx9527.fw.plus.dify.support.RecordingHttpServer;
import com.xiongx9527.fw.plus.dify.types.DifyException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

/**
 * 知识库侧新增端点（brief A3、A14-A18）的本地 HTTP 契约测试：
 * 断言请求方法、路径、query、请求体与 403/404 权限语义透传。
 * 不依赖 JUnit，由 Surefire 的 POJO 测试发现机制执行。
 */
public class DatasetNewEndpointsContractTest {

    private static final String DATASET_JSON =
            "{\"id\":\"ds-1\",\"name\":\"kb\",\"data\":[],\"has_more\":false,\"total\":0,\"page\":1,\"limit\":20}";

    public void testDatasetListAndDetailEndpoints() throws Exception {
        RecordingHttpServer server = new RecordingHttpServer(RecordingHttpServer.sequence(
                RecordingHttpServer.json(200, DATASET_JSON),
                RecordingHttpServer.json(200, "{\"id\":\"ds-1\",\"name\":\"kb\"}"),
                RecordingHttpServer.json(403, "{\"code\":\"forbidden\",\"message\":\"key scope\",\"status\":403}"),
                RecordingHttpServer.json(404, "{\"code\":\"not_found\",\"message\":\"dataset not found\",\"status\":404}")
        )).start();
        try {
            IDatasetClient client = DifyClientBuilder.dataset(server.baseUrl(), "dataset-key").build();

            // A3: GET /datasets（query：page/limit/keyword/include_all/tag_ids）
            client.listDatasets(1, 20, "java", true, Arrays.asList("t1", "t2"));
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("GET", recorded.method, "listDatasets method");
            assertEquals("/datasets", recorded.path, "GET /datasets path");
            assertEquals("1", recorded.query("page"), "query page");
            assertEquals("20", recorded.query("limit"), "query limit");
            assertEquals("java", recorded.query("keyword"), "query keyword");
            assertEquals("true", recorded.query("include_all"), "query include_all");
            assertEquals("t1,t2", recorded.query("tag_ids"), "query tag_ids（逗号分隔）");

            // A3: GET /datasets/{dataset_id} 详情
            client.get("ds-1");
            recorded = server.last();
            assertEquals("GET", recorded.method, "detail method");
            assertEquals("/datasets/ds-1", recorded.path, "GET /datasets/{id} path");

            // A3/A15: 限定范围密钥 403 与 404 透传
            try {
                client.listDatasets(1, 20, null, null, null);
                throw new AssertionError("expected DifyException(403)");
            } catch (DifyException e) {
                assertEquals(Integer.valueOf(403), e.getHttpStatus(), "list 403 status");
                assertEquals("forbidden", e.getDifyCode(), "list 403 code");
            }
            try {
                client.get("missing");
                throw new AssertionError("expected DifyException(404)");
            } catch (DifyException e) {
                assertEquals(Integer.valueOf(404), e.getHttpStatus(), "detail 404 status");
                assertEquals("not_found", e.getDifyCode(), "detail 404 code");
            }
        } finally {
            server.close();
        }
    }

    public void testDocumentDomainEndpoints() throws Exception {
        File file = tempFile("replace-doc", ".txt", "hello replace");
        RecordingHttpServer server = new RecordingHttpServer(RecordingHttpServer.sequence(
                RecordingHttpServer.json(200, "{\"id\":\"doc-1\",\"indexing_status\":\"completed\"}"),
                RecordingHttpServer.json(200, "{\"url\":\"https://files.example/signed.docx\"}"),
                RecordingHttpServer.bytes(200, "application/zip", "PKzip".getBytes(StandardCharsets.UTF_8)),
                RecordingHttpServer.json(200, "{\"result\":\"success\"}"),
                RecordingHttpServer.json(200, "{\"batch\":\"b1\",\"document\":\"{\\\"id\\\":\\\"doc-1\\\"}\"}"),
                RecordingHttpServer.json(200, "{\"batch\":\"b2\",\"document\":\"{\\\"id\\\":\\\"doc-1\\\"}\"}")
        )).start();
        try {
            IDatasetClient client = DifyClientBuilder.dataset(server.baseUrl(), "dataset-key").build();

            // A14: 文档详情（query metadata=all/only/without）
            JSONObject detail = client.getDocument("ds-1", "doc-1", "only");
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("GET", recorded.method, "getDocument method");
            assertEquals("/datasets/ds-1/documents/doc-1", recorded.path, "GET .../documents/{id} path");
            assertEquals("only", recorded.query("metadata"), "query metadata");
            assertEquals("completed", detail.getString("indexing_status"), "getDocument 响应");

            // A14: 下载文档 → 签名 URL
            String url = client.downloadDocument("ds-1", "doc-1");
            recorded = server.last();
            assertEquals("GET", recorded.method, "downloadDocument method");
            assertEquals("/datasets/ds-1/documents/doc-1/download", recorded.path, "GET .../download path");
            assertEquals("https://files.example/signed.docx", url, "签名下载 URL");

            // A14: ZIP 批量下载（body document_ids 1–100）
            byte[] zip = client.downloadDocumentsZip("ds-1", Arrays.asList("d1", "d2"));
            recorded = server.last();
            assertEquals("POST", recorded.method, "downloadZip method");
            assertEquals("/datasets/ds-1/documents/download-zip", recorded.path, "POST .../documents/download-zip path");
            List<?> ids = JSON.parseObject(recorded.body).getJSONArray("document_ids");
            assertEquals(Integer.valueOf(2), Integer.valueOf(ids.size()), "document_ids 数量");
            assertEquals("d1", ids.get(0), "document_ids[0]");
            assertTrue(zip.length > 0, "zip 字节返回");

            // A14: 批量状态更新（PATCH，action 枚举 4 值之一）
            Boolean statusUpdated = client.updateDocumentsStatus("ds-1", "enable", Arrays.asList("d1"));
            recorded = server.last();
            assertEquals("PATCH", recorded.method, "updateStatus method");
            assertEquals("/datasets/ds-1/documents/status/enable", recorded.path, "PATCH .../status/{action} path");
            assertEquals("d1", JSON.parseObject(recorded.body).getJSONArray("document_ids").get(0), "body document_ids");
            assertEquals(Boolean.TRUE, statusUpdated, "updateDocumentsStatus 返回值");

            // A14: replaceDocByFile 走 PATCH，multipart data 不含 name/text
            ParamDocument doc = ParamDocument.builder()
                    .name("doc-new")
                    .text("hello replace")
                    .docForm(DatasetConsts.DocForm.text_model)
                    .build();
            client.replaceDocByFile("ds-1", "doc-1", file, doc);
            recorded = server.last();
            assertEquals("PATCH", recorded.method, "replaceDocByFile method");
            assertEquals("/datasets/ds-1/documents/doc-1", recorded.path, "PATCH .../documents/{id} path");
            assertTrue(recorded.body.contains("name=\"file\""), "multipart 含 file 部分");
            JSONObject dataPart = extractJsonPart(recorded.body, "data");
            assertEquals(null, dataPart.getString("name"), "data 不含 name");
            assertEquals(null, dataPart.getString("text"), "data 不含 text");
            assertEquals("text_model", dataPart.getString("doc_form"), "data 含 doc_form");

            // A14: 旧 POST .../update-by-file 标记 @Deprecated 后仍可调用
            DocumentResult legacy = legacyUpdate(client, file);
            recorded = server.last();
            assertEquals("POST", recorded.method, "旧 updateDocByFile 仍为 POST");
            assertEquals("/datasets/ds-1/documents/doc-1/update-by-file", recorded.path,
                    "旧 POST .../update-by-file path 保留");
            assertEquals("b2", legacy.getBatch(), "旧方法返回 batch");

            // A14: 索引状态状态机取值齐全
            assertEquals("waiting", IndexingStatus.waiting.getValue(), "waiting");
            assertEquals("parsing", IndexingStatus.parsing.getValue(), "parsing");
            assertEquals("cleaning", IndexingStatus.cleaning.getValue(), "cleaning");
            assertEquals("splitting", IndexingStatus.splitting.getValue(), "splitting");
            assertEquals("indexing", IndexingStatus.indexing.getValue(), "indexing");
            assertEquals("completed", IndexingStatus.completed.getValue(), "completed");
            assertEquals("error", IndexingStatus.error.getValue(), "error");
        } finally {
            server.close();
        }
    }

    @SuppressWarnings("deprecation")
    private static DocumentResult legacyUpdate(IDatasetClient client, File file) {
        return client.updateDocByFile("ds-1", "doc-1", file, ProcessRule.builder()
                .mode(ProcessRule.Mode.automatic)
                .build());
    }

    public void testSegmentDetailAndPermissionSemantics() throws Exception {
        RecordingHttpServer server = new RecordingHttpServer(RecordingHttpServer.sequence(
                RecordingHttpServer.json(200, "{\"data\":{\"id\":\"seg-1\",\"content\":\"c\"},\"doc_form\":\"text-model\"}"),
                RecordingHttpServer.json(403, "{\"code\":\"forbidden\",\"message\":\"key scope\",\"status\":403}"),
                RecordingHttpServer.json(404, "{\"code\":\"not_found\",\"message\":\"segment not found\",\"status\":404}")
        )).start();
        try {
            IDatasetClient client = DifyClientBuilder.dataset(server.baseUrl(), "dataset-key").build();

            // A15: GET .../segments/{segment_id}，响应 data + doc_form
            JSONObject segment = client.getSegment("ds-1", "doc-1", "seg-1");
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("GET", recorded.method, "getSegment method");
            assertEquals("/datasets/ds-1/documents/doc-1/segments/seg-1", recorded.path,
                    "GET .../segments/{segment_id} path");
            assertEquals("seg-1", segment.getJSONObject("data").getString("id"), "响应 data");
            assertEquals("text-model", segment.getString("doc_form"), "响应 doc_form");

            // A15: 403（密钥范围）与 404 透传，不折叠
            try {
                client.getSegment("ds-1", "doc-1", "seg-1");
                throw new AssertionError("expected DifyException(403)");
            } catch (DifyException e) {
                assertEquals(Integer.valueOf(403), e.getHttpStatus(), "403 status");
                assertEquals("forbidden", e.getDifyCode(), "403 code");
            }
            try {
                client.getSegment("ds-1", "doc-1", "seg-1");
                throw new AssertionError("expected DifyException(404)");
            } catch (DifyException e) {
                assertEquals(Integer.valueOf(404), e.getHttpStatus(), "404 status");
                assertEquals("not_found", e.getDifyCode(), "404 code");
            }
        } finally {
            server.close();
        }
    }

    public void testMetadataDomainEndpoints() throws Exception {
        RecordingHttpServer server = new RecordingHttpServer(RecordingHttpServer.sequence(
                RecordingHttpServer.json(201, "{\"id\":\"m1\",\"name\":\"source\",\"type\":\"string\"}"),
                RecordingHttpServer.json(200, "{\"built_in_field_enabled\":true,\"doc_metadata\":[]}"),
                RecordingHttpServer.json(200, "{\"id\":\"m1\",\"name\":\"src\"}"),
                RecordingHttpServer.json(200, "{\"result\":\"success\"}"),
                RecordingHttpServer.json(200, "{\"fields\":[{\"name\":\"document_name\"}]}"),
                RecordingHttpServer.json(200, "{\"result\":\"success\"}"),
                RecordingHttpServer.json(200, "{\"result\":\"success\"}")
        )).start();
        try {
            IDatasetClient client = DifyClientBuilder.dataset(server.baseUrl(), "dataset-key").build();

            // A16-1: 创建元数据字段
            JSONObject created = client.createMetadataField("ds-1", "source", "string");
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("POST", recorded.method, "createMetadataField method");
            assertEquals("/datasets/ds-1/metadata", recorded.path, "POST /datasets/{id}/metadata path");
            JSONObject body = JSON.parseObject(recorded.body);
            assertEquals("source", body.getString("name"), "body name");
            assertEquals("string", body.getString("type"), "body type");
            assertEquals("m1", created.getString("id"), "create 响应");

            // A16-2: 字段列表
            client.listMetadataFields("ds-1");
            recorded = server.last();
            assertEquals("GET", recorded.method, "listMetadataFields method");
            assertEquals("/datasets/ds-1/metadata", recorded.path, "GET /datasets/{id}/metadata path");

            // A16-3: 重命名字段
            client.updateMetadataField("ds-1", "m1", "src");
            recorded = server.last();
            assertEquals("PATCH", recorded.method, "updateMetadataField method");
            assertEquals("/datasets/ds-1/metadata/m1", recorded.path, "PATCH /datasets/{id}/metadata/{metadata_id} path");
            assertEquals("src", JSON.parseObject(recorded.body).getString("name"), "body name");

            // A16-4: 删除字段
            Boolean deleted = client.deleteMetadataField("ds-1", "m1");
            recorded = server.last();
            assertEquals("DELETE", recorded.method, "deleteMetadataField method");
            assertEquals("/datasets/ds-1/metadata/m1", recorded.path, "DELETE path");
            assertEquals(Boolean.TRUE, deleted, "delete 返回值");

            // A16-5: 内置字段列表
            client.getBuiltInMetadataFields("ds-1");
            recorded = server.last();
            assertEquals("GET", recorded.method, "getBuiltInMetadataFields method");
            assertEquals("/datasets/ds-1/metadata/built-in", recorded.path, "GET .../metadata/built-in path");

            // A16-6: 内置字段 enable/disable
            client.toggleBuiltInMetadataField("ds-1", "enable");
            recorded = server.last();
            assertEquals("POST", recorded.method, "toggleBuiltInMetadataField method");
            assertEquals("/datasets/ds-1/metadata/built-in/enable", recorded.path, "POST .../built-in/{action} path");

            // A16-7: 批量更新文档元数据
            Map<String, Object> operation = new HashMap<String, Object>();
            operation.put("document_id", "doc-1");
            List<Map<String, Object>> operations = new ArrayList<Map<String, Object>>();
            operations.add(operation);
            client.batchUpdateDocumentMetadata("ds-1", operations, Boolean.TRUE);
            recorded = server.last();
            assertEquals("POST", recorded.method, "batchUpdateDocumentMetadata method");
            assertEquals("/datasets/ds-1/documents/metadata", recorded.path, "POST .../documents/metadata path");
            JSONObject batchBody = JSON.parseObject(recorded.body);
            assertEquals(Boolean.TRUE, batchBody.getBoolean("partial_update"), "body partial_update");
            assertEquals(Integer.valueOf(1), Integer.valueOf(batchBody.getJSONArray("operation_data").size()),
                    "body operation_data 数量");
        } finally {
            server.close();
        }
    }

    public void testTagDomainAndModelQueryEndpoints() throws Exception {
        RecordingHttpServer server = new RecordingHttpServer(RecordingHttpServer.sequence(
                RecordingHttpServer.json(201, "{\"id\":\"t1\",\"name\":\"tag1\"}"),
                RecordingHttpServer.json(200, "{\"data\":[]}"),
                RecordingHttpServer.json(200, "{\"id\":\"t1\",\"name\":\"tag1-new\"}"),
                RecordingHttpServer.json(200, "{\"result\":\"success\"}"),
                RecordingHttpServer.json(200, "{\"result\":\"success\"}"),
                RecordingHttpServer.json(200, "{\"result\":\"success\"}"),
                RecordingHttpServer.json(200, "{\"data\":[{\"id\":\"t1\",\"name\":\"tag1\"}],\"total\":1}"),
                RecordingHttpServer.json(200, "{\"data\":[]}")
        )).start();
        try {
            IDatasetClient client = DifyClientBuilder.dataset(server.baseUrl(), "dataset-key").build();

            // A17-1: 创建标签
            client.createTag("tag1");
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("POST", recorded.method, "createTag method");
            assertEquals("/datasets/tags", recorded.path, "POST /datasets/tags path");
            assertEquals("tag1", JSON.parseObject(recorded.body).getString("name"), "body name");

            // A17-2: 标签列表
            client.listTags();
            recorded = server.last();
            assertEquals("GET", recorded.method, "listTags method");
            assertEquals("/datasets/tags", recorded.path, "GET /datasets/tags path");

            // A17-3: 更新标签
            client.updateTag("t1", "tag1-new");
            recorded = server.last();
            assertEquals("PATCH", recorded.method, "updateTag method");
            assertEquals("/datasets/tags", recorded.path, "PATCH /datasets/tags path");
            JSONObject body = JSON.parseObject(recorded.body);
            assertEquals("t1", body.getString("tag_id"), "body tag_id");
            assertEquals("tag1-new", body.getString("name"), "body name");

            // A17-4: 删除标签
            Boolean deleted = client.deleteTag("t1");
            recorded = server.last();
            assertEquals("DELETE", recorded.method, "deleteTag method");
            assertEquals("/datasets/tags", recorded.path, "DELETE /datasets/tags path");
            assertEquals(Boolean.TRUE, deleted, "delete 返回值");

            // A17-5: 绑定标签
            Boolean bound = client.bindTags("ds-1", Arrays.asList("t1"));
            recorded = server.last();
            assertEquals("POST", recorded.method, "bindTags method");
            assertEquals("/datasets/tags/binding", recorded.path, "POST /datasets/tags/binding path");
            body = JSON.parseObject(recorded.body);
            assertEquals("ds-1", body.getString("target_id"), "binding body target_id");
            assertEquals(Integer.valueOf(1), Integer.valueOf(body.getJSONArray("tag_ids").size()),
                    "binding body tag_ids");
            assertEquals(Boolean.TRUE, bound, "bind 返回值");

            // A17-6: 解绑标签（仅暴露 tag_ids 数组，无 tag_id 单数废弃别名）
            client.unbindTags("ds-1", Arrays.asList("t1"));
            recorded = server.last();
            assertEquals("POST", recorded.method, "unbindTags method");
            assertEquals("/datasets/tags/unbinding", recorded.path, "POST /datasets/tags/unbinding path");
            body = JSON.parseObject(recorded.body);
            assertEquals("ds-1", body.getString("target_id"), "unbinding body target_id");
            assertEquals(Integer.valueOf(1), Integer.valueOf(body.getJSONArray("tag_ids").size()),
                    "unbinding body tag_ids");
            assertEquals(null, body.get("tag_id"), "unbinding 不暴露 tag_id 单数字段");

            // A17-7: 知识库已绑标签
            JSONObject datasetTags = client.getDatasetTags("ds-1");
            recorded = server.last();
            assertEquals("GET", recorded.method, "getDatasetTags method");
            assertEquals("/datasets/ds-1/tags", recorded.path, "GET /datasets/{id}/tags path");
            assertEquals(Integer.valueOf(1), datasetTags.getInteger("total"), "tags total");

            // A17: 可用模型查询（model_type 路径参数）
            client.getAvailableModels("llm");
            recorded = server.last();
            assertEquals("GET", recorded.method, "getAvailableModels method");
            assertEquals("/workspaces/current/models/model-types/llm", recorded.path,
                    "GET /workspaces/current/models/model-types/{model_type} path");
        } finally {
            server.close();
        }
    }

    public void testPipelineEndpoints() throws Exception {
        final CountDownLatch nodeLatch = new CountDownLatch(1);
        final CountDownLatch streamLatch = new CountDownLatch(1);
        final List<String> nodeBodies = new ArrayList<String>();
        final List<String> streamBodies = new ArrayList<String>();

        RecordingHttpServer server = new RecordingHttpServer(RecordingHttpServer.sequence(
                RecordingHttpServer.json(201, "{\"id\":\"f1\"}"),
                RecordingHttpServer.json(200, "{\"data\":[]}"),
                new RecordingHttpServer.Responder() {
                    @Override
                    public void respond(RecordingHttpServer.Recorded request,
                                        com.sun.net.httpserver.HttpExchange exchange) throws IOException {
                        nodeBodies.add(request.body);
                        writeSse(exchange, "event: node_run\ndata: {\"node_id\":\"n1\",\"status\":\"succeeded\"}\n\n");
                    }
                },
                RecordingHttpServer.json(200,
                        "{\"workflow_run_id\":\"wr-1\",\"task_id\":\"tk-1\",\"data\":{\"status\":\"succeeded\"}}")
        )).start();
        try {
            IDatasetClient client = DifyClientBuilder.dataset(server.baseUrl(), "dataset-key").build();

            // A18-1: 流水线文件上传（路径不含 dataset_id，需全量密钥）
            File file = tempFile("pipeline-src", ".csv", "a,b\n1,2\n");
            JSONObject uploaded = client.uploadPipelineFile(file);
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("POST", recorded.method, "uploadPipelineFile method");
            assertEquals("/datasets/pipeline/file-upload", recorded.path, "POST /datasets/pipeline/file-upload path");
            assertTrue(recorded.body.contains("name=\"file\""), "multipart 含 file 部分");
            assertEquals("f1", uploaded.getString("id"), "upload 响应 id");

            // A18-2: 数据源插件列表
            client.listDatasourcePlugins("ds-1", Boolean.TRUE);
            recorded = server.last();
            assertEquals("GET", recorded.method, "listDatasourcePlugins method");
            assertEquals("/datasets/ds-1/pipeline/datasource-plugins", recorded.path,
                    "GET .../pipeline/datasource-plugins path");
            assertEquals("true", recorded.query("is_published"), "query is_published");

            // A18-3: 数据源节点执行（SSE）
            final List<String> nodeEvents = new ArrayList<String>();
            client.runDatasourceNode("ds-1", "n1", new HashMap<String, Object>(),
                    "local_file", true, null, new EventSourceListener() {
                        @Override
                        public void onEvent(EventSource eventSource, String id, String type, String data) {
                            nodeEvents.add(type);
                            nodeLatch.countDown();
                        }

                        @Override
                        public void onFailure(EventSource eventSource, Throwable t, Response response) {
                            nodeLatch.countDown();
                        }
                    });
            assertTrue(nodeLatch.await(15, TimeUnit.SECONDS), "节点 SSE 事件到达");
            recorded = server.last();
            assertEquals("POST", recorded.method, "runDatasourceNode method");
            assertEquals("/datasets/ds-1/pipeline/datasource/nodes/n1/run", recorded.path,
                    "POST .../pipeline/datasource/nodes/{node_id}/run path");
            assertTrue(nodeEvents.contains("node_run"), "节点 SSE 事件类型");
            JSONObject nodeBody = JSON.parseObject(nodeBodies.get(0));
            assertEquals("local_file", nodeBody.getString("datasource_type"), "body datasource_type");
            assertEquals(Boolean.TRUE, nodeBody.getBoolean("is_published"), "body is_published");
            assertTrue(nodeBody.containsKey("inputs"), "body 含 inputs");

            // A18-4a: 流水线运行（blocking，response_mode 强制 blocking）
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("inputs", new HashMap<String, Object>());
            params.put("datasource_type", "local_file");
            JSONObject blocking = client.runPipelineBlocking("ds-1", params);
            recorded = server.last();
            assertEquals("POST", recorded.method, "runPipelineBlocking method");
            assertEquals("/datasets/ds-1/pipeline/run", recorded.path, "POST .../pipeline/run path");
            JSONObject blockingBody = JSON.parseObject(recorded.body);
            assertEquals("blocking", blockingBody.getString("response_mode"), "blocking response_mode 强制");
            assertEquals("wr-1", blocking.getString("workflow_run_id"), "blocking 返回值");

            // A18-4b: 流水线运行（streaming，response_mode 强制 streaming）
            server.respondWith(new RecordingHttpServer.Responder() {
                @Override
                public void respond(RecordingHttpServer.Recorded request,
                                    com.sun.net.httpserver.HttpExchange exchange) throws IOException {
                    streamBodies.add(request.body);
                    writeSse(exchange, "event: workflow_started\ndata: {\"workflow_run_id\":\"wr-1\"}\n\n");
                }
            });
            final List<String> streamEvents = new ArrayList<String>();
            client.runPipelineStreaming("ds-1", params, new EventSourceListener() {
                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    streamEvents.add(type);
                    streamLatch.countDown();
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    streamLatch.countDown();
                }
            });
            assertTrue(streamLatch.await(15, TimeUnit.SECONDS), "流水线 SSE 事件到达");
            recorded = server.last();
            assertEquals("POST", recorded.method, "runPipelineStreaming method");
            assertEquals("/datasets/ds-1/pipeline/run", recorded.path, "streaming path");
            assertEquals("streaming", JSON.parseObject(streamBodies.get(0)).getString("response_mode"),
                    "streaming response_mode 强制");
            assertTrue(streamEvents.contains("workflow_started"), "流水线 SSE 事件类型");
        } finally {
            server.close();
        }
    }

    private static void writeSse(com.sun.net.httpserver.HttpExchange exchange, String payload) throws IOException {
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);
        OutputStream out = exchange.getResponseBody();
        out.write(data);
        out.flush();
        out.close();
    }

    /**
     * 从 multipart 请求体中提取指定字段的 JSON 字符串部分并解析
     */
    private static JSONObject extractJsonPart(String body, String fieldName) {
        String marker = "name=\"" + fieldName + "\"";
        int markerIdx = body.indexOf(marker);
        if (markerIdx < 0) {
            throw new AssertionError("multipart 缺少字段 " + fieldName);
        }
        int headerEnd = body.indexOf("\r\n\r\n", markerIdx);
        if (headerEnd < 0) {
            throw new AssertionError("multipart 字段 " + fieldName + " 缺少内容分隔");
        }
        int contentStart = headerEnd + 4;
        int contentEnd = body.indexOf("\r\n", contentStart);
        if (contentEnd < 0) {
            throw new AssertionError("multipart 字段 " + fieldName + " 缺少结束标记");
        }
        return JSON.parseObject(body.substring(contentStart, contentEnd));
    }

    private static File tempFile(String prefix, String suffix, String content) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        file.deleteOnExit();
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        } finally {
            out.close();
        }
        return file;
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " expected:<" + expected + "> but was:<" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
