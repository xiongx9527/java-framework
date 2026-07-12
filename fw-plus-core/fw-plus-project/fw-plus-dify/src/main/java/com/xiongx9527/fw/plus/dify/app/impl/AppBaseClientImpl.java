package com.xiongx9527.fw.plus.dify.app.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.xiongx9527.fw.plus.dify.SimpleHttpClient;
import com.xiongx9527.fw.plus.dify.app.DifyEventSourceBaseListener;
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

@Slf4j
public class AppBaseClientImpl implements IAppBaseClient {
    /**
     * API配置对象
     */
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

    /**
     * 获取HTTP客户端实例
     *
     * @return SimpleHttpClient对象
     */
    @Override
    public SimpleHttpClient httpClient() {
        return SimpleHttpClient.get(config);
    }

    /**
     * 获取应用基础信息
     *
     * @return 应用基础信息的JSON字符串
     */
    @Override
    public String getAppInfo()  {
        return SimpleHttpClient.get(config).requestJson(AppRoutes.INFO, null, null);
    }

    /**
     * 获取应用参数信息
     *
     * @return 应用参数信息的JSON字符串
     */
    @Override
    public String getAppParameters() {
        return SimpleHttpClient.get(config).requestJson(AppRoutes.PARAMETERS, null, null);
    }

    /**
     * 获取应用元数据信息
     *
     * @return 应用元数据信息的JSON字符串
     */
    @Override
    public String getAppMetaInfo() {
        return SimpleHttpClient.get(config).requestJson(AppRoutes.META_INFO, null, null);
    }

    /**
     * 上传文件到Dify平台
     *
     * @param file 文件对象
     * @param user 用户标识
     * @return DifyFileResult文件上传结果对象
     */
    @Override
    public DifyFileResult uploadFile(File file, String user) {
        Map<String, Object> data = new HashMap<>();
        data.put("file", file);
        data.put("user", user);

        String result = SimpleHttpClient.get(config).requestMultipart(AppRoutes.FILE_UPLOAD, null, data);
        return JSON.parseObject(result, DifyFileResult.class);
    }

    /**
     * 提交消息反馈
     *
     * @param messageId 消息ID
     * @param rating 反馈类型：like(点赞)、dislike(点踩)、null(撤销点赞)
     * @param user 用户标识，需保证在应用内唯一
     * @param content 反馈的具体内容
     * @return 操作是否成功
     */
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

    /**
     * 语音转文字（使用默认ContentType）
     *
     * @param file 语音文件对象
     * @param user 用户标识，需保证在应用内唯一
     * @return 转换后的文本内容
     */
    @Override
    public String audioToText(File file, String user) {
        return audioToText(file, user, null);
    }

    /**
     * 语音转文字（可指定ContentType）
     *
     * @param file 语音文件对象，支持格式：mp3、mp4、mpeg、mpga、m4a、wav、webm，大小限制15MB
     * @param user 用户标识，需保证在应用内唯一
     * @param fileType 文件的ContentType类型，如果为空则使用默认值
     * @return 转换后的文本内容
     */
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

    /**
     * 通过消息ID将文本转换为语音
     *
     * @param user 用户标识，需保证在应用内唯一
     * @param messageId Dify生成的文本消息ID
     * @return AudioFile音频文件对象
     */
    @Override
    public AudioFile textToAudioByMessage(String user, String messageId) {
        return textToAudio(user, null, messageId);
    }

    /**
     * 将文本转换为语音（不使用消息ID）
     *
     * @param user 用户标识，需保证在应用内唯一
     * @param text 要转换为语音的文本内容
     * @return AudioFile音频文件对象
     */
    @Override
    public AudioFile textToAudio(String user, String text) {
        return textToAudio(user, text, null);
    }

    /**
     * 将文本转换为语音（支持消息ID和文本内容）
     *
     * @param user 用户标识，需保证在应用内唯一
     * @param text 要转换为语音的文本内容，当messageId为空时使用
     * @param messageId Dify生成的文本消息ID，优先使用此ID查找内容进行语音合成
     * @return AudioFile音频文件对象
     */
    @Override
    public AudioFile textToAudio(String user, String text, String messageId) {
        Map<String, Object> data = new HashMap<>();
        data.put("message_id", messageId);
        data.put("text", text);
        data.put("user", user);

        return SimpleHttpClient.get(config).requestFile(AppRoutes.TEXT_TO_AUDIO, null, data);
    }

    /**
     * 发送同步请求到Dify接口
     *
     * @param route Dify路由配置
     * @param query URL查询参数
     * @param params 请求体参数
     * @return 响应结果的JSON字符串
     */
    @Override
    public String requestBlocking(DifyRoute route, Map<String, Object> query, Map<String, Object> params) {
        // body请求对象中，强制覆盖 response_mode
        params.put("response_mode", ResponseMode.blocking);
        return SimpleHttpClient.get(config).requestJson(route, query, params);
    }

    /**
     * 发送流式请求到Dify接口（带事件监听器）
     *
     * @param route Dify路由配置
     * @param query URL查询参数
     * @param params 请求体参数
     * @param listener SSE事件监听器
     * @return EventSource对象
     */
    @Override
    public EventSource requestStreaming(DifyRoute route,Map<String, Object> query,Map<String, Object> params,EventSourceListener listener) {
        // body请求对象中，强制覆盖 response_mode
        params.put("response_mode", ResponseMode.streaming);

        return SimpleHttpClient.get(config).requestSse(route, query, params, listener);
    }

    /**
     * 发送流式请求到Dify接口（使用默认监听器）
     * 
     * <p>该方法用于向 Dify 接口发送流式请求，使用默认的 DifyEventSourceListener 
     * 处理响应事件。适用于不需要自定义事件处理逻辑的常规流式请求场景。</p>
     *
     * @param route Dify路由配置
     * @param query URL查询参数
     * @param params 请求体参数
     * @return EventSource对象
     */
    @Override
    public EventSource requestStreaming(DifyRoute route,Map<String, Object> query,Map<String, Object> params) {
        return requestStreaming(route, query, params, new DifyEventSourceBaseListener());
    }

    /**
     * 查询标注列表
     *
     * @param page 页码，从1开始
     * @param limit 每页数量
     * @return DifyPage分页对象，包含JSONObject列表
     */
    @Override
    public DifyPage<JSONObject> queryAnnotations(Integer page, Integer limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("page", page);
        params.put("limit", limit);

        String result = SimpleHttpClient.get(config).requestJson(AppRoutes.ANNOTATION_GET, params);
        return JSON.parseObject(result, new TypeReference<DifyPage<JSONObject>>() {});
    }

    /**
     * 创建新的标注
     *
     * @param question 问题内容
     * @param answer 答案内容
     * @return 创建的标注对象
     */
    @Override
    public JSONObject createAnnotation(String question, String answer) {
        Map<String, Object> data = new HashMap<>();
        data.put("question", question);
        data.put("answer", answer);

        String result = SimpleHttpClient.get(config).requestJson(AppRoutes.ANNOTATION_ADD, null, data);
        return JSON.parseObject(result);
    }

    /**
     * 更新已有标注
     *
     * @param annotationId 标注ID
     * @param question 更新后的问题内容
     * @param answer 更新后的答案内容
     * @return 更新后的标注对象
     */
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

    /**
     * 删除标注
     *
     * @param annotationId 标注ID
     * @return 操作是否成功
     */
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

    /**
     * 配置标注回复功能
     *
     * @param enable 是否启用标注回复，true为启用，false为禁用
     * @param embeddingModelProvider 嵌入模型提供商，必须先在系统中配置
     * @param embeddingModel 嵌入模型名称
     * @param scoreThreshold 相似度阈值，大于此阈值时系统自动回复
     * @return 配置结果对象
     */
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

    /**
     * 查询标注回复配置任务的状态
     *
     * @param enable 动作标识，必须与setAnnotationReply调用时保持一致
     * @param jobId 任务ID，从setAnnotationReply接口返回的job_id
     * @return 任务状态对象
     */
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
