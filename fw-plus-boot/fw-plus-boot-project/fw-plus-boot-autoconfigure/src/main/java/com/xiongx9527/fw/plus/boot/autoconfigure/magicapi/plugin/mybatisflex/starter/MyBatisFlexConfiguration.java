package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.starter;

import com.mybatisflex.core.datasource.DataSourceDecipher;
import com.mybatisflex.core.datasource.DataSourceManager;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.mybatisflex.core.logicdelete.LogicDeleteManager;
import com.mybatisflex.core.logicdelete.LogicDeleteProcessor;
import com.mybatisflex.core.table.DynamicSchemaProcessor;
import com.mybatisflex.core.table.DynamicTableProcessor;
import com.mybatisflex.core.table.TableManager;
import com.mybatisflex.core.tenant.TenantFactory;
import com.mybatisflex.core.tenant.TenantManager;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.module.MybatisFlexModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.service.MybatisFlexXmlMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.service.MybatisFlexXmlMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.service.MybatisFlexXmlMapperRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.modules.db.SQLModule;

import javax.sql.DataSource;
import java.util.Objects;

@Slf4j
@ConditionalOnProperty(prefix = "magic.api.mybatis-flex", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "com.mybatisflex.core.row.RowMapperInvoker")
@EnableConfigurationProperties({MyBatisFlexProperties.class})
@Configuration
public class MyBatisFlexConfiguration implements InitializingBean, MagicPluginConfiguration {

    private static final String JAVASCRIPT_FILENAME = "magic-mybatisflex-xml.1.0.0.iife.js";

    /**
     * MyBatis-Flex 配置类
     * 负责初始化和管理 MyBatis-Flex 的各种处理器和配置属性
     */
    private final MyBatisFlexProperties myBatisFlexProperties;

    /**
     * 数据源解密器
     * 用于解密加密的数据源配置信息
     */
    private final DataSourceDecipher dataSourceDecipher;

    /**
     * 动态表名处理器
     * 用于处理运行时动态表名的生成和替换
     */
    private final DynamicTableProcessor dynamicTableProcessor;

    /**
     * 动态 schema 处理器
     * 用于处理运行时动态 schema 的生成和替换
     */
    private final DynamicSchemaProcessor dynamicSchemaProcessor;

    /**
     * 多租户工厂
     * 用于处理多租户相关的数据隔离和租户信息管理
     */
    private final TenantFactory tenantFactory;

    /**
     * 自定义逻辑删除处理器
     * 用于处理逻辑删除相关的操作和查询条件构建
     */
    private final LogicDeleteProcessor logicDeleteProcessor;

    @Bean
    @ConditionalOnMissingBean
    public MybatisFlexXmlMapperRegistry mybatisFlexXmlMapperRegistry(
            ObjectProvider<MybatisFlexModule> moduleProvider,
            org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource dynamicDataSource,
            @Autowired(required = false) DynamicEntityRegistry dynamicEntityRegistry) {
        return new MybatisFlexXmlMapperRegistry(moduleProvider, dynamicDataSource, dynamicEntityRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public MybatisFlexXmlMagicResourceStorage mybatisFlexXmlMagicResourceStorage(MybatisFlexXmlMapperRegistry registry) {
        return new MybatisFlexXmlMagicResourceStorage(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public MybatisFlexXmlMagicDynamicRegistry mybatisFlexXmlMagicDynamicRegistry(
            MybatisFlexXmlMagicResourceStorage storage, MybatisFlexXmlMapperRegistry registry) {
        return new MybatisFlexXmlMagicDynamicRegistry(storage, registry);
    }

    /**
     * 构造函数，初始化 MyBatis-Flex 配置
     *
     * @param myBatisFlexProperties MyBatis-Flex 配置属性
     * @param dataSourceDecipherProvider 数据源解密器提供者
     * @param dynamicTableProcessorProvider 动态表名处理器提供者
     * @param dynamicSchemaProcessorProvider 动态 schema 处理器提供者
     * @param tenantFactoryProvider 多租户工厂提供者
     * @param logicDeleteProcessorProvider 逻辑删除处理器提供者
     */
    public MyBatisFlexConfiguration(MyBatisFlexProperties myBatisFlexProperties, ObjectProvider<DataSourceDecipher> dataSourceDecipherProvider, ObjectProvider<DynamicTableProcessor> dynamicTableProcessorProvider, ObjectProvider<DynamicSchemaProcessor> dynamicSchemaProcessorProvider, ObjectProvider<TenantFactory> tenantFactoryProvider, ObjectProvider<LogicDeleteProcessor> logicDeleteProcessorProvider) {
        this.myBatisFlexProperties = myBatisFlexProperties;
        //数据源解密器
        this.dataSourceDecipher = dataSourceDecipherProvider.getIfAvailable();

        //动态表名
        this.dynamicTableProcessor = dynamicTableProcessorProvider.getIfAvailable();

        //动态 schema 处理器
        this.dynamicSchemaProcessor = dynamicSchemaProcessorProvider.getIfAvailable();

        //多租户
        this.tenantFactory = tenantFactoryProvider.getIfAvailable();

        //逻辑删除处理器
        this.logicDeleteProcessor = logicDeleteProcessorProvider.getIfAvailable();
    }


    /**
     * 创建并配置MybatisFlexModule实例
     *
     * @param interceptorsProvider 拦截器数组的对象提供者
     * @param typeHandlersProvider 类型处理器数组的对象提供者
     * @param databaseIdProvider 数据库ID提供者
     * @param dynamicDataSource 魔法API动态数据源
     * @param dataSource 数据源（可选注入）
     * @return 配置好的MybatisFlexModule实例
     * @throws Exception 创建模块时可能抛出的异常
     */
    @Bean
    public  MybatisFlexModule mybatisFlexModule(ObjectProvider<Interceptor[]> interceptorsProvider,
                                                ObjectProvider<TypeHandler[]> typeHandlersProvider,
                                                ObjectProvider<DatabaseIdProvider> databaseIdProvider,
                                                ObjectProvider<SQLModule> sqlModuleProvider,
                                                ObjectProvider<MybatisFlexXmlMapperRegistry> xmlMapperRegistryProvider,
                                                org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource dynamicDataSource,
                                                @Autowired(required = false) DataSource dataSource) throws Exception {
		if (dataSource instanceof DynamicRoutingDataSource
				&& ((DynamicRoutingDataSource) dataSource).getDataSources().isEmpty()) {
			dataSource = null;
        }
        return new MybatisFlexModule(interceptorsProvider,typeHandlersProvider,databaseIdProvider,
                sqlModuleProvider,xmlMapperRegistryProvider,dynamicDataSource,this.myBatisFlexProperties,dataSource);
    }

    /**
     * 在所有属性设置完成后执行初始化配置
     * 配置包括数据源解密器、动态表名处理器、动态schema处理器、多租户工厂和逻辑删除处理器
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        //数据源解密器
        if (Objects.nonNull(this.dataSourceDecipher) && Objects.isNull(DataSourceManager.getDecipher())) {
            DataSourceManager.setDecipher(this.dataSourceDecipher);
        }

        // 动态表名配置
        if (Objects.nonNull(this.dynamicTableProcessor) && Objects.isNull(TableManager.getDynamicTableProcessor())) {
            TableManager.setDynamicTableProcessor(this.dynamicTableProcessor);
        }

        // 动态 schema 处理器配置
        if (Objects.nonNull(this.dynamicSchemaProcessor) && Objects.isNull(TableManager.getDynamicSchemaProcessor())) {
            TableManager.setDynamicSchemaProcessor(this.dynamicSchemaProcessor);
        }

        //多租户
        if (Objects.nonNull(this.tenantFactory) && Objects.isNull(TenantManager.getTenantFactory())) {
            TenantManager.setTenantFactory(this.tenantFactory);
        }

        //逻辑删除处理器
        if (Objects.nonNull(this.logicDeleteProcessor) && Objects.isNull(LogicDeleteManager.getProcessor())) {
            LogicDeleteManager.setProcessor(this.logicDeleteProcessor);
        }
    }

    @Override
    public Plugin plugin() {
        if (MyBatisFlexConfiguration.class.getClassLoader()
                .getResource("magic-editor/plugins/" + JAVASCRIPT_FILENAME) == null) {
            return new Plugin("mybatisflex-xml");
        }
        return new Plugin("mybatisflex-xml", "MagicMybatisFlexXml", JAVASCRIPT_FILENAME);
    }


}
