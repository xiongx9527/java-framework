package com.xiongx9527.fw.plus.ai.askdata.vector;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * AI 框架和模型厂商无关的 Embedding SPI。
 *
 * <p>实现必须按当前项目和操作人执行配额、路由和审计，但不得把该上下文编码进向量内容。实例可能被并发
 * 调用，Adapter 应复用线程安全客户端，并验证维度和有限数值。禁止记录原始敏感文本、默认绑定 Provider，
 * 或在失败时静默返回零向量。</p>
 */
public interface AskDataEmbeddingModel {

	/**
	 * 将已获授权的文本转换为向量。
	 *
	 * @param context 当前项目和已认证操作人
	 * @param text 已按业务权限选择且准备送模的文本
	 * @return 新数组形式的有限浮点向量
	 */
	float[] embed(AskDataProjectContext context, String text);
}
