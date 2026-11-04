package com.xiongx9527.fw.plus.ai.interactive.tool;

import com.xiongx9527.fw.plus.ai.interactive.AnswerExtractor;
import com.xiongx9527.fw.plus.ai.interactive.CollectionResult;
import com.xiongx9527.fw.plus.ai.interactive.Form;

import java.util.Map;

/**
 * 反问 / 选项工具定义（端口）：一次性的参数收集对话。
 *
 * <p>框架只定义契约与 DTO，具体实现由业务完成（实现可从 MCP / DB 取数、管理会话存储）。
 * 业务实现内部通常组合 {@code FormCollector}。输入 {@link Form} 由业务按场景提供；
 * 输出复用 {@link CollectionResult}（含 ClarifyFrame / 答案），可直接序列化为 JSON 交给模型或前端。</p>
 */
public interface ClarifyTool {

    /** 开启一次收集，返回会话与首帧（INCOMPLETE 带反问帧 / COMPLETE 已齐）。 */
    ClarifySession start(Form form);

    /** 按题干名提交答案，返回推进后的帧。 */
    CollectionResult answer(String sessionId, String questionName, Object value);

    /** 从用户一句话抽取并提交，返回推进后的帧。 */
    CollectionResult answer(String sessionId, String utterance, AnswerExtractor extractor);

    /** 当前收集状态。 */
    CollectionResult state(String sessionId);

    /** 是否收集完成（必填已齐）。 */
    boolean isComplete(String sessionId);

    /** 交卷：返回全部 题干名 → 答案，由业务调用自己的接口提交。 */
    Map<String, Object> finish(String sessionId);
}
