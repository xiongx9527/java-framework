/**
 * 调用上下文：一次请求在整条 AI 链路里携带的身份与追踪信息。
 *
 * <p>只放「跨能力都要用」的字段（trace / 租户 / 用户 / 扩展属性），不放业务参数。</p>
 */
package com.xiongx9527.fw.plus.ai.base.context;
