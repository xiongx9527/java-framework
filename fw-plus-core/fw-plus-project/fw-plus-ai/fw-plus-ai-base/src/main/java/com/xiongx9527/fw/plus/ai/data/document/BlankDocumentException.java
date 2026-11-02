package com.xiongx9527.fw.plus.ai.data.document;

import com.xiongx9527.fw.plus.ai.exception.AiException;

/**
 * 空文档异常：解析结果为空文本时抛出。
 *
 * <p><b>作用</b>：空文档继续往下走只会产生无意义的向量与噪声检索结果，因此在解析阶段就失败。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.document.BlankDocumentException}，
 * 继承链对齐为 {@link AiException}。</p>
 *
 * @author xiongxiao
 */
public class BlankDocumentException extends AiException {

    /** 创建异常（无堆栈，避免高频抛出时的开销）。 */
    public BlankDocumentException() {
        super("Document is blank");
    }
}
