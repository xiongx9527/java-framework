package com.xiongx9527.fw.plus.dify.app;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.dify.SimpleHttpClient;
import com.xiongx9527.fw.plus.dify.app.types.DifyFileResult;
import com.xiongx9527.fw.plus.dify.types.DifyPage;
import com.xiongx9527.fw.plus.dify.types.DifyRoute;
import com.xiongx9527.fw.plus.dify.types.AudioFile;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 基础Dify客户端
 */
public interface IAppBaseClient {
    /**
     * 获取一个Simple http client 对象
     */
    SimpleHttpClient httpClient();

    /**
     * 获取应用基础信息
     *
     * @return String
     */
    String getAppInfo();

    /**
     * 获取应用参数信息
     *
     * @return String
     */
    String getAppParameters();

    /**
     * 获取应用元数据
     *
     * @return String
     */
    String getAppMetaInfo();

    /**
     * 获取应用 Web 应用设置（站点配置、主题与自定义选项）
     * <p>对应 GET /site；未找到站点或工作空间已归档时返回 403 forbidden。</p>
     *
     * @return Web 应用设置的 JSON 字符串
     */
    String getAppWebAppSettings();

    /**
     * 获取终端用户信息
     * <p>对应 GET /end-users/{end_user_id}，可解析上传文件响应中的 created_by 等终端用户 ID；404 end_user_not_found。</p>
     *
     * @param endUserId 终端用户 ID（例如上传文件响应中的 created_by）
     * @return 终端用户详情的 JSON 字符串
     */
    String getEndUser(String endUserId);

    /**
     * 下载/预览已上传的文件
     * <p>对应 GET /files/{file_id}/preview；文件只能被引用它的消息所属应用访问
     * （403 file_access_denied、404 file_not_found）。</p>
     *
     * @param fileId        上传文件接口返回的文件 ID
     * @param asAttachment  true 时以附件形式下载（Content-Disposition: attachment）
     * @return 文件原始字节数据
     */
    byte[] downloadFile(String fileId, boolean asAttachment);

    /**
     * 下载/预览已上传的文件（携带终端用户标识）
     * <p>对应 GET /files/{file_id}/preview?user=；user 为端点可选参数，
     * 本接口的访问范围由应用与消息决定、与 user 取值无关（文档语义），此处携带以保持
     * 文件相关调用的 user 显式传递约定。</p>
     *
     * @param fileId        上传文件接口返回的文件 ID
     * @param user          终端用户标识（可为 null，可选参数）
     * @param asAttachment  true 时以附件形式下载（Content-Disposition: attachment）
     * @return 文件原始字节数据
     */
    byte[] downloadFile(String fileId, String user, boolean asAttachment);

    /**
     * 获取应用的消息反馈列表
     * <p>对应 GET /app/feedbacks。</p>
     *
     * @param page 页码（可选，默认 1）
     * @param limit 每页条数（可选，1–101，默认 20）
     * @return 反馈列表的 JSON 字符串
     */
    String queryFeedbacks(Integer page, Integer limit);

    /**
     * 获取人工介入表单
     * <p>对应 GET /form/human_input/{form_token}；表单一次性使用，
     * 412 human_input_form_submitted（已提交）/ 412 human_input_form_expired（已过期）为独立错误码。</p>
     *
     * @param formToken 流式 human_input_required 事件携带的 form_token
     * @return 表单内容的 JSON 字符串（form_content/inputs/user_actions/expiration_time）
     */
    String getHumanInputForm(String formToken);

    /**
     * 提交人工介入表单（提交后工作流恢复执行）
     * <p>对应 POST /form/human_input/{form_token}；action 必须来自表单 user_actions[].id；
     * 执行、上传、提交各次调用须使用一致的 user；412 语义同 {@link #getHumanInputForm(String)}。</p>
     *
     * @param formToken 表单令牌
     * @param action    操作按钮 ID（来自表单 user_actions[].id）
     * @param inputs    按各输入的 output_variable_name 为键的表单数据
     * @param user      终端用户标识，须与发起运行的 user 一致
     * @return 提交结果的 JSON 字符串（成功为 {}）
     */
    String submitHumanInputForm(String formToken, String action, Map<String, Object> inputs, String user);

    /**
     * 上传文件
     *
     * @param file 文件对象
     * @param user 用户标识
     * @return DifyFileResult
     */
    DifyFileResult uploadFile(File file, String user);

    /**
     * 消息反馈
     *
     * @param messageId 消息ID
     * @param rating    点赞 like, 点踩 dislike, 撤销点赞 null
     * @param user      用户标识，由开发者定义规则，需保证用户标识在应用内唯一。
     * @param content   消息反馈的具体信息。
     */
    Boolean feedbacks(String messageId, String rating, String user, String content);

    /**
     * 语音转文字
     *
     * @param file 语音文件。 对齐文档：接受 MIME：audio/mp3、audio/m4a（含 audio/x-m4a）、audio/wav、
     *             audio/amr、audio/mpga（audio/mpeg 不被接受），文件大小限制 30MB，
     *             超限服务端返回 413 audio_too_large，类型不支持返回 415 unsupported_audio_type
     * @param user 用户标识，由开发者定义规则，需保证用户标识在应用内唯一。
     */
    String audioToText(File file, String user);

    /**
     * 语音转文字
     *
     * @param file 语音文件。 对齐文档：接受 MIME：audio/mp3、audio/m4a（含 audio/x-m4a）、audio/wav、
     *             audio/amr、audio/mpga（audio/mpeg 不被接受），文件大小限制 30MB，
     *             超限服务端返回 413 audio_too_large，类型不支持返回 415 unsupported_audio_type
     * @param user 用户标识，由开发者定义规则，需保证用户标识在应用内唯一。
     * @param fileType 如果该参数不为空，那么file对象的ContentType将设置为该值。
     */
    String audioToText(File file, String user, String fileType);

    /**
     * 文字转语音
     *
     * @param user      用户标识，由开发者定义规则，需保证用户标识在应用内唯一
     * @param messageId Dify 生成的文本消息 message-id
     */
    AudioFile textToAudioByMessage(String user, String messageId);

    /**
     * 文字转语音
     *
     * @param user 用户标识，由开发者定义规则，需保证用户标识在应用内唯一。
     * @param text 语音生成内容。
     */
    AudioFile textToAudio(String user, String text);

    /**
     * 文字转语音
     *
     * @param user      用户标识，由开发者定义规则，需保证用户标识在应用内唯一。
     * @param text      语音生成内容。如果没有传 message-id的话，则会使用这个字段的内容
     * @param messageId Dify 生成的文本消息，那么直接传递生成的message-id 即可，后台会通过 message_id 查找相应的内容直接合成语音信息。
     *                  如果同时传 message_id 和 text，优先使用 message_id。
     */
    AudioFile textToAudio(String user, String text, String messageId);

    /**
     * 文字转语音（支持指定音色）
     *
     * @param user      用户标识，由开发者定义规则，需保证用户标识在应用内唯一
     * @param text      语音生成内容。如果没有传 message-id 的话，则会使用这个字段的内容
     * @param messageId Dify 生成的文本消息 message-id；与 text 同传时优先使用 message_id
     * @param voice     音色（可选）。不传时使用应用参数 text_to_speech.voice 的默认音色
     * @return AudioFile 音频文件对象
     */
    AudioFile textToAudio(String user, String text, String messageId, String voice);

    /**
     * 发送同步接口请求
     *
     * @param route  DifyRoute
     * @param query  Query params
     * @param params Body params
     * @return String
     */
    String requestBlocking(DifyRoute route, Map<String, Object> query, Map<String, Object> params);

    /**
     * 发送流式接口请求
     *
     * @param route  DifyRoute
     * @param query  Query params
     * @param params Body params
     * @return EventSource
     */
    EventSource requestStreaming(DifyRoute route,Map<String, Object> query,Map<String, Object> params,EventSourceListener listener);

    /**
     * 发送流式接口请求
     * @param route
     * @param query
     * @param params
     * @return
     */
    EventSource requestStreaming(DifyRoute route,Map<String, Object> query,Map<String, Object> params);

    /**
     * 获取标注列表
     * @param page 页码
     * @param limit 每页数量
     */
    DifyPage<JSONObject> queryAnnotations(Integer page, Integer limit);

    /**
     * 创建标注
     * @param question 问题
     * @param answer 答案内容
     */
    JSONObject createAnnotation(String question, String answer);

    /**
     * 更新标注
     * @param annotationId 标注ID
     * @param question 问题
     * @param answer 答案内容
     */
    JSONObject updateAnnotation(String annotationId, String question, String answer);

    /**
     * 删除标注
     * @param annotationId 标注ID
     */
    Boolean deleteAnnotation(String annotationId);

    /**
     * 标注回复初始设置
     * @param enable 动作，true 代表 'enable'，false 代表 'disable'
     * @param embeddingModelProvider 指定的嵌入模型提供商, 必须先在系统内设定好接入的模型，对应的是provider字段
     * @param embeddingModel 指定的嵌入模型，对应的是model字段
     * @param scoreThreshold 相似度阈值，当相似度大于该阈值时，系统会自动回复，否则不回复
     */
    JSONObject setAnnotationReply(Boolean enable, String embeddingModelProvider, String embeddingModel, Double scoreThreshold);

    /**
     * 查询标注回复初始设置任务状态
     * @param enable 动作，true 代表 'enable'，false 代表 'disable'，必须和标注回复初始设置接口的动作一致
     * @param jobId 任务 ID，从标注回复初始设置接口返回的 job_id
     */
    JSONObject getAnnotationJobStatus(Boolean enable, String jobId);
}
