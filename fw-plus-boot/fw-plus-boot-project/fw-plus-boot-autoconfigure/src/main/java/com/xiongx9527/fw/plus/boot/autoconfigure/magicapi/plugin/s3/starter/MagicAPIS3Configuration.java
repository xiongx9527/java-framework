package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.MyFileStorageService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.MagicDynamicS3DataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.module.S3Module;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.options.MagicS3PlatformOptionProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.options.MagicS3PlatformOptionProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.service.S3DataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.service.S3DataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.web.MagicS3Controller;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.recorder.DefaultFileRecorder;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;
import org.dromara.x.file.storage.core.tika.DefaultTikaFactory;
import org.dromara.x.file.storage.core.tika.TikaContentTypeDetect;
import org.dromara.x.file.storage.core.tika.TikaFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

@Slf4j
@ConditionalOnProperty(prefix = "magic-api.plugin.s3", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.dromara.x.file.storage.core.FileStorageService"})
@EnableConfigurationProperties(MagicS3PlatformOptionProperties.class)
@Configuration
public class MagicAPIS3Configuration implements MagicPluginConfiguration {

    private final ObjectProvider<MagicS3PlatformOptionProvider> magicS3PlatformOptionProvider;

    private final MagicS3PlatformOptionProperties magicS3PlatformOptionProperties;

    public MagicAPIS3Configuration(ObjectProvider<MagicS3PlatformOptionProvider> magicS3PlatformOptionProvider,
                                   MagicS3PlatformOptionProperties magicS3PlatformOptionProperties) {
        this.magicS3PlatformOptionProvider = magicS3PlatformOptionProvider;
        this.magicS3PlatformOptionProperties = magicS3PlatformOptionProperties;
    }

    /**
     * 创建FileRecorder bean实例
     *
     * 当Spring容器中不存在FileRecorder类型的bean时，创建并返回一个默认的FileRecorder实现
     * 该方法会在没有找到FileRecorder实现类时记录警告日志，提示文件上传功能可能受限
     *
     * @return FileRecorder 返回默认的文件记录器实现类实例
     */
    @Bean
    @ConditionalOnMissingBean(FileRecorder.class)
    public FileRecorder fileRecorder() {
        // 警告用户没有找到FileRecorder的自定义实现，可能导致部分功能无法正常使用
        log.warn("没有找到 FileRecorder 的实现类，文件上传之外的部分功能无法正常使用，必须实现该接口才能使用完整功能！");
        return new DefaultFileRecorder();
    }

    /**
     * Tika 工厂类型，用于识别上传的文件的 MINE
     */
    @Bean
    @ConditionalOnMissingBean(TikaFactory.class)
    public TikaFactory tikaFactory() {
        return new DefaultTikaFactory();
    }

    /**
     * 识别文件的 MIME 类型
     */
    @Bean
    @ConditionalOnMissingBean(ContentTypeDetect.class)
    public ContentTypeDetect contentTypeDetect(TikaFactory tikaFactory) {
        return new TikaContentTypeDetect(tikaFactory);
    }

    /**
     * 创建S3数据源信息存储Bean
     * 用于存储和管理S3数据源的配置信息
     *
     * @return S3DataSourceInfoMagicResourceStorage S3数据源信息存储实例
     */
    @Bean
    public S3DataSourceInfoMagicResourceStorage s3DataSourceInfoMagicResourceStorage() {
        return new S3DataSourceInfoMagicResourceStorage();
    }

    /**
     * 创建S3数据源动态注册Bean
     * 用于动态注册和管理S3数据源
     *
     * @param s3DataSourceInfoMagicResourceStorage S3数据源信息存储服务
     * @param magicDynamicS3DataSource 动态S3数据源管理器
     * @return S3DataSourceMagicDynamicRegistry S3数据源动态注册实例
     */
    @Bean
    public S3DataSourceMagicDynamicRegistry s3DataSourceMagicDynamicRegistry(S3DataSourceInfoMagicResourceStorage s3DataSourceInfoMagicResourceStorage,
                                                                             MagicDynamicS3DataSource magicDynamicS3DataSource) {
        return new S3DataSourceMagicDynamicRegistry(s3DataSourceInfoMagicResourceStorage,magicDynamicS3DataSource);
    }

    /**
     * 创建MagicDynamicS3DataSource Bean实例
     * 用于动态管理S3数据源和文件存储服务
     *
     * @param fileRecorder 文件记录器，用于记录文件操作信息
     * @param contentTypeDetect 内容类型检测器，用于检测文件的MIME类型
     * @return MagicDynamicS3DataSource实例
     */
    @Bean
    public MagicDynamicS3DataSource magicDynamicS3DataSource(FileRecorder fileRecorder, ContentTypeDetect contentTypeDetect) {
        return new MagicDynamicS3DataSource(fileRecorder, contentTypeDetect);
    }

    @Bean
    @ConditionalOnMissingBean(MagicS3PlatformOptionProvider.class)
    public MagicS3PlatformOptionProvider magicS3PlatformOptionProvider(MagicS3PlatformOptionProperties properties) {
        return new MagicS3PlatformOptionProvider(properties, MagicAPIS3Configuration.class.getClassLoader());
    }

    /**
     * 创建S3模块Bean
     * 用于在MagicAPI中提供S3文件存储功能
     *
     * @param magicDynamicS3DataSource 动态S3数据源管理器
     * @param fileStorageService       文件存储服务（可选）
     * @return S3Module S3模块实例
     */
    @Bean
    public S3Module s3Module(MagicDynamicS3DataSource magicDynamicS3DataSource,@Autowired(required = false) FileStorageService fileStorageService) {
        return new S3Module(magicDynamicS3DataSource,new MyFileStorageService(fileStorageService,new S3DataSourceInfo()));
    }

    /**
     * 定义插件信息
     * 返回当前S3插件的元数据信息
     *
     * @return Plugin 插件信息对象，包含插件名称、显示名称和JavaScript文件路径
     */
    @Override
    public Plugin plugin() {
        Plugin plugin = new Plugin("s3", "MagicS3", "magic-s3.1.0.0.iife.js");
        String adminEnabled = SpringUtils.getAppConfig("magic-api.plugin.s3.admin.enabled", "false");
        plugin.setAdminEnabled(Boolean.parseBoolean(adminEnabled));
        return plugin;
    }


    /**
     * 控制器注册方法
     * 用于在MagicAPI中注册S3相关的控制器
     * 
     * @return MagicControllerRegister 控制器注册器，用于注册MagicS3Controller控制器
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicS3Controller(configuration, getMagicS3PlatformOptionProvider()));
    }

    private MagicS3PlatformOptionProvider getMagicS3PlatformOptionProvider() {
        MagicS3PlatformOptionProvider provider = magicS3PlatformOptionProvider == null ? null : magicS3PlatformOptionProvider.getIfAvailable();
        return provider == null
                ? new MagicS3PlatformOptionProvider(magicS3PlatformOptionProperties, MagicAPIS3Configuration.class.getClassLoader())
                : provider;
    }

}
