package com.xiongx9527.fw.plus.ai.interactive.tool;

import com.xiongx9527.fw.plus.ai.interactive.AnswerExtractor;
import com.xiongx9527.fw.plus.ai.interactive.CollectionResult;
import com.xiongx9527.fw.plus.ai.interactive.appointment.AppointmentRequest;

/**
 * 智能预约工具定义（端口）：预约项目（由业务按 id 解析，含可变表单模板）选定后，收集参数，提交确认。
 *
 * <p>框架只定义契约与 DTO，实现由业务完成：实现负责按预约项目 id 解析 {@code AppointmentItem}
 * + 其模板 {@code Form}、管理会话存储，可组合 {@code AppointmentFlow}。
 * 注意：预约项目由业务 / 上游选定后传入，本工具内部不做"列出项目供用户选"；
 * 收集完成后 {@link #finish(String)} 返回带 itemId 的 {@link AppointmentRequest} 交业务落库。</p>
 */
public interface AppointmentTool {

    /** 通过预约项目 id 开启实例（模板由实现按项目解析），返回会话与首帧。 */
    AppointmentSession open(String itemId);

    /** 按题干名提交答案，返回推进后的帧。 */
    CollectionResult answer(String sessionId, String questionName, Object value);

    /** 从用户一句话抽取并提交，返回推进后的帧。 */
    CollectionResult answer(String sessionId, String utterance, AnswerExtractor extractor);

    /** 当前收集状态。 */
    CollectionResult state(String sessionId);

    /** 是否收集完成。 */
    boolean isComplete(String sessionId);

    /** 交卷：返回带 itemId 的预约请求，业务据此调用自己的接口 / 工具提交。 */
    AppointmentRequest finish(String sessionId);
}
