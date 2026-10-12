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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class AppBaseClientImpl implements IAppBaseClient {
    /**
     * Dify 文档规定的语音转文字可接受 MIME 集合（audio/mpeg 不被接受）
     */
    private static final Set<String> AUDIO_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "audio/mp3", "audio/m4a", "audio/x-m4a", "audio/wav", "audio/amr", "audio/mpga"
    ));

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
     * 获取应用 Web 应用设置（站点配置、主题与自定义选项）
     * <p>对应 GET /site；未找到站点或工作空间已归档时服务端返回 403 forbidden。</p>
     *
     * @return Web 应用设置的JSON字符串
     */
    @Override
    public String getAppWebAppSettings() {
        return SimpleHttpClient.get(config).requestJson(AppRoutes.WEBAPP_SETTINGS, null, null);
    }

    /**
     * 获取终端用户信息
     * <p>对应 GET /end-users/{end_user_id}；404 end_user_not_found 时抛出 DifyException。</p>
     *
     * @param endUserId 终端用户 ID（例如上传文件响应中的 created_by）
     * @return 终端用户详情的JSON字符串
     */
    @Override
    public String getEndUser(String endUserId) {
        DifyRoute route = AppRoutes.END_USER.format(new HashMap<String, Object>() {{
            put("endUserId", endUserId);
        }});
        return SimpleHttpClient.get(config).requestJson(route, null, null);
    }

    /**
     * 下载/预览已上传的文件
     * <p>对应 GET /files/{file_id}/preview；403 file_access_denied、404 file_not_found 抛出 DifyException。</p>
     *
     * @param fileId       上传文件接口返回的文件 ID
     * @param asAttachment true 时以附件形式下载
     * @return 文件原始字节数据
     */
    @Override
    public byte[] downloadFile(String fileId, boolean asAttachment) {
        return downloadFile(fileId, null, asAttachment);
    }

    /**
     * 下载/预览已上传的文件（携带终端用户标识）
     * <p>对应 GET /files/{file_id}/preview?user=；该接口访问范围由应用与消息决定、
     * 与 user 取值无关（文档语义），此处携带以保持文件相关调用的 user 显式传递约定。</p>
     *
     * @param fileId       上传文件接口返回的文件 ID
     * @param user         终端用户标识（可为 null）
     * @param asAttachment true 时以附件形式下载
     * @return 文件原始字节数据
     */
    @Override
    public byte[] downloadFile(String fileId, String user, boolean asAttachment) {
        DifyRoute route = AppRoutes.FILE_PREVIEW.format(new HashMap<String, Object>() {{
            put("fileId", fileId);
        }});
        Map<String, Object> query = new HashMap<>();
        if (user != null && user.trim().length() > 0) {
            query.put("user", user);
        }
        if (asAttachment) {
            query.put("as_attachment", "true");
        }
        return SimpleHttpClient.get(config).requestBytes(route, query, null);
    }

    /**
     * 获取应用的消息反馈列表
     * <p>对应 GET /app/feedbacks。</p>
     *
     * @param page  页码（可选，默认 1）
     * @param limit 每页条数（可选，1–101，默认 20）
     * @return 反馈列表的JSON字符串
     */
    @Override
    public String queryFeedbacks(Integer page, Integer limit) {
        Map<String, Object> query = new HashMap<>();
        query.put("page", page);
        query.put("limit", limit);
        return SimpleHttpClient.get(config).requestJson(AppRoutes.APP_FEEDBACKS, query, null);
    }

    /**
     * 获取人工介入表单
     * <p>对应 GET /form/human_input/{form_token}；表单一次性使用，
     * 412 human_input_form_submitted（已提交）/ 412 human_input_form_expired（已过期）为独立错误码。</p>
     *
     * @param formToken 流式 human_input_required 事件携带的 form_token
     * @return 表单内容的JSON字符串
     */
    @Override
    public String getHumanInputForm(String formToken) {
        DifyRoute route = AppRoutes.HUMAN_INPUT_FORM_GET.format(new HashMap<String, Object>() {{
            put("formToken", formToken);
        }});
        return SimpleHttpClient.get(config).requestJson(route, null, null);
    }

    /**
     * 提交人工介入表单（提交后工作流恢复执行）
     * <p>对应 POST /form/human_input/{form_token}；action 必须来自表单 user_actions[].id。</p>
     *
     * @param formToken 表单令牌
     * @param action    操作按钮 ID
     * @param inputs    按 output_variable_name 为键的表单数据
     * @param user      终端用户标识，须与发起运行的 user 一致
     * @return 提交结果的JSON字符串（成功为 {}）
     */
    @Override
    public String submitHumanInputForm(String formToken, String action, Map<String, Object> inputs, String user) {
        DifyRoute route = AppRoutes.HUMAN_INPUT_FORM_SUBMIT.format(new HashMap<String, Object>() {{
            put("formToken", formToken);
        }});
        Map<String, Object> data = new HashMap<>();
        data.put("action", action);
        data.put("inputs", inputs != null ? inputs : new HashMap<String, Object>());
        data.put("user", user);
        return SimpleHttpClient.get(config).requestJson(route, null, data);
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
     * <p>对齐文档限制：接受 MIME 为 audio/mp3、audio/m4a（含 audio/x-m4a）、audio/wav、audio/amr、
     * audio/mpga（audio/mpeg 不被接受），文件最大 30MB；本地校验失败抛 DifyClientException，
     * 服务端仍可能返回 413 audio_too_large / 415 unsupported_audio_type。</p>
     *
     * @param file 语音文件对象，大小限制30MB
     * @param user 用户标识，需保证在应用内唯一
     * @param fileType 文件的ContentType类型，如果为空则根据文件后缀推断
     * @return 转换后的文本内容
     */
    @Override
    public String audioToText(File file, String user, String fileType) {
        String effectiveType = validateAudioFile(file, fileType);

        Map<String, Object> data = new HashMap<>();
        data.put("file", file);
        data.put("user", user);

        JSONObject result = JSON.parseObject(
                SimpleHttpClient.get(config).requestMultipart(AppRoutes.AUDIO_TO_TEXT, null, data, effectiveType)
        );

        return result.getString("text");
    }

    /**
     * 语音文件本地校验（对齐 Dify 文档：30MB 上限 + 受支持 MIME 集合）
     *
     * @param file     待转写的语音文件
     * @param fileType 显式指定的 ContentType，可为 null
     * @return 实际生效的 ContentType
     */
    private static String validateAudioFile(File file, String fileType) {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new DifyClientException("audio file does not exist: " + file);
        }

        long maxBytes = 30L * 1024 * 1024;
        if (file.length() > maxBytes) {
            throw new DifyClientException(
                    "audio_too_large: audio file exceeds 30MB limit, size=" + file.length() + " bytes");
        }

        String effectiveType = fileType != null && fileType.trim().length() > 0
                ? fileType.trim()
                : guessAudioContentType(file.getName());

        if (effectiveType == null || !AUDIO_CONTENT_TYPES.contains(effectiveType)) {
            throw new DifyClientException(
                    "unsupported_audio_type: accepted types are audio/mp3, audio/m4a, audio/x-m4a, "
                            + "audio/wav, audio/amr, audio/mpga; actual=" + effectiveType);
        }
        return effectiveType;
    }

    private static String guessAudioContentType(String fileName) {
        String lower = fileName != null ? fileName.toLowerCase() : "";
        int dot = lower.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        String ext = lower.substring(dot + 1);
        if ("mp3".equals(ext)) {
            return "audio/mp3";
        }
        if ("m4a".equals(ext)) {
            return "audio/m4a";
        }
        if ("wav".equals(ext)) {
            return "audio/wav";
        }
        if ("amr".equals(ext)) {
            return "audio/amr";
        }
        if ("mpga".equals(ext)) {
            return "audio/mpga";
        }
        return null;
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
     * 将文本转换为语音（支持指定音色）
     *
     * <p>voice 不传（null）时使用应用参数 text_to_speech.voice 的默认音色；
     * streaming 字段为向后兼容保留、无实际效果。</p>
     *
     * @param user 用户标识，需保证在应用内唯一
     * @param text 语音生成内容。如果没有传 message-id 的话，则会使用这个字段的内容
     * @param messageId Dify 生成的文本消息 message-id，与 text 同传时优先使用
     * @param voice 音色（可选）
     * @return AudioFile音频文件对象
     */
    @Override
    public AudioFile textToAudio(String user, String text, String messageId, String voice) {
        Map<String, Object> data = new HashMap<>();
        data.put("message_id", messageId);
        data.put("text", text);
        data.put("user", user);
        data.put("voice", voice);

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
