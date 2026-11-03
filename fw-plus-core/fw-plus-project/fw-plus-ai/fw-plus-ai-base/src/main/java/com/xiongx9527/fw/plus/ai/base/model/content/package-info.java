/**
 * 多模态内容片段：一条消息可由文本、图片、音频、文件任意组合。
 *
 * <p>只描述「片段是什么」，不描述「怎么编码」：URL 还是 base64、放在 {@code content}
 * 还是 {@code image_url} 字段、Anthropic 的 {@code source} 结构，都由 adapter 按上游协议转换。</p>
 */
package com.xiongx9527.fw.plus.ai.base.model.content;
