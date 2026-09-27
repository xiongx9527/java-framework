package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.MagicAPIController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options.MagicJdbcClassOptionProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options.MagicJdbcClassOptionProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service.DataSourceBuilder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service.DruidDataSourceBuilder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service.MyDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.filter.auth.AuthConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.Lock;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dify.DifyModuleConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.LockModuleConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.MagicDataSourceDecorator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.starter.MagicAPIDbTxConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.seata.starter.MagicAPISeataConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.starter.MyBatisFlexConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.starter.MybatisPlusModuleConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.template.TemplateModuleConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.trace.TraceModuleConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.uid.UidModuleConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.sse.SseModuleConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.MagicApiPluginAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.ParamProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.ResultProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.key.MagicApiRequestKeyResolver;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat.DefaultRepeatSubmitKeySuffixProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat.RepeatSubmitKeyResolver;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat.RepeatSubmitKeySuffixProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat.RepeatSubmitRequestInterceptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.sentinel.MagicApiSentinelGuardRequestInterceptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.sentinel.MagicApiSentinelResourceResolver;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.sentinel.MagicApiSentinelRuleManager;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.resource.MyDefaultMagicResourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.ssssssss.magicapi.core.config.*;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.datasource.model.DataSourceInfo;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;
import org.ssssssss.magicapi.datasource.service.DataSourceMagicDynamicRegistry;

import java.util.*;

@Slf4j
@ConditionalOnProperty(prefix = "fw.plus.magicapi", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({DataSourceProperties.class, MagicJdbcClassOptionProperties.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name={"org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration"})
@Configuration
@Import({MagicAPIController.class,
		 AuthConfiguration.class,
         UidModuleConfiguration.class,
		 LockModuleConfiguration.class,
		 MyBatisFlexConfiguration.class,
		 MybatisPlusModuleConfiguration.class,
		 TemplateModuleConfiguration.class,
         MagicApiPluginAutoConfiguration.class,
		 DifyModuleConfiguration.class,
		 SseModuleConfiguration.class,
		 TraceModuleConfiguration.class,
		 MagicAPIDbTxConfiguration.class,
		 MagicAPISeataConfiguration.class})
public class MagicAPIWebMvcAutoConfiguration implements ApplicationRunner {

	/**
	 * 魔法资源存储提供者，用于获取魔法资源存储列表
	 * 该字段是一个ObjectProvider实例，提供了延迟获取和依赖注入的功能，
	 * 可以在运行时动态获取类型为MagicResourceStorage的Bean列表，
	 * 其中泛型参数限定为MagicEntity的子类型
	 */
	private final ObjectProvider<List<MagicResourceStorage<? extends MagicEntity>>> magicResourceStoragesProvider;

	/**
	 * 应用上下文对象，提供Spring容器的核心功能
	 * 通过该字段可以访问Spring应用上下文中的各种Bean和服务，
	 * 支持Bean的获取、环境信息的读取、事件发布等核心ApplicationContext功能
	 */
	private final ApplicationContext applicationContext;

	/**
	 * MagicAPI配置属性
	 * 使用@Lazy注解实现延迟加载，在需要时才进行注入
	 */
	@Autowired
	@Lazy
	private MagicAPIProperties properties;

	/**
	 * 请求映射处理器映射器
	 * 负责将请求URL映射到相应的处理器方法
	 * 使用@Lazy注解实现延迟加载
	 */
	@Autowired
	@Lazy
	private RequestMappingHandlerMapping requestMappingHandlerMapping;

	/**
	 * MagicAPI核心配置类
	 * 包含MagicAPI的主要配置信息和运行时设置
	 * 使用@Lazy注解实现延迟加载
	 */
	@Autowired
	@Lazy
	private MagicConfiguration configuration;


    /**
     * 构造函数，用于初始化MagicAPIWebMvcAutoConfiguration
     *
     * @param magicResourceStoragesProvider 提供MagicResourceStorage列表的对象提供者，
     *                                      MagicResourceStorage是泛型类，泛型参数继承自MagicEntity
     * @param applicationContext Spring应用上下文，用于访问应用的bean和配置信息
     */
    public MagicAPIWebMvcAutoConfiguration(ObjectProvider<List<MagicResourceStorage<? extends MagicEntity>>> magicResourceStoragesProvider, ApplicationContext applicationContext) {
        this.magicResourceStoragesProvider = magicResourceStoragesProvider;
        this.applicationContext = applicationContext;
    }

	/**
	 * 配置并创建一个ParamProvider的Bean实例
	 *
	 * @return ParamProvider的实例，用于处理参数提供
	 */
	@Bean
	public ParamProvider paramProvider() {
		return new ParamProvider();
	}

	@Bean
	@ConditionalOnMissingBean(name = "defaultRepeatSubmitKeySuffixProvider")
	public RepeatSubmitKeySuffixProvider defaultRepeatSubmitKeySuffixProvider() {
		return new DefaultRepeatSubmitKeySuffixProvider();
	}

	@Bean
	@ConditionalOnMissingBean
	public RepeatSubmitKeyResolver repeatSubmitKeyResolver(ApplicationContext applicationContext,
														   @Qualifier("defaultRepeatSubmitKeySuffixProvider")
														   RepeatSubmitKeySuffixProvider defaultSuffixProvider) {
		return new RepeatSubmitKeyResolver(applicationContext, defaultSuffixProvider);
	}

	@Bean
	@ConditionalOnMissingBean
	public RepeatSubmitRequestInterceptor repeatSubmitRequestInterceptor(Lock lock,
																		 org.ssssssss.magicapi.core.interceptor.ResultProvider resultProvider,
																		 RepeatSubmitKeyResolver keyResolver) {
		return new RepeatSubmitRequestInterceptor(lock, resultProvider, keyResolver);
	}

	@Bean
	@ConditionalOnMissingBean
	public MagicApiRequestKeyResolver magicApiRequestKeyResolver() {
		return new MagicApiRequestKeyResolver();
	}

	@Bean
	@ConditionalOnMissingBean
	public MagicApiSentinelResourceResolver magicApiSentinelResourceResolver() {
		return new MagicApiSentinelResourceResolver();
	}

	@Bean
	@ConditionalOnMissingBean
	public MagicApiSentinelRuleManager magicApiSentinelRuleManager() {
		return new MagicApiSentinelRuleManager();
	}

	@Bean
	@ConditionalOnMissingBean
	public MagicApiSentinelGuardRequestInterceptor magicApiSentinelGuardRequestInterceptor(
			org.ssssssss.magicapi.core.interceptor.ResultProvider resultProvider,
			MagicApiRequestKeyResolver keyResolver,
			MagicApiSentinelResourceResolver resourceResolver,
			MagicApiSentinelRuleManager ruleManager) {
		return new MagicApiSentinelGuardRequestInterceptor(resultProvider, keyResolver, resourceResolver, ruleManager);
	}


	    /**
	     * 创建ResultProvider Bean实例
     *
     * 该方法用于向Spring容器中注册ResultProvider组件，用于统一处理MagicAPI的响应结果格式
     *
     * @return ResultProvider实例，用于构建统一的API响应结果
     */
	/**
	 * Boot 自定义的 ResultProvider。bean 名加前缀以避免与 magic-api starter 的
	 * {@code MagicModuleConfiguration#resultProvider()}（返回 core interface）撞名：
	 * 两者同时在 classpath 时同名 bean 会触发 BeanDefinitionOverrideException。
	 * 需要该实现的地方用 {@code @Qualifier("fwPlusMagicResultProvider")} 注入。
	 */
	@Bean
    public ResultProvider fwPlusMagicResultProvider() {
        return new ResultProvider(properties);
    }

	/**
	 * 创建并配置Druid数据源构建器Bean
	 *
	 * @return DruidDataSourceBuilder 数据源构建器实例
	 *
	 * 该方法用于创建Druid数据源的构建器对象，通过Spring的Bean注解将其注册为名为
	 * "com.alibaba.druid.pool.DruidDataSource"的Bean。同时使用条件注解确保只有在
	 * classpath中存在DruidDataSource类时才会创建该Bean。
	 */
	@Bean(name="com.alibaba.druid.pool.DruidDataSource")
	@ConditionalOnClass(name={"com.alibaba.druid.pool.DruidDataSource"})
	public DruidDataSourceBuilder magicDynamicDataSourceBuilder() {
		return new DruidDataSourceBuilder();
	}

	@Bean
	@ConditionalOnMissingBean(MagicJdbcClassOptionProvider.class)
	public MagicJdbcClassOptionProvider magicJdbcClassOptionProvider(MagicJdbcClassOptionProperties properties,
																	 Map<String, DataSourceBuilder> dataSourceBuilderMap) {
		return new MagicJdbcClassOptionProvider(properties, dataSourceBuilderMap, MagicAPIWebMvcAutoConfiguration.class.getClassLoader());
	}

	/**
	 * 创建数据源动态注册器Bean
	 *
	 * @param magicResourceStorage 数据源信息存储器
	 * @param magicDynamicDataSource 动态数据源实例
	 * @param dataSourceBuilderMap 数据源构建器映射表
	 * @return 数据源动态注册器实例
	 */
	@Bean
	@ConditionalOnMissingBean
	public DataSourceMagicDynamicRegistry magicDynamicRegistry(MagicResourceStorage<DataSourceInfo> magicResourceStorage,
															   MagicDynamicDataSource magicDynamicDataSource,
															   Map<String, DataSourceBuilder> dataSourceBuilderMap,
															   List<MagicDataSourceDecorator> dataSourceDecorators) {
		// 尝试加载支持的数据源类型，如果找到可用的则创建对应的注册器
		for (String className : MyDataSourceMagicDynamicRegistry.DATA_SOURCE_TYPE_NAMES) {
			try {
				Class.forName(className);
				return new MyDataSourceMagicDynamicRegistry(magicResourceStorage, magicDynamicDataSource,
						dataSourceBuilderMap, dataSourceDecorators);
			} catch (ClassNotFoundException e) {
			}
		}
		// 如果没有找到支持的数据源类型，则返回空注册器
		return new EmptyDataSourceMagicDynamicRegistry(magicResourceStorage, magicDynamicDataSource);
	}

	/**
	 * 创建MagicResourceService实例的Bean工厂方法
	 *
	 * @param workspace 工作空间资源对象，用于提供资源配置和管理功能
	 * @return 返回配置好的MagicResourceService实例
	 */
	@Bean
	@ConditionalOnMissingBean
	public MagicResourceService magicResourceService(org.ssssssss.magicapi.core.resource.Resource workspace) {
		return new MyDefaultMagicResourceService(workspace, magicResourceStoragesProvider.getObject(), applicationContext);
	}

    /**
     * 应用启动完成后执行的回调方法
     * 
     * @param args 应用启动参数
     * @throws Exception 如果执行过程中发生错误
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
    }

	/**
	 * 自定义数据源魔法动态注册器类
	 * 继承自DataSourceMagicDynamicRegistry，用于处理数据源的动态注册和管理
	 */
	class EmptyDataSourceMagicDynamicRegistry extends DataSourceMagicDynamicRegistry {

		/**
		 * 构造函数
		 * @param magicResourceStorage 魔法资源存储器，用于存储DataSourceInfo类型的资源信息
		 * @param magicDynamicDataSource 魔法动态数据源，用于实现数据源的动态切换和管理
		 */
		public EmptyDataSourceMagicDynamicRegistry(MagicResourceStorage<DataSourceInfo> magicResourceStorage, MagicDynamicDataSource magicDynamicDataSource) {
			super(magicResourceStorage, magicDynamicDataSource);
		}

		/**
		 * 文件事件监听处理器
		 * 当接收到文件事件且事件类型为空字符串时触发此方法
		 * @param event 文件事件对象，包含文件变化的相关信息
		 */
		@EventListener(condition = "#event.type == ''")
		@Override
		public void onFileEvent(FileEvent event) {
		}

		/**
		 * 获取默认的数据源映射列表
		 * @return 返回空的DataSourceInfo列表作为默认映射配置
		 */
		@Override
		public List<DataSourceInfo> defaultMappings() {
			return new ArrayList<>();
		}
	}


}
