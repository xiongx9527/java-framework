package com.xiongx9527.fw.plus.ai.nl2sql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * NL2SQL 样例应用（langchain4j RAG 召回版）。
 *
 * <p>默认使用 H2 内存示例库（商品/客户/销售订单），启动后即可：
 * <pre>
 *   POST /nl2sql/train     # 训练：JDBC 元数据 → DDL → bge-m3 向量化 → 写入 Qdrant
 *   POST /nl2sql/ask       # 问答：向量召回 → gpt-oss 生成 SQL → 执行并返回
 * </pre>
 * 依赖 192.168.1.100 的 Ollama 与 Qdrant（连接参数见 application.yaml）；
 * 切真实 MySQL 时改 datasource 配置即可（详见 docs/nl2sql/ 下文档）。
 */
@SpringBootApplication
public class Nl2SqlSampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(Nl2SqlSampleApplication.class, args);
	}
}