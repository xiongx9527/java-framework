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

	/**
	 * 使用 Router 已选择的稳定模型 key 生成向量。
	 *
	 * <p>默认实现保持现有二参数 SPI 的源码与二进制兼容；需要管理多个模型实例的 Adapter 应覆盖本方法，
	 * 并严格使用 {@code modelKey} 选择实例。默认 Flow 已完成请求、项目、数据源、Agent、策略和候选链路由，
	 * Adapter 不得重新解释层级优先级或静默切换其他模型。</p>
	 *
	 * @param context 当前项目和已认证操作人
	 * @param text 已获授权且准备送模的文本
	 * @param modelKey Core Router 选中的稳定模型 key
	 * @return 新数组形式的有限浮点向量
	 */
	default float[] embed(AskDataProjectContext context, String text, String modelKey) {
		return embed(context, text);
	}

	/**
	 * 返回带稳定失败分类和模型用量的 Embedding 结果。
	 *
	 * <p>旧实现无需修改即可继续工作；需要受控候选切换或精确用量审计的 Adapter 应覆盖本方法。</p>
	 */
	default AskDataEmbeddingResult embedResult(AskDataProjectContext context, String text, String modelKey) {
		return AskDataEmbeddingResult.success(embed(context, text, modelKey), modelKey);
	}
}
