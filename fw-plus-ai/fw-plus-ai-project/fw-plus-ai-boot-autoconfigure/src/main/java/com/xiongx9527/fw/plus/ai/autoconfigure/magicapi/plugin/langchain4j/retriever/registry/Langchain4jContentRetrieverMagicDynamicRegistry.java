package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverProviderRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.Langchain4jContentRetrieverInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.MagicDynamicLangchain4jContentRetrieverDataSource;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code langchain4j-content-retriever} 动态注册器：控制台新增/修改/删除资源即增删 ContentRetriever 实例。
 *
 * <p>注册顺序敏感的资源（如引用向量库的 retriever）构建失败只记日志不注册，
 * 待依赖的数据源保存后重新触发本类型事件即可重建。</p>
 */
public class Langchain4jContentRetrieverMagicDynamicRegistry
		extends AbstractMagicDynamicRegistry<Langchain4jContentRetrieverInfo> {

	private static final Logger logger =
			LoggerFactory.getLogger(Langchain4jContentRetrieverMagicDynamicRegistry.class);

	private final MagicDynamicLangchain4jContentRetrieverDataSource dataSource;

	private final ContentRetrieverProviderRegistry providers;

	private final ContentRetrieverContext context;

	/** Spring 容器中的默认 ContentRetriever（core 层 langchain4j 自动配置注入），可能不存在 */
	private final ObjectProvider<ContentRetriever> defaultInstance;

	public Langchain4jContentRetrieverMagicDynamicRegistry(
			MagicResourceStorage<Langchain4jContentRetrieverInfo> magicResourceStorage,
			MagicDynamicLangchain4jContentRetrieverDataSource dataSource,
			ContentRetrieverProviderRegistry providers,
			ContentRetrieverContext context,
			ObjectProvider<ContentRetriever> defaultInstance) {
		super(magicResourceStorage);
		this.dataSource = dataSource;
		this.providers = providers;
		this.context = context;
		this.defaultInstance = defaultInstance;
	}

	@Override
	protected boolean register(MappingNode<Langchain4jContentRetrieverInfo> mappingNode) {
		Langchain4jContentRetrieverInfo entity = mappingNode.getEntity();
		dataSource.evict(entity.getKey());
		if (!entity.isEnabled()) {
			return true;
		}
		ContentRetriever retriever;
		try {
			retriever = providers.build(entity, context);
		} catch (Exception e) {
			logger.warn("构建 LangChain4j ContentRetriever 失败: key={}, provider={}",
					entity.getKey(), entity.getProvider(), e);
			return false;
		}
		boolean registered = super.register(mappingNode);
		if (registered) {
			// 登记成功后才放入实例：避免「实例已可取用但映射登记失败」的不一致状态
			dataSource.put(entity.getKey(), retriever);
		}
		return registered;
	}

	@Override
	protected void unregister(MappingNode<Langchain4jContentRetrieverInfo> mappingNode) {
		dataSource.evict(mappingNode.getEntity().getKey());
		super.unregister(mappingNode);
	}

	@EventListener(condition = "#event.type == 'langchain4j-content-retriever'")
	public void onFileEvent(FileEvent event) {
		processEvent(event);
	}

	@EventListener(condition = "#event.type == 'langchain4j-content-retriever'")
	public void onGroupEvent(GroupEvent event) {
		processEvent(event);
	}

	/** 默认 ContentRetriever（Spring 注入，key=default，只读展示）+ 控制台配置的数据源，一并暴露 */
	@Override
	public List<Langchain4jContentRetrieverInfo> defaultMappings() {
		List<Langchain4jContentRetrieverInfo> result = new ArrayList<>();
		if (defaultInstance != null && defaultInstance.getIfAvailable() != null) {
			Langchain4jContentRetrieverInfo info = new Langchain4jContentRetrieverInfo();
			info.setKey("default");
			info.setName("默认数据源");
			info.setEnabled(true);
			result.add(info);
		}
		result.addAll(mappings());
		return result;
	}
}
