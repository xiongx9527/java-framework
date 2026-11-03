/**
 * 统一事件监听契约。
 *
 * <p>所有能力（大语言、向量、重排、向量存储、检索、对话记忆）都有自己的监听器接口，
 * 共同继承 {@link com.xiongx9527.fw.plus.ai.base.event.AiListener}：一处实现
 * {@code onError} 即可覆盖全部能力的失败观测，其余事件按能力各自扩展。</p>
 *
 * <p>约定：监听器同步执行、必须线程安全、异常一律被吞（不得影响主链路）。</p>
 */
package com.xiongx9527.fw.plus.ai.base.event;
