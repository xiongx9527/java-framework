package com.xiongx9527.fw.plus.ai.askdata.context;

/**
 * AskData 上下文压缩 SPI。
 *
 * <p>业务可以注册确定性实现或语义模型 Adapter。任何实现都只能压缩脱敏正文，并必须原样保留结构化字段；
 * 失败时由 Starter 回退到确定性压缩，不能丢失恢复安全事实。</p>
 */
public interface AskDataContextCompressor {
	AskDataContextCompressionResult compress(AskDataContextCompressionRequest request);
}
