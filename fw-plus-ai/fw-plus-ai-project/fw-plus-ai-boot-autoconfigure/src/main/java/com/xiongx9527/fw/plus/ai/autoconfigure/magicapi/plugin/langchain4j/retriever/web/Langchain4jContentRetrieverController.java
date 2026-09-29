package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.web;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverProviderRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.Langchain4jContentRetrieverInfo;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * content-retriever 连接测试：按 body（datasource JSON）构建实例并真实检索一次，不落库、不注册。
 * 协议对齐官方 datasource test：返回 "ok" 或中文错误信息。
 */
public class Langchain4jContentRetrieverController extends MagicController implements MagicExceptionHandler {

	private static final Logger logger =
			LoggerFactory.getLogger(Langchain4jContentRetrieverController.class);

	private static final String DEFAULT_QUERY = "ping";

	private final ContentRetrieverProviderRegistry providers;

	private final ContentRetrieverContext context;

	public Langchain4jContentRetrieverController(MagicConfiguration configuration,
			ContentRetrieverProviderRegistry providers, ContentRetrieverContext context) {
		super(configuration);
		this.providers = providers;
		this.context = context;
	}

	/**
	 * @param body datasource JSON（key/provider/options），可额外带 {@code query} 指定探针问题
	 */
	@PostMapping("/langchain4j/content-retriever/datasource/test")
	@ResponseBody
	public String test(@RequestBody Map<String, Object> body) {
		try {
			Langchain4jContentRetrieverInfo info =
					JSON.parseObject(JSON.toJSONBytes(body), Langchain4jContentRetrieverInfo.class);
			String query = body.get("query") == null ? DEFAULT_QUERY : String.valueOf(body.get("query"));
			ContentRetriever retriever = providers.build(info, context);
			List<Content> contents = retriever.retrieve(Query.from(query));
			logger.info("测试 LangChain4j ContentRetriever: key={}, provider={}, 命中 {} 条",
					info.getKey(), info.getProvider(), contents == null ? 0 : contents.size());
			return "ok";
		} catch (IllegalArgumentException e) {
			return e.getMessage();
		} catch (Exception e) {
			return "测试失败: " + e.getMessage();
		}
	}
}
