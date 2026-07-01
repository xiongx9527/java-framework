package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.MagicDynamicElasticSearchDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.starter.ElasticSearchConnection;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.starter.ElasticSearchIndex;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.RestClient;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.annotation.Comment;
import org.ssssssss.script.functions.DynamicAttribute;

@MagicModule("elasticsearch")
public class ElasticSearchModule implements DynamicAttribute<ElasticSearchModule, ElasticSearchModule>, DynamicModule<ElasticSearchModule>  {

	private static final String DOC = "_doc";

	/**
	 * MagicDynamicElasticSearchDataSource实例，用于处理动态ElasticSearch数据源的相关操作
	 */
	private final MagicDynamicElasticSearchDataSource magicDynamicElasticSearchDataSource;

	/**
	 * RestClient实例，用于执行REST API请求
	 */
	private final RestClient restClient;

	/**
	 * 构造函数，初始化ElasticSearch模块
	 *
	 * @param magicDynamicElasticSearchDataSource 动态ElasticSearch数据源，用于管理多个ElasticSearch连接
	 * @param restClient REST客户端，用于与ElasticSearch集群进行HTTP通信
	 */
	public ElasticSearchModule(MagicDynamicElasticSearchDataSource magicDynamicElasticSearchDataSource, RestClient restClient) {
		this.magicDynamicElasticSearchDataSource = magicDynamicElasticSearchDataSource;
		this.restClient = restClient;
	}

	/**
	 * 创建ElasticSearch连接对象
	 * @param url ElasticSearch服务的URL地址
	 * @return ElasticSearchConnection 连接对象实例
	 */
	@Comment(value = "ElasticSearch REST API")
	public ElasticSearchConnection rest(String url){
		return new ElasticSearchConnection(this.restClient, url);
	}

	/**
	 * 创建ElasticSearch索引操作对象
	 * @param indexName 索引名称
	 * @return ElasticSearchIndex 索引操作对象实例
	 */
	public ElasticSearchIndex index(String indexName){
		return new ElasticSearchIndex(this.restClient, indexName, DOC);
	}

	/**
	 * 根据上下文获取动态数据源模块
	 * 从MagicScriptContext中获取数据源键值，如果存在则返回对应的数据源模块，否则返回当前模块
	 * @param context MagicScript执行上下文
	 * @return ElasticSearchModule 动态数据源模块实例
	 */
	@Override
	public ElasticSearchModule getDynamicModule(MagicScriptContext context) {
		String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
		if (StringUtils.isEmpty(dataSourceKey)) return this;
		return new ElasticSearchModule(this.magicDynamicElasticSearchDataSource, this.magicDynamicElasticSearchDataSource.getElasticSearchRestClient(dataSourceKey));
	}

	/**
	 * 根据键值获取动态属性模块
	 * 通过键值获取对应的ElasticSearch REST客户端并创建新的模块实例
	 * @param key 数据源标识键
	 * @return ElasticSearchModule 动态属性模块实例
	 */
	@Override
	public ElasticSearchModule getDynamicAttribute(String key) {
		return new ElasticSearchModule(this.magicDynamicElasticSearchDataSource, this.magicDynamicElasticSearchDataSource.getElasticSearchRestClient(key));
	}

}
