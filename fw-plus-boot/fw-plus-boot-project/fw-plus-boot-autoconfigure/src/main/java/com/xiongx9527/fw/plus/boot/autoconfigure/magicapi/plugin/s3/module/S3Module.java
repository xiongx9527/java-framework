package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.MyFileStorageService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.MagicDynamicS3DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

@Slf4j
@MagicModule("s3")
public class S3Module implements DynamicAttribute<S3Module, S3Module>, DynamicModule<S3Module> {

    /**
     * 动态S3数据源管理器
     * 用于管理和获取动态创建的S3数据库工厂
     */
    private final MagicDynamicS3DataSource magicDynamicS3DataSource;

    /**
     * 文件存储服务实例
     * 用于执行S3文件存储相关操作的服务实例
     */
    private final MyFileStorageService fileStorageService;

    /**
     * 构造函数
     * 初始化S3Module实例
     *
     * @param magicDynamicS3DataSource 动态S3数据源管理器
     * @param fileStorageService       文件存储服务实例
     */
    public S3Module(MagicDynamicS3DataSource magicDynamicS3DataSource, MyFileStorageService fileStorageService) {
        this.magicDynamicS3DataSource = magicDynamicS3DataSource;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 获取文件存储服务实例
     *
     * @return 返回当前对象的文件存储服务实例
     */
    public MyFileStorageService getFileStorageService() {
        return this.fileStorageService;
    }

    /**
     * 根据脚本上下文获取动态S3模块实例
     * 该方法从脚本上下文中获取数据源key，如果key不为空则创建基于该数据源的S3Module实例，否则返回当前实例
     *
     * @param context MagicScript脚本上下文
     * @return 基于上下文中指定数据源创建的S3Module实例或当前实例
     */
    @Override
    public S3Module getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) return this;
        return new S3Module(magicDynamicS3DataSource,magicDynamicS3DataSource.getFileStorageService(dataSourceKey));
    }

    /**
     * 根据指定的key获取动态S3模块实例
     * 该方法通过key获取对应的文件存储服务，并创建一个新的S3Module实例
     *
     * @param key 文件存储服务的唯一标识符
     * @return 基于指定key对应的文件存储服务创建的S3Module实例
     */
    @Override
    public S3Module getDynamicAttribute(String key) {
        return new S3Module(magicDynamicS3DataSource, magicDynamicS3DataSource.getFileStorageService(key));
    }

}
