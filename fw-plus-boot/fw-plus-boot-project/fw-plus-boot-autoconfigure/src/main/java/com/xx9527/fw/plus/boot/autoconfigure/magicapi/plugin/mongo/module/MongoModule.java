package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.module;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MagicDynamicMongoDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

@Slf4j
@MagicModule("mongo")
public class MongoModule implements DynamicAttribute<MongoModule, MongoModule>, DynamicModule<MongoModule> {

    /**
     * 动态Mongo数据源管理器
     * 用于管理和获取动态创建的Mongo数据库工厂
     */
    private final MagicDynamicMongoDataSource magicDynamicMongoDataSource;

    /**
     * 原始Mongo模块
     * 用于执行Mongo操作的原始模块实例
     */
    private final org.ssssssss.magicapi.mongo.MongoModule mongoModule;

    /**
     * Mongo模板
     * 用于执行Mongo操作的模板对象
     */
    private final MongoTemplate mongoTemplate;

    /**
     * Mongo模块构造函数
     * 使用MongoTemplate初始化Mongo模块
     *
     * @param mongoTemplate Mongo模板对象
     * @param magicDynamicMongoDataSource 动态Mongo数据源管理器
     */
    public MongoModule(MongoTemplate mongoTemplate, MagicDynamicMongoDataSource magicDynamicMongoDataSource) {
        this.mongoTemplate = mongoTemplate;
        this.mongoModule = new org.ssssssss.magicapi.mongo.MongoModule(this.mongoTemplate);
        this.magicDynamicMongoDataSource = magicDynamicMongoDataSource;
    }

    /**
     * Mongo模块构造函数
     * 使用MongoDatabaseFactory初始化Mongo模块
     *
     * @param mongoDatabaseFactory Mongo数据库工厂
     * @param magicDynamicMongoDataSource 动态Mongo数据源管理器
     */
    public MongoModule(MongoDatabaseFactory mongoDatabaseFactory, MagicDynamicMongoDataSource magicDynamicMongoDataSource) {
        this.mongoTemplate = new MongoTemplate(mongoDatabaseFactory);
        this.mongoModule =  new org.ssssssss.magicapi.mongo.MongoModule(this.mongoTemplate);
        this.magicDynamicMongoDataSource = magicDynamicMongoDataSource;
    }

    /**
     * 获取原始Mongo模块对象
     * 返回当前模块使用的原始MongoModule实例
     *
     * @return 原始MongoModule实例
     */
    public org.ssssssss.magicapi.mongo.MongoModule getMongoModule() {
        return this.mongoModule;
    }
    
    /**
     * 获取Mongo模板对象
     * 返回当前模块使用的MongoTemplate实例
     *
     * @return MongoTemplate实例
     */
    public MongoTemplate getMongoTemplate() {
        return this.mongoTemplate;
    }

    /**
     * 获取动态Mongo模块实例
     * 根据脚本上下文中的数据源配置获取对应的Mongo数据库工厂，并创建新的MongoModule实例
     *
     * @param context MagicScript脚本上下文，包含数据源配置信息
     * @return 包含指定数据库工厂的MongoModule实例，如果未指定数据源则返回当前实例
     */
    @Override
    public MongoModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) return this;
        return new MongoModule(magicDynamicMongoDataSource.getMongoDatabaseFactory(dataSourceKey), magicDynamicMongoDataSource);
    }

    /**
     * 根据指定key获取动态Mongo模块实例
     * 根据key从动态数据源中获取对应的Mongo数据库工厂，并创建新的MongoModule实例
     *
     * @param key Mongo数据库工厂的唯一标识符
     * @return 包含指定数据库工厂的MongoModule实例
     */
    @Override
    public MongoModule getDynamicAttribute(String key) {
        return new MongoModule(magicDynamicMongoDataSource.getMongoDatabaseFactory(key), magicDynamicMongoDataSource);
    }

}
