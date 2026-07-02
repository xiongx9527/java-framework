package com.xiongx9527.fw.plus.dify.app.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.xiongx9527.fw.plus.dify.SimpleHttpClient;
import com.xiongx9527.fw.plus.dify.app.IAppBaseClient;
import com.xiongx9527.fw.plus.dify.app.types.DifyFileResult;
import com.xiongx9527.fw.plus.dify.app.types.ResponseMode;
import com.xiongx9527.fw.plus.dify.routes.AppRoutes;
import com.xiongx9527.fw.plus.dify.types.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class AppBaseClientImpl implements IAppBaseClient {
    protected final ApiConfig config;

    /**
     * constructor
     *
     * @param server Dify Server URL
     * @param apiKey The App Api Key
     */
    public AppBaseClientImpl(String server, String apiKey) {
        config = ApiConfig.builder().server(server).apiKey(apiKey).build();
    }

    @Override
    public SimpleHttpClient httpClient() {
        return SimpleHttpClient.get(config);
    }

    @Override
    public String getAppInfo()  {
        return SimpleHttpClient.get(config).requestJson(AppRoutes.INFO, null, null);
    }

    @Override
    public String getAppParameters() {
        return SimpleHttpClient.get(config).requestJson(AppRoutes.PARAMETERS, null, null);
    }

    @Override
    public String getAppMetaInfo() {
        return SimpleHttpClient.get(config).requestJson(AppRoutes.META_INFO, null, null);
    }

    @Override
    public DifyFileResult uploadFile(File file, String user) {
        Map<String, Object> data = new HashMap<>();
        data.put("file", file);
        data.put("user", user);

        String result = SimpleHttpClient.get(config).requestMultipart(AppRoutes.FILE_UPLOAD, null, data);
        return JSON.parseObject(result, DifyFileResult.class);
    }

    @Override
    public Boolean feedbacks(String messageId, String rating, String user, String content) {
        String result = SimpleHttpClient.get(config).requestJson(
                String.format("%s/%s/feedbacks", AppRoutes.MESSAGES.getUrl(), messageId),
                HttpMethod.POST,
                null,
                new HashMap<String, String>() {{
                    put("rating", rating);
                    put("user", user);
                    put("content", content);
                }}
        );

        JSONObject json = JSON.parseObject(result);
        return "success".equals(json.getString("result"));
    }

    @Override
    public String audioToText(File file, String user) {
        return audioToText(file, user, null);
    }

    @Override
    public String audioToText(File file, String user, String fileType) {
        Map<String, Object> data = new HashMap<>();
        data.put("file", file);
        data.put("user", user);

        JSONObject result = JSON.parseObject(
                SimpleHttpClient.get(config).requestMultipart(AppRoutes.AUDIO_TO_TEXT, null, data, fileType)
        );

        return result.getString("text");
    }

    @Override
    public AudioFile textToAudioByMessage(String user, String messageId) {
        return textToAudio(user, null, messageId);
    }

    @Override
    public AudioFile textToAudio(String user, String text) {
        return textToAudio(user, text, null);
    }

    @Override
    public AudioFile textToAudio(String user, String text, String messageId) {
        Map<String, Object> data = new HashMap<>();
        data.put("message_id", messageId);
        data.put("text", text);
        data.put("user", user);

        return SimpleHttpClient.get(config).requestFile(AppRoutes.TEXT_TO_AUDIO, null, data);
    }

    @Override
    public String requestBlocking(DifyRoute route, Map<String, Object> query, Map<String, Object> params) {
        // body请求对象中，强制覆盖 response_mode
        params.put("response_mode", ResponseMode.blocking);
        return SimpleHttpClient.get(config).requestJson(route, query, params);
    }

    @Override
    public EventSource requestStreaming(
            DifyRoute route,
            Map<String, Object> query,
            Map<String, Object> params,
            EventSourceListener listener
    ) {
        // body请求对象中，强制覆盖 response_mode
        params.put("response_mode", ResponseMode.streaming);

        return SimpleHttpClient.get(config).requestSse(route, query, params, listener);
    }

    @Override
    public DifyPage<JSONObject> queryAnnotations(Integer page, Integer limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("page", page);
        params.put("limit", limit);

        String result = SimpleHttpClient.get(config).requestJson(AppRoutes.ANNOTATION_GET, params);
        return JSON.parseObject(result, new TypeReference<DifyPage<JSONObject>>() {});
    }

    @Override
    public JSONObject createAnnotation(String question, String answer) {
        Map<String, Object> data = new HashMap<>();
        data.put("question", question);
        data.put("answer", answer);

        String result = SimpleHttpClient.get(config).requestJson(AppRoutes.ANNOTATION_ADD, null, data);
        return JSON.parseObject(result);
    }

    @Override
    public JSONObject updateAnnotation(String annotationId, String question, String answer) {
        Map<String, Object> data = new HashMap<>();
        data.put("question", question);
        data.put("answer", answer);

        DifyRoute route = AppRoutes.ANNOTATION_SET.format(
                new HashMap<String, Object>() {{
                    put("annotationId", annotationId);
                }}
        );

        String result = SimpleHttpClient.get(config).requestJson(route, null, data);
        return JSON.parseObject(result);
    }

    @Override
    public Boolean deleteAnnotation(String annotationId) {
        DifyRoute route = AppRoutes.ANNOTATION_DEL.format(
                new HashMap<String, Object>() {{
                    put("annotationId", annotationId);
                }}
        );

        String result = SimpleHttpClient.get(config).requestJson(route);
        JSONObject json = JSON.parseObject(result);

        return "success".equals(json.getString("result"));
    }

    @Override
    public JSONObject setAnnotationReply(Boolean enable, String embeddingModelProvider, String embeddingModel, Double scoreThreshold) {
        Map<String, Object> data = new HashMap<>();
        data.put("embedding_model_provider", embeddingModelProvider);
        data.put("embedding_model", embeddingModel);
        data.put("score_threshold", scoreThreshold);

        DifyRoute route = AppRoutes.ANNOTATION_REPLY_SET.format(
                new HashMap<String, Object>() {{
                    put("action", enable ? "enable" : "disable");
                }}
        );

        String result = SimpleHttpClient.get(config).requestJson(route, null, data);
        return JSON.parseObject(result);
    }

    @Override
    public JSONObject getAnnotationJobStatus(Boolean enable, String jobId) {
        DifyRoute route = AppRoutes.ANNOTATION_REPLY_SET.format(
                new HashMap<String, Object>() {{
                    put("action", enable ? "enable" : "disable");
                    put("jobId", jobId);
                }}
        );

        String result = SimpleHttpClient.get(config).requestJson(route);
        return JSON.parseObject(result);
    }

}
