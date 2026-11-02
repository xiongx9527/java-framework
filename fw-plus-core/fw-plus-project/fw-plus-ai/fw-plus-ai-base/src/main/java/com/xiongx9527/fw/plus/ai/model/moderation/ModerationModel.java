package com.xiongx9527.fw.plus.ai.model.moderation;

import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;
import com.xiongx9527.fw.plus.ai.exception.UnsupportedFeatureException;
import com.xiongx9527.fw.plus.ai.message.ChatMessage;
import com.xiongx9527.fw.plus.ai.model.ModelProvider;
import com.xiongx9527.fw.plus.ai.model.moderation.listener.ModerationModelListener;
import com.xiongx9527.fw.plus.ai.model.moderation.listener.ModerationModelRequestContext;
import com.xiongx9527.fw.plus.ai.model.moderation.listener.ModerationModelResponseContext;
import com.xiongx9527.fw.plus.ai.model.output.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 内容审核模型端口。
 *
 * <p><b>作用</b>：在用户输入进入模型前、模型输出返回用户前做违规判定，
 * 是安全合规链路的标准一环。上层只依赖本接口，换审核服务不改调用方代码。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。逐方法镜像
 * {@code dev.langchain4j.model.moderation.ModerationModel}。</p>
 *
 * <p><b>实现约定</b>：实现者通常只需覆写 {@link #doModerate(ModerationRequest)}；
 * {@link #moderate(ModerationRequest)} 已负责监听器派发与异常回调。</p>
 *
 * @author xiongxiao
 */
public interface ModerationModel {

    /**
     * 执行审核请求（含监听器派发）。
     *
     * @param request 审核请求
     * @return 审核响应
     */
    default ModerationResponse moderate(ModerationRequest request) {
        List<ModerationModelListener> listeners = listeners();
        ModerationModelRequestContext requestContext =
                new ModerationModelRequestContext(request, provider(), Collections.<Object, Object>emptyMap());
        ModerationModelListenerUtils.onRequest(requestContext, listeners);
        try {
            ModerationResponse response = doModerate(request);
            ModerationModelListenerUtils.onResponse(new ModerationModelResponseContext(
                    response, request, provider(), Collections.<Object, Object>emptyMap()), listeners);
            return response;
        } catch (RuntimeException e) {
            ModerationModelListenerUtils.onError(new com.xiongx9527.fw.plus.ai.model.moderation.listener
                    .ModerationModelErrorContext(e, request, provider(), Collections.<Object, Object>emptyMap()),
                    listeners);
            throw e;
        }
    }

    /**
     * 真正执行审核（由实现覆写）。
     *
     * @param request 审核请求
     * @return 审核响应
     */
    default ModerationResponse doModerate(ModerationRequest request) {
        throw new UnsupportedFeatureException("Not supported yet.");
    }

    /**
     * 审核一段文本。
     *
     * @param text 文本
     * @return 审核结论
     */
    default Response<Moderation> moderate(String text) {
        return toResponse(moderate(ModerationRequest.builder().texts(Collections.singletonList(text)).build()));
    }

    /**
     * 审核一条聊天消息。
     *
     * @param message 聊天消息
     * @return 审核结论
     */
    default Response<Moderation> moderate(ChatMessage message) {
        return moderate(toText(message));
    }

    /**
     * 审核一组聊天消息（合并为一次请求）。
     *
     * @param messages 聊天消息列表
     * @return 审核结论
     */
    default Response<Moderation> moderate(List<ChatMessage> messages) {
        List<String> texts = new ArrayList<String>();
        if (messages != null) {
            for (ChatMessage message : messages) {
                texts.add(toText(message));
            }
        }
        return toResponse(moderate(ModerationRequest.builder().texts(texts).build()));
    }

    /**
     * 审核一个文本片段。
     *
     * @param textSegment 文本片段
     * @return 审核结论
     */
    default Response<Moderation> moderate(TextSegment textSegment) {
        return moderate(textSegment.text());
    }

    /**
     * 已注册的监听器。
     *
     * @return 不可变监听器列表，默认空
     */
    default List<ModerationModelListener> listeners() {
        return Collections.emptyList();
    }

    /**
     * 厂商标识。
     *
     * @return 厂商标识；未声明时为 {@code null}
     */
    default ModelProvider provider() {
        return null;
    }

    /**
     * 模型名。
     *
     * @return 模型名；未知时为 {@code null}
     */
    default String modelName() {
        return null;
    }

    /**
     * 把聊天消息转成待审文本。
     *
     * @param message 聊天消息
     * @return 文本内容
     */
    static String toText(ChatMessage message) {
        return message == null ? "" : message.text();
    }

    /**
     * 把审核响应转换为通用响应载体。
     *
     * @param response 审核响应
     * @return 通用响应
     */
    static Response<Moderation> toResponse(ModerationResponse response) {
        return Response.from(response == null ? null : response.moderation());
    }

    /**
     * 便捷方法：审核多段文本。
     *
     * @param texts 文本数组
     * @return 审核结论
     */
    default Response<Moderation> moderateTexts(String... texts) {
        return toResponse(moderate(ModerationRequest.builder().texts(Arrays.asList(texts)).build()));
    }
}
