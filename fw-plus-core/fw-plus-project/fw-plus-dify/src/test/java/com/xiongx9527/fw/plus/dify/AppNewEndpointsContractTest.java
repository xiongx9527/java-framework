package com.xiongx9527.fw.plus.dify;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.dify.app.IAppBaseClient;
import com.xiongx9527.fw.plus.dify.app.IAppChatClient;
import com.xiongx9527.fw.plus.dify.app.IAppFlowClient;
import com.xiongx9527.fw.plus.dify.app.params.ParamMessage;
import com.xiongx9527.fw.plus.dify.app.types.DifyChatEvent;
import com.xiongx9527.fw.plus.dify.app.types.WorkflowStatus;
import com.xiongx9527.fw.plus.dify.support.RecordingHttpServer;
import com.xiongx9527.fw.plus.dify.types.ApiConfig;
import com.xiongx9527.fw.plus.dify.types.DifyException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

/**
 * App 侧新增/对齐端点（brief A1、A2、A4-A13）的本地 HTTP 契约测试：
 * 断言请求方法、路径、query、Authorization 头、请求体与错误码透传均与官方文档一致。
 * 不依赖 JUnit，由 Surefire 的 POJO 测试发现机制执行。
 */
public class AppNewEndpointsContractTest {

    public void testApiConfigDefaultAndCustomBaseUrl() {
        ApiConfig defaults = ApiConfig.builder().apiKey("app-key").build();
        assertEquals("https://api.dify.ai/v1", defaults.getServer(), "ApiConfig 默认基址");

        ApiConfig custom = ApiConfig.builder()
                .server("https://dify.internal.example/v1")
                .apiKey("app-key")
                .build();
        assertEquals("https://dify.internal.example/v1", custom.getServer(), "自定义自部署基址");
    }

    public void testAppConfigEndpointsCarryBearerAuth() throws Exception {
        RecordingHttpServer server = new RecordingHttpServer(RecordingHttpServer.json(200, "{\"name\":\"demo\"}")).start();
        try {
            IAppBaseClient base = DifyClientBuilder.app(server.baseUrl(), "app-key").base().build();

            base.getAppInfo();
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("GET", recorded.method, "GET /info method");
            assertEquals("/info", recorded.path, "GET /info path");
            assertEquals("Bearer app-key", recorded.authorization, "Bearer 认证头");

            base.getAppParameters();
            assertEquals("/parameters", server.last().path, "GET /parameters path");

            base.getAppMetaInfo();
            assertEquals("/meta", server.last().path, "GET /meta path");

            base.getAppWebAppSettings();
            assertEquals("GET", server.last().method, "GET /site method");
            assertEquals("/site", server.last().path, "GET /site path");
        } finally {
            server.close();
        }
    }

    public void testSiteEndpointForbiddenPassesThrough() throws Exception {
        RecordingHttpServer server = new RecordingHttpServer(
                RecordingHttpServer.json(403, "{\"code\":\"forbidden\",\"message\":\"site not found\",\"status\":403}")
        ).start();
        try {
            IAppBaseClient base = DifyClientBuilder.app(server.baseUrl(), "app-key").base().build();
            try {
                base.getAppWebAppSettings();
                throw new AssertionError("expected DifyException");
            } catch (DifyException e) {
                assertEquals(Integer.valueOf(403), e.getHttpStatus(), "403 http status");
                assertEquals("forbidden", e.getDifyCode(), "403 code 透传");
            }
        } finally {
            server.close();
        }
    }

    public void testEndUserEndpointAndNotFoundPassesThrough() throws Exception {
        RecordingHttpServer server = new RecordingHttpServer(
                RecordingHttpServer.json(200, "{\"id\":\"eu-1\",\"type\":\"end-user\"}")
        ).start();
        try {
            IAppBaseClient base = DifyClientBuilder.app(server.baseUrl(), "app-key").base().build();

            String result = base.getEndUser("eu-1");
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("GET", recorded.method, "GET /end-users method");
            assertEquals("/end-users/eu-1", recorded.path, "GET /end-users/{id} path");
            assertEquals("eu-1", JSON.parseObject(result).getString("id"), "end user 响应");

            server.respondWith(RecordingHttpServer.json(404,
                    "{\"code\":\"end_user_not_found\",\"message\":\"End user not found\",\"status\":404}"));
            try {
                base.getEndUser("missing");
                throw new AssertionError("expected DifyException");
            } catch (DifyException e) {
                assertEquals(Integer.valueOf(404), e.getHttpStatus(), "404 http status");
                assertEquals("end_user_not_found", e.getDifyCode(), "404 code 透传");
            }
        } finally {
            server.close();
        }
    }

    public void testFilePreviewDownloadCarriesUserAndAttachmentFlag() throws Exception {
        final byte[] payload = "file-bytes".getBytes(StandardCharsets.UTF_8);
        RecordingHttpServer server = new RecordingHttpServer(RecordingHttpServer.sequence(
                RecordingHttpServer.bytes(200, "application/octet-stream", payload),
                RecordingHttpServer.bytes(200, "application/octet-stream", payload),
                RecordingHttpServer.json(403, "{\"code\":\"file_access_denied\",\"message\":\"denied\",\"status\":403}"),
                RecordingHttpServer.json(404, "{\"code\":\"file_not_found\",\"message\":\"not found\",\"status\":404}")
        )).start();
        try {
            IAppBaseClient base = DifyClientBuilder.app(server.baseUrl(), "app-key").base().build();

            byte[] data = base.downloadFile("file-1", "u1", true);
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("GET", recorded.method, "GET preview method");
            assertEquals("/files/file-1/preview", recorded.path, "GET /files/{id}/preview path");
            assertEquals("u1", recorded.query("user"), "query user");
            assertEquals("true", recorded.query("as_attachment"), "query as_attachment");
            assertEquals(new String(payload, StandardCharsets.UTF_8), new String(data, StandardCharsets.UTF_8), "文件字节");

            base.downloadFile("file-1", false);
            recorded = server.last();
            assertEquals(null, recorded.query("user"), "无 user 时不携带");
            assertEquals(null, recorded.query("as_attachment"), "as_attachment=false 不携带");

            // A8: 403 file_access_denied 与 404 file_not_found 透传
            try {
                base.downloadFile("file-1", "u1", false);
                throw new AssertionError("expected DifyException(403)");
            } catch (DifyException e) {
                assertEquals(Integer.valueOf(403), e.getHttpStatus(), "403 status");
                assertEquals("file_access_denied", e.getDifyCode(), "403 code=file_access_denied");
            }
            try {
                base.downloadFile("file-1", "u1", false);
                throw new AssertionError("expected DifyException(404)");
            } catch (DifyException e) {
                assertEquals(Integer.valueOf(404), e.getHttpStatus(), "404 status");
                assertEquals("file_not_found", e.getDifyCode(), "404 code=file_not_found");
            }
        } finally {
            server.close();
        }
    }

    public void testAppFeedbackListEndpoint() throws Exception {
        RecordingHttpServer server = new RecordingHttpServer(
                RecordingHttpServer.json(200, "{\"data\":[]}")
        ).start();
        try {
            IAppBaseClient base = DifyClientBuilder.app(server.baseUrl(), "app-key").base().build();

            base.queryFeedbacks(2, 50);
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("GET", recorded.method, "GET /app/feedbacks method");
            assertEquals("/app/feedbacks", recorded.path, "GET /app/feedbacks path");
            assertEquals("2", recorded.query("page"), "query page");
            assertEquals("50", recorded.query("limit"), "query limit");
        } finally {
            server.close();
        }
    }

    public void testConversationVariableEndpoints() throws Exception {
        RecordingHttpServer server = new RecordingHttpServer(RecordingHttpServer.sequence(
                RecordingHttpServer.json(200, "{\"data\":[],\"has_more\":false,\"limit\":20}"),
                RecordingHttpServer.json(200, "{}"),
                RecordingHttpServer.json(400, "{\"code\":\"bad_request\",\"message\":\"value type mismatch\",\"status\":400}"),
                RecordingHttpServer.json(404, "{\"code\":\"conversation_variable_not_found\",\"message\":\"not found\",\"status\":404}")
        )).start();
        try {
            IAppChatClient chat = DifyClientBuilder.app(server.baseUrl(), "app-key").chat().build();

            chat.listConversationVariables("conv-1", "u1", 10, "last-1", "flag");
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("GET", recorded.method, "GET variables method");
            assertEquals("/conversations/conv-1/variables", recorded.path, "GET /conversations/{id}/variables path");
            assertEquals("u1", recorded.query("user"), "query user");
            assertEquals("10", recorded.query("limit"), "query limit");
            assertEquals("last-1", recorded.query("last_id"), "query last_id");
            assertEquals("flag", recorded.query("variable_name"), "query variable_name");

            chat.updateConversationVariable("conv-1", "var-1", "dark_mode", "u1");
            recorded = server.last();
            assertEquals("PUT", recorded.method, "PUT variables/{id} method");
            assertEquals("/conversations/conv-1/variables/var-1", recorded.path, "PUT path");
            JSONObject body = JSON.parseObject(recorded.body);
            assertEquals("dark_mode", body.getString("value"), "body value 必填");
            assertEquals("u1", body.getString("user"), "body user");

            // A7: 400 bad_request（类型不匹配）透传
            try {
                chat.updateConversationVariable("conv-1", "var-1", "dark_mode", "u1");
                throw new AssertionError("expected DifyException");
            } catch (DifyException e) {
                assertEquals(Integer.valueOf(400), e.getHttpStatus(), "400 http status");
                assertEquals("bad_request", e.getDifyCode(), "400 code=bad_request 透传");
            }

            // A7: 404 透传
            try {
                chat.updateConversationVariable("conv-1", "var-1", "dark_mode", "u1");
                throw new AssertionError("expected DifyException");
            } catch (DifyException e) {
                assertEquals(Integer.valueOf(404), e.getHttpStatus(), "404 http status");
                assertEquals("conversation_variable_not_found", e.getDifyCode(), "404 code 透传");
            }
        } finally {
            server.close();
        }
    }

    public void testMessageDomainRegressionAndAgentBlockingBadRequest() throws Exception {
        RecordingHttpServer server = new RecordingHttpServer(RecordingHttpServer.sequence(
                RecordingHttpServer.json(200, "{\"result\":\"success\"}"),
                RecordingHttpServer.json(200, "{\"result\":\"success\",\"data\":[\"s1\",\"s2\"]}"),
                RecordingHttpServer.json(400, "{\"code\":\"bad_request\",\"message\":\"Agent apps only support streaming mode.\",\"status\":400}")
        )).start();
        try {
            IAppChatClient chat = DifyClientBuilder.app(server.baseUrl(), "app-key").chat().build();

            // stopResponse: POST /chat-messages/{task_id}/stop（现状回归）
            Boolean stopped = chat.stopResponse("task-1", "u1");
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("POST", recorded.method, "stop method");
            assertEquals("/chat-messages/task-1/stop", recorded.path, "POST /chat-messages/{task_id}/stop path");
            assertEquals("u1", JSON.parseObject(recorded.body).getString("user"), "stop body user");
            assertEquals(Boolean.TRUE, stopped, "stop result");

            // suggestedList: GET /messages/{message_id}/suggested（现状回归）
            List<String> suggested = chat.suggestedList("msg-1", "u1");
            recorded = server.last();
            assertEquals("GET", recorded.method, "suggested method");
            assertEquals("/messages/msg-1/suggested", recorded.path, "GET /messages/{id}/suggested path");
            assertEquals("u1", recorded.query("user"), "suggested query user");
            assertEquals("s1", suggested.get(0), "suggested data");

            // Agent 应用阻塞调用返回 400 bad_request 原样透传（A6）
            Map<String, Object> inputs = new HashMap<String, Object>();
            inputs.put("q", "hi");
            ParamMessage message = ParamMessage.build(inputs, "u1");
            message.setResponseMode("blocking");
            try {
                chat.sendMessages(message);
                throw new AssertionError("expected DifyException");
            } catch (DifyException e) {
                assertEquals(Integer.valueOf(400), e.getHttpStatus(), "400 http status");
                assertEquals("bad_request", e.getDifyCode(), "400 code=bad_request 透传");
            }
            assertEquals("/chat-messages", server.last().path, "POST /chat-messages path");
        } finally {
            server.close();
        }
    }

    public void testWorkflowRunByIdStopStatusAndLogPaths() throws Exception {
        RecordingHttpServer server = new RecordingHttpServer(
                RecordingHttpServer.json(200, "{\"workflow_run_id\":\"run-9\",\"task_id\":\"task-9\",\"result\":\"success\"}")
        ).start();
        try {
            IAppFlowClient flow = DifyClientBuilder.app(server.baseUrl(), "app-key").flow().build();

            // 按 ID 执行：POST /workflows/{workflow_id}/run（A11）
            Map<String, Object> inputs = new HashMap<String, Object>();
            inputs.put("q", "hi");
            JSONObject runResult;
            ParamMessage runMessage = ParamMessage.build(inputs, "u1");
            runMessage.setResponseMode("blocking");
            runResult = flow.runByIdBlocking("wf-1", runMessage);
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("POST", recorded.method, "runById method");
            assertEquals("/workflows/wf-1/run", recorded.path, "POST /workflows/{workflow_id}/run path");
            JSONObject runBody = JSON.parseObject(recorded.body);
            assertEquals("u1", runBody.getString("user"), "run body user");
            assertEquals("blocking", runBody.getString("response_mode"), "run body response_mode");
            assertEquals("run-9", runResult.getString("workflow_run_id"), "runByIdBlocking 返回值");

            // 运行详情：GET /workflows/run/{workflow_run_id}（复数 workflows，现状回归）
            flow.getWorkFlowStatus("run-9");
            recorded = server.last();
            assertEquals("GET", recorded.method, "status method");
            assertEquals("/workflows/run/run-9", recorded.path, "GET /workflows/run/{id} path");

            // 停止任务：POST /workflows/tasks/{task_id}/stop（复数 tasks，A11 修正项）
            Boolean stopped = flow.stopWorkFlow("task-1", "u1");
            recorded = server.last();
            assertEquals("POST", recorded.method, "stop workflow method");
            assertEquals("/workflows/tasks/task-1/stop", recorded.path, "POST /workflows/tasks/{task_id}/stop path（复数）");
            assertEquals("u1", JSON.parseObject(recorded.body).getString("user"), "stop workflow body user");
            assertEquals(Boolean.TRUE, stopped, "stop workflow result");

            // 工作流日志：status 取 partial-succeeded（A11 WorkflowStatus 补齐）
            flow.getWorkFlowLog("kw", WorkflowStatus.partialSucceeded, 1, 20);
            recorded = server.last();
            assertEquals("/workflows/logs", recorded.path, "GET /workflows/logs path");
            assertEquals("kw", recorded.query("keyword"), "log query keyword");
            assertEquals("partial-succeeded", recorded.query("status"), "log query status=partial-succeeded");
            assertEquals("1", recorded.query("page"), "log query page");
            assertEquals("20", recorded.query("limit"), "log query limit");

            // WorkflowStatus 全部取值（A11）
            assertEquals("running", WorkflowStatus.running.getValue(), "running");
            assertEquals("succeeded", WorkflowStatus.succeeded.getValue(), "succeeded");
            assertEquals("failed", WorkflowStatus.failed.getValue(), "failed");
            assertEquals("stopped", WorkflowStatus.stopped.getValue(), "stopped");
            assertEquals("partial-succeeded", WorkflowStatus.partialSucceeded.getValue(), "partial-succeeded");
            assertEquals("paused", WorkflowStatus.paused.getValue(), "paused");
        } finally {
            server.close();
        }
    }

    public void testHumanInputFormGetSubmitAnd412PassThrough() throws Exception {
        RecordingHttpServer server = new RecordingHttpServer(RecordingHttpServer.sequence(
                RecordingHttpServer.json(200, "{\"form_content\":\"review\",\"user_actions\":[{\"id\":\"approve\"}]}"),
                RecordingHttpServer.json(200, "{}"),
                RecordingHttpServer.json(412, "{\"code\":\"human_input_form_expired\",\"message\":\"Form expired\",\"status\":412}"),
                RecordingHttpServer.json(412, "{\"code\":\"human_input_form_submitted\",\"message\":\"Already submitted\",\"status\":412}"),
                RecordingHttpServer.json(404, "{\"code\":\"form_token_not_found\",\"message\":\"Form not found\",\"status\":404}")
        )).start();
        try {
            IAppBaseClient base = DifyClientBuilder.app(server.baseUrl(), "app-key").base().build();

            // GET /form/human_input/{form_token}（A12）
            String form = base.getHumanInputForm("tok-1");
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("GET", recorded.method, "form GET method");
            assertEquals("/form/human_input/tok-1", recorded.path, "GET /form/human_input/{form_token} path");
            assertEquals("review", JSON.parseObject(form).getString("form_content"), "form 响应");

            // POST /form/human_input/{form_token}（A12）
            Map<String, Object> answers = new HashMap<String, Object>();
            answers.put("feedback", "ok");
            base.submitHumanInputForm("tok-1", "approve", answers, "u1");
            recorded = server.last();
            assertEquals("POST", recorded.method, "form POST method");
            assertEquals("/form/human_input/tok-1", recorded.path, "POST path");
            JSONObject body = JSON.parseObject(recorded.body);
            assertEquals("approve", body.getString("action"), "body action");
            assertEquals("u1", body.getString("user"), "body user");
            assertEquals("ok", body.getJSONObject("inputs").getString("feedback"), "body inputs");

            // 412 独立错误码透传，不折叠进 400（A12）
            try {
                base.getHumanInputForm("tok-1");
                throw new AssertionError("expected DifyException");
            } catch (DifyException e) {
                assertEquals(Integer.valueOf(412), e.getHttpStatus(), "412 http status");
                assertEquals("human_input_form_expired", e.getDifyCode(), "412 独立错误码 expired");
            }

            // A12: 412 human_input_form_submitted 独立错误码透传
            try {
                base.getHumanInputForm("tok-1");
                throw new AssertionError("expected DifyException");
            } catch (DifyException e) {
                assertEquals(Integer.valueOf(412), e.getHttpStatus(), "412 submitted http status");
                assertEquals("human_input_form_submitted", e.getDifyCode(), "412 独立错误码 submitted");
            }

            // A12: 404 独立错误码透传（表单 token 不存在）
            try {
                base.getHumanInputForm("tok-1");
                throw new AssertionError("expected DifyException");
            } catch (DifyException e) {
                assertEquals(Integer.valueOf(404), e.getHttpStatus(), "404 http status");
                assertEquals("form_token_not_found", e.getDifyCode(), "404 独立错误码");
            }
        } finally {
            server.close();
        }
    }

    public void testWorkflowEventStreamDispatchesDocumentedEvents() throws Exception {
        StringBuilder payload = new StringBuilder();
        String[][] events = new String[][] {
                {"ping", "{\"x\":1}"},
                {"workflow_started", "{\"workflow_run_id\":\"run-1\"}"},
                {"node_started", "{\"node_id\":\"n1\"}"},
                {"text_chunk", "{\"text\":\"hi\"}"},
                {"reasoning_chunk", "{\"text\":\"think\"}"},
                {"agent_message", "{\"text\":\"a\"}"},
                {"agent_thought", "{\"thought\":\"t\"}"},
                {"tts_message", "{\"audio\":\"xx\"}"},
                {"human_input_required", "{\"form_token\":\"ft-1\",\"workflow_run_id\":\"run-1\"}"},
                {"workflow_paused", "{\"workflow_run_id\":\"run-1\"}"},
                {"node_finished", "{\"node_id\":\"n1\"}"},
                {"workflow_finished", "{\"workflow_run_id\":\"run-1\",\"status\":\"stopped\"}"},
                {"message", "{\"answer\":\"hi\"}"},
                {"message_end", "{\"conversation_id\":\"c1\"}"},
                {"error", "{\"code\":\"invalid_param\",\"message\":\"boom\",\"status\":400}"}
        };
        for (String[] event : events) {
            payload.append("event: ").append(event[0]).append('\n');
            payload.append("data: ").append(event[1]).append('\n');
            payload.append('\n');
        }

        RecordingHttpServer server = new RecordingHttpServer(RecordingHttpServer.sse(payload.toString())).start();
        try {
            IAppFlowClient flow = DifyClientBuilder.app(server.baseUrl(), "app-key").flow().build();

            final List<String> receivedTypes = Collections.synchronizedList(new ArrayList<String>());
            final List<String> receivedData = Collections.synchronizedList(new ArrayList<String>());
            final CountDownLatch latch = new CountDownLatch(events.length);
            final List<Throwable> failures = Collections.synchronizedList(new ArrayList<Throwable>());

            flow.streamWorkflowEvents("run-1", "u1", true, true, new EventSourceListener() {
                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    receivedTypes.add(type);
                    receivedData.add(data);
                    latch.countDown();
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    // 连接收完后关闭属正常路径；仅记录供诊断，不作为断言
                    failures.add(t);
                }
            });

            assertTrue(latch.await(15, TimeUnit.SECONDS), "SSE 事件全部到达");
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("GET", recorded.method, "恢复事件流 method");
            assertEquals("/workflow/run-1/events", recorded.path, "GET /workflow/{id}/events path（单数 workflow）");
            assertEquals("u1", recorded.query("user"), "query user 必填");
            assertEquals("true", recorded.query("include_state_snapshot"), "query include_state_snapshot");
            assertEquals("true", recorded.query("continue_on_pause"), "query continue_on_pause");
            assertEquals("Bearer app-key", recorded.authorization, "SSE 也携带 Bearer");

            Set<String> expected = new HashSet<String>();
            for (String[] event : events) {
                expected.add(event[0]);
            }
            assertTrue(receivedTypes.containsAll(expected),
                    "全部文档事件类型被分派，实际=" + receivedTypes);

            // human_input_required 携带 form_token 与 workflow_run_id（A13）
            int idx = receivedTypes.indexOf("human_input_required");
            assertTrue(idx >= 0, "human_input_required 已分派");
            JSONObject humanInput = JSON.parseObject(receivedData.get(idx));
            assertEquals("ft-1", humanInput.getString("form_token"), "human_input_required.form_token");
            assertEquals("run-1", humanInput.getString("workflow_run_id"), "human_input_required.workflow_run_id");

            // HTTP 200 + error 事件的业务错误以事件形式分派（A13）
            assertTrue(receivedTypes.contains("error"), "error 事件按事件分派而非异常");
        } finally {
            server.close();
        }
    }

    public void testDifyChatEventConstantsMatchDocumentedTypes() {
        assertEquals("message", DifyChatEvent.message, "message");
        assertEquals("message_end", DifyChatEvent.messageEnd, "message_end");
        assertEquals("agent_message", DifyChatEvent.agentMessage, "agent_message");
        assertEquals("agent_thought", DifyChatEvent.agentThought, "agent_thought");
        assertEquals("tts_message", DifyChatEvent.ttsMessage, "tts_message");
        assertEquals("workflow_started", DifyChatEvent.workflowStarted, "workflow_started");
        assertEquals("node_started", DifyChatEvent.nodeStarted, "node_started");
        assertEquals("node_finished", DifyChatEvent.nodeFinished, "node_finished");
        assertEquals("text_chunk", DifyChatEvent.textChunk, "text_chunk");
        assertEquals("reasoning_chunk", DifyChatEvent.reasoningChunk, "reasoning_chunk");
        assertEquals("human_input_required", DifyChatEvent.humanInputRequired, "human_input_required");
        assertEquals("workflow_paused", DifyChatEvent.workflowPaused, "workflow_paused");
        assertEquals("workflow_finished", DifyChatEvent.workflowFinished, "workflow_finished");
        assertEquals("error", DifyChatEvent.error, "error");
        assertEquals("ping", DifyChatEvent.ping, "ping");
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
