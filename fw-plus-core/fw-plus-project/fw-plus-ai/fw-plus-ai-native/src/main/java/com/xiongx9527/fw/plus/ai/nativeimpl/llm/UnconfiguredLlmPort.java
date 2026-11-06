package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import com.xiongx9527.fw.plus.ai.base.model.LlmException;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;

/**
 * 未配置占位端口：LLM 后端未配置任何厂商 / 协议时暴露，保证应用缺省也能启动；
 * 任意调用都抛 {@link LlmException}（区别于「已配置但不可达」）。
 */
public class UnconfiguredLlmPort implements LlmPort {

    private static final String MSG =
            "llm is not configured; set fw.plus.ai.llm.default.{protocol,base-url,api-key} to enable it";

    @Override
    public LlmTokenStream stream(LlmRequest request) {
        throw new LlmException(MSG);
    }
}
