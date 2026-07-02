package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.MultipartFileWrapperAdapter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.MyFileStorageService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.SpringFileStorageProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.SpringFileStorageProperties.SpringLocalConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.SpringFileStorageProperties.SpringLocalPlusConfig;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.FileStorageServiceBuilder;
import org.dromara.x.file.storage.core.file.FileWrapperAdapter;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.FileStorageClientFactory;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;
import org.springframework.beans.factory.DisposableBean;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.dromara.x.file.storage.core.FileStorageServiceBuilder.doesNotExistClass;

@Slf4j
public class MagicDynamicS3DataSource implements DisposableBean {

    /**
     * 存储文件存储服务的线程安全映射表
     * 用于缓存和管理多个S3文件存储服务实例
     * key为数据源唯一标识符，value为对应的FileStorageService实例
     */
    private static final Map<String, MyFileStorageService> fileStorageServiceMap = new ConcurrentHashMap<>();

    /**
     * 存储S3数据源信息的线程安全映射表
     * 用于缓存和管理多个S3数据源的配置信息
     * key为数据源唯一标识符，value为对应的S3DataSourceInfo实例
     */
    private static final Map<String, S3DataSourceInfo> d3DataSourceInfooMap = new ConcurrentHashMap<>();

    /**
     * 文件记录器，用于记录和管理文件操作相关信息
     */
    private final FileRecorder fileRecorder;

    /**
     * 内容类型检测器，用于检测文件的MIME类型
     */
    private final ContentTypeDetect contentTypeDetect;

    /**
     * 构造函数，初始化MagicDynamicS3DataSource实例
     *
     * @param fileRecorder 文件记录器，用于记录文件操作
     * @param contentTypeDetect 内容类型检测器，用于检测文件的MIME类型
     */
    public MagicDynamicS3DataSource(FileRecorder fileRecorder, ContentTypeDetect contentTypeDetect) {
        this.fileRecorder = fileRecorder;
        this.contentTypeDetect = contentTypeDetect;
    }

    /**
     * 构建并注册文件存储服务
     * 根据提供的数据源信息创建文件存储服务实例，并将其注册到内部映射表中
     *
     * @param dataSourceInfo S3数据源信息，包含创建文件存储服务所需的所有配置信息
     */
    public void buildFileStorageService(S3DataSourceInfo dataSourceInfo) {
        MyFileStorageService fileStorageService = createFileStorageService(dataSourceInfo);
        this.fileStorageServiceMap.put(dataSourceInfo.getKey(), fileStorageService);
    }


    /**
     * 根据S3数据源信息创建Spring文件存储属性配置
     * 该方法根据platform类型将S3DataSourceInfo中的配置信息转换为相应的SpringFileStorageProperties对象
     *
     * @param dataSourceInfo S3数据源信息
     * @return SpringFileStorageProperties Spring文件存储属性配置
     */
    private SpringFileStorageProperties createProperties(S3DataSourceInfo dataSourceInfo) {
        SpringFileStorageProperties properties = new SpringFileStorageProperties();
        
        String platform = dataSourceInfo.getPlatform();
        platform = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.platform",dataSourceInfo.getKey()), platform);
        if (platform == null) {
            platform = "minio";
        }
        
        // 解析 other 字段中的额外属性
        Map<String, Object> otherProperties = new HashMap<>();
        String other = dataSourceInfo.getOther();
        other = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.other",dataSourceInfo.getKey()), other);
        if (StringUtils.isNotBlank(other)) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                otherProperties = objectMapper.readValue(other, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                // 如果解析失败，忽略错误，使用默认配置
                otherProperties = new HashMap<>();
            }
        }
        
        // 设置默认平台
        properties.setDefaultPlatform(platform);
        
        // 应用 other 中的额外属性到 SpringFileStorageProperties 对象
        setAdditionalProperties(properties, otherProperties);
        String accessKey = dataSourceInfo.getAccessKey();
        accessKey = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.accessKey",dataSourceInfo.getKey()), accessKey);
        String secretKey = dataSourceInfo.getSecretKey();
        secretKey = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.secretKey",dataSourceInfo.getKey()), secretKey);
        String  endPoint = dataSourceInfo.getEndPoint();
        endPoint = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.endPoint",dataSourceInfo.getKey()), endPoint);
        String domain = dataSourceInfo.getDomain();
        domain = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.domain",dataSourceInfo.getKey()), domain);
        String bucketName = dataSourceInfo.getBucketName();
        bucketName = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.bucketName",dataSourceInfo.getKey()), bucketName);
        String basePath = dataSourceInfo.getBasePath();
        basePath = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.basePath",dataSourceInfo.getKey()), basePath);
        String defaultAcl = dataSourceInfo.getDefaultAcl();
        defaultAcl = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.defaultAcl",dataSourceInfo.getKey()), defaultAcl);

        String enableStorageStr = dataSourceInfo.getEnableStorage() ;
        enableStorageStr = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.enableStorage",dataSourceInfo.getKey()), enableStorageStr);
        boolean enableStorage = Boolean.parseBoolean(enableStorageStr);
        switch (platform.toLowerCase()) {
            case "minio":
                List<SpringFileStorageProperties.SpringMinioConfig> minioConfigs = new ArrayList<>();
                SpringFileStorageProperties.SpringMinioConfig minioConfig = new SpringFileStorageProperties.SpringMinioConfig();
                minioConfig.setPlatform(platform);
                minioConfig.setAccessKey(accessKey);
                minioConfig.setSecretKey(secretKey);
                minioConfig.setEndPoint(endPoint);
                minioConfig.setBucketName(bucketName);
                minioConfig.setDomain(domain);
                minioConfig.setBasePath(basePath);
                minioConfig.setEnableStorage(enableStorage);
                //minioConfig.setDefaultAcl(defaultAcl);
                // 应用 other 中的额外属性到平台配置对象
                setAdditionalProperties(minioConfig, otherProperties);
                
                minioConfigs.add(minioConfig);
                properties.setMinio(minioConfigs);
                break;
                
            case "amazon-s3":
                List<SpringFileStorageProperties.SpringAmazonS3Config> amazonS3Configs = new ArrayList<>();
                SpringFileStorageProperties.SpringAmazonS3Config s3Config = new SpringFileStorageProperties.SpringAmazonS3Config();
                s3Config.setPlatform(platform);
                s3Config.setAccessKey(accessKey);
                s3Config.setSecretKey(secretKey);
                s3Config.setEndPoint(endPoint);
                s3Config.setBucketName(bucketName);
                s3Config.setDomain(domain);
                s3Config.setBasePath(basePath);
                s3Config.setEnableStorage(enableStorage);
                s3Config.setDefaultAcl(defaultAcl);
                
                // 应用 other 中的额外属性到平台配置对象
                setAdditionalProperties(s3Config, otherProperties);
                
                amazonS3Configs.add(s3Config);
                properties.setAmazonS3(amazonS3Configs);
                break;
                
            case "huawei-obs":
                List<SpringFileStorageProperties.SpringHuaweiObsConfig> huaweiObsConfigs = new ArrayList<>();
                SpringFileStorageProperties.SpringHuaweiObsConfig huaweiObsConfig = new SpringFileStorageProperties.SpringHuaweiObsConfig();
                huaweiObsConfig.setPlatform(platform);
                huaweiObsConfig.setAccessKey(accessKey);
                huaweiObsConfig.setSecretKey(secretKey);
                huaweiObsConfig.setEndPoint(endPoint);
                huaweiObsConfig.setBucketName(bucketName);
                huaweiObsConfig.setDomain(domain);
                huaweiObsConfig.setBasePath(basePath);
                huaweiObsConfig.setEnableStorage(enableStorage);
                huaweiObsConfig.setDefaultAcl(defaultAcl);
                
                // 应用 other 中的额外属性到平台配置对象
                setAdditionalProperties(huaweiObsConfig, otherProperties);
                
                huaweiObsConfigs.add(huaweiObsConfig);
                properties.setHuaweiObs(huaweiObsConfigs);
                break;
                
            case "aliyun-oss":
                List<SpringFileStorageProperties.SpringAliyunOssConfig> aliyunOssConfigs = new ArrayList<>();
                SpringFileStorageProperties.SpringAliyunOssConfig aliyunOssConfig = new SpringFileStorageProperties.SpringAliyunOssConfig();
                aliyunOssConfig.setPlatform(platform);
                aliyunOssConfig.setAccessKey(accessKey);
                aliyunOssConfig.setSecretKey(secretKey);
                aliyunOssConfig.setEndPoint(endPoint);
                aliyunOssConfig.setBucketName(bucketName);
                aliyunOssConfig.setDomain(domain);
                aliyunOssConfig.setBasePath(basePath);
                aliyunOssConfig.setEnableStorage(enableStorage);
                aliyunOssConfig.setDefaultAcl(defaultAcl);
                
                // 应用 other 中的额外属性到平台配置对象
                setAdditionalProperties(aliyunOssConfig, otherProperties);
                
                aliyunOssConfigs.add(aliyunOssConfig);
                properties.setAliyunOss(aliyunOssConfigs);
                break;
                
            case "qiniu-kodo":
                List<SpringFileStorageProperties.SpringQiniuKodoConfig> qiniuKodoConfigs = new ArrayList<>();
                SpringFileStorageProperties.SpringQiniuKodoConfig qiniuKodoConfig = new SpringFileStorageProperties.SpringQiniuKodoConfig();
                qiniuKodoConfig.setPlatform(platform);
                qiniuKodoConfig.setAccessKey(accessKey);
                qiniuKodoConfig.setSecretKey(secretKey);
                //qiniuKodoConfig.setEndPoint(endPoint);
                qiniuKodoConfig.setBucketName(bucketName);
                qiniuKodoConfig.setDomain(domain);
                qiniuKodoConfig.setBasePath(basePath);
                qiniuKodoConfig.setEnableStorage(enableStorage);
                //qiniuKodoConfig.setDefaultAcl(defaultAcl);
                
                // 应用 other 中的额外属性到平台配置对象
                setAdditionalProperties(qiniuKodoConfig, otherProperties);
                
                qiniuKodoConfigs.add(qiniuKodoConfig);
                properties.setQiniuKodo(qiniuKodoConfigs);
                break;
                
            case "tencent-cos":
                List<SpringFileStorageProperties.SpringTencentCosConfig> tencentCosConfigs = new ArrayList<>();
                SpringFileStorageProperties.SpringTencentCosConfig tencentCosConfig = new SpringFileStorageProperties.SpringTencentCosConfig();
                tencentCosConfig.setPlatform(platform);
                tencentCosConfig.setSecretId(accessKey);
                tencentCosConfig.setSecretKey(secretKey);
                tencentCosConfig.setRegion(endPoint);
                tencentCosConfig.setBucketName(bucketName);
                tencentCosConfig.setDomain(domain);
                tencentCosConfig.setBasePath(basePath);
                tencentCosConfig.setEnableStorage(enableStorage);
                tencentCosConfig.setDefaultAcl(defaultAcl);
                
                // 应用 other 中的额外属性到平台配置对象
                setAdditionalProperties(tencentCosConfig, otherProperties);
                
                tencentCosConfigs.add(tencentCosConfig);
                properties.setTencentCos(tencentCosConfigs);
                break;
                
            case "baidu-bos":
                List<SpringFileStorageProperties.SpringBaiduBosConfig> baiduBosConfigs = new ArrayList<>();
                SpringFileStorageProperties.SpringBaiduBosConfig baiduBosConfig = new SpringFileStorageProperties.SpringBaiduBosConfig();
                baiduBosConfig.setPlatform(platform);
                baiduBosConfig.setAccessKey(accessKey);
                baiduBosConfig.setSecretKey(secretKey);
                baiduBosConfig.setEndPoint(endPoint);
                baiduBosConfig.setBucketName(bucketName);
                baiduBosConfig.setDomain(domain);
                baiduBosConfig.setBasePath(basePath);
                baiduBosConfig.setEnableStorage(enableStorage);
                baiduBosConfig.setDefaultAcl(defaultAcl);
                
                // 应用 other 中的额外属性到平台配置对象
                setAdditionalProperties(baiduBosConfig, otherProperties);
                
                baiduBosConfigs.add(baiduBosConfig);
                properties.setBaiduBos(baiduBosConfigs);
                break;
                
            case "upyun-uss":
                List<SpringFileStorageProperties.SpringUpyunUssConfig> upyunUssConfigs = new ArrayList<>();
                SpringFileStorageProperties.SpringUpyunUssConfig upyunUssConfig = new SpringFileStorageProperties.SpringUpyunUssConfig();
                upyunUssConfig.setPlatform(platform);
                upyunUssConfig.setUsername(accessKey);
                upyunUssConfig.setPassword(secretKey);
                //upyunUssConfig.setEndPoint(endPoint);
                upyunUssConfig.setBucketName(bucketName);
                upyunUssConfig.setDomain(domain);
                upyunUssConfig.setBasePath(basePath);
                upyunUssConfig.setEnableStorage(enableStorage);
                //upyunUssConfig.setDefaultAcl(defaultAcl);
                
                // 应用 other 中的额外属性到平台配置对象
                setAdditionalProperties(upyunUssConfig, otherProperties);
                
                upyunUssConfigs.add(upyunUssConfig);
                properties.setUpyunUss(upyunUssConfigs);
                break;
                
            default:
                // 默认使用MinIO配置
                List<SpringFileStorageProperties.SpringMinioConfig> defaultConfigs = new ArrayList<>();
                SpringFileStorageProperties.SpringMinioConfig defaultConfig = new SpringFileStorageProperties.SpringMinioConfig();
                defaultConfig.setPlatform(platform);
                defaultConfig.setAccessKey(accessKey);
                defaultConfig.setSecretKey(secretKey);
                defaultConfig.setEndPoint(endPoint);
                defaultConfig.setBucketName(bucketName);
                defaultConfig.setDomain(domain);
                defaultConfig.setBasePath(basePath);
                defaultConfig.setEnableStorage(enableStorage);
                //defaultConfig.setDefaultAcl(defaultAcl);
                
                // 应用 other 中的额外属性到平台配置对象
                setAdditionalProperties(defaultConfig, otherProperties);
                
                defaultConfigs.add(defaultConfig);
                properties.setMinio(defaultConfigs);
                break;
        }
        
        return properties;
    }

    /**
     * 使用 other 字段中的额外属性设置配置对象
     * @param config 配置对象
     * @param otherProperties 其他属性
     */
    private void setAdditionalProperties(Object config, Map<String, Object> otherProperties) {
        if (otherProperties.isEmpty()) {
            return;
        }

        try {
            // 使用反射设置额外属性
            for (Map.Entry<String, Object> entry : otherProperties.entrySet()) {
                String fieldName = entry.getKey();
                Object fieldValue = entry.getValue();

                // 获取字段
                Field field = null;
                Class<?> clazz = config.getClass();
                while (clazz != null) {
                    try {
                        field = clazz.getDeclaredField(fieldName);
                        break;
                    } catch (NoSuchFieldException e) {
                        clazz = clazz.getSuperclass();
                    }
                }

                if (field != null) {
                    field.setAccessible(true);
                    // 类型转换
                    if (fieldValue != null) {
                        Class<?> fieldType = field.getType();
                        if (!fieldType.isAssignableFrom(fieldValue.getClass())) {
                            fieldValue = convertToTargetType(fieldValue, fieldType);
                        }
                        field.set(config, fieldValue);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("设置额外属性时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 将值转换为目标类型
     * @param value 原始值
     * @param targetType 目标类型
     * @return 转换后的值
     */
    private Object convertToTargetType(Object value, Class<?> targetType) {
        // 处理 Map 类型
        if (Map.class.isAssignableFrom(targetType) && value instanceof Map) {
            return value;
        }

        // 处理字符串类型
        if (targetType == String.class) {
            return value.toString();
        }

        // 处理布尔类型
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof String) {
                return Boolean.valueOf((String) value);
            } else {
                return Boolean.valueOf(value.toString());
            }
        }

        // 处理整数类型
        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof String) {
                return Integer.valueOf((String) value);
            } else {
                return Integer.valueOf(value.toString());
            }
        }

        // 处理长整型
        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof String) {
                return Long.valueOf((String) value);
            } else {
                return Long.valueOf(value.toString());
            }
        }

        // 处理浮点型
        if (targetType == Float.class || targetType == float.class) {
            if (value instanceof String) {
                return Float.valueOf((String) value);
            } else {
                return Float.valueOf(value.toString());
            }
        }

        // 处理双精度浮点型
        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof String) {
                return Double.valueOf((String) value);
            } else {
                return Double.valueOf(value.toString());
            }
        }

        // 如果没有匹配的转换规则，返回原始值
        return value;
    }

    /**
     * 根据S3数据源信息创建文件存储服务
     * 该方法基于给定的数据源信息构建并配置文件存储服务实例
     *
     * @param dataSourceInfo S3数据源信息，包含创建文件存储服务所需的配置参数
     * @return FileStorageService 构建完成的文件存储服务实例
     */
    public MyFileStorageService createFileStorageService(S3DataSourceInfo dataSourceInfo) {
        SpringFileStorageProperties properties = createProperties(dataSourceInfo);
        List<List<? extends FileStorage>> fileStorageLists = new ArrayList<>();
        List<FileStorageAspect> aspectList = new ArrayList<>();
        List<List<FileStorageClientFactory<?>>> clientFactoryList = new ArrayList<>();
        List<FileWrapperAdapter> fileWrapperAdapterList = new ArrayList<>();
        FileStorageServiceBuilder builder = FileStorageServiceBuilder.create(properties.toFileStorageProperties())
                .setFileRecorder(fileRecorder)
                .setAspectList(aspectList)
                .setContentTypeDetect(contentTypeDetect)
                .setFileWrapperAdapterList(fileWrapperAdapterList)
                .setClientFactoryList(clientFactoryList);
        fileStorageLists.forEach(builder::addFileStorage);
        if (properties.getEnableByteFileWrapper()) {
            builder.addByteFileWrapperAdapter();
        }
        if (properties.getEnableUriFileWrapper()) {
            builder.addUriFileWrapperAdapter();
        }
        if (properties.getEnableInputStreamFileWrapper()) {
            builder.addInputStreamFileWrapperAdapter();
        }
        if (properties.getEnableLocalFileWrapper()) {
            builder.addLocalFileWrapperAdapter();
        }
        if (properties.getEnableHttpServletRequestFileWrapper()) {
            // 检查是否存在Servlet环境，如果存在则添加HttpServletRequest文件包装适配器
            if (doesNotExistClass("javax.servlet.http.HttpServletRequest")
                    && doesNotExistClass("jakarta.servlet.http.HttpServletRequest")) {
                log.warn(
                        "当前未检测到 Servlet 环境，无法加载 HttpServletRequest 的文件包装适配器，请将参数【dromara.x-file-storage.enable-http-servlet-request-file-wrapper】设置为 【false】来消除此警告");
            } else {
                builder.addHttpServletRequestFileWrapperAdapter();
            }
        }
        if (properties.getEnableMultipartFileWrapper()) {
            // 检查是否存在SpringWeb环境，如果存在则添加MultipartFile文件包装适配器
            if (doesNotExistClass("org.springframework.web.multipart.MultipartFile")) {
                log.warn(
                        "当前未检测到 SpringWeb 环境，无法加载 MultipartFile 的文件包装适配器，请将参数【dromara.x-file-storage.enable-multipart-file-wrapper】设置为 【false】来消除此警告");
            } else {
                builder.addFileWrapperAdapter(new MultipartFileWrapperAdapter());
            }
        }

        // 检查是否存在SpringWeb环境，如果不存在且启用了本地访问功能则输出警告
        if (doesNotExistClass("org.springframework.web.servlet.config.annotation.WebMvcConfigurer")) {
            long localAccessNum = properties.getLocal().stream()
                    .filter(SpringLocalConfig::getEnableStorage)
                    .filter(SpringLocalConfig::getEnableAccess)
                    .count();
            long localPlusAccessNum = properties.getLocalPlus().stream()
                    .filter(SpringLocalPlusConfig::getEnableStorage)
                    .filter(SpringLocalPlusConfig::getEnableAccess)
                    .count();

            if (localAccessNum + localPlusAccessNum > 0) {
                log.warn("当前未检测到 SpringWeb 环境，无法开启本地存储平台的本地访问功能，请将关闭本地访问来消除此警告");
            }
        }

        FileStorageService fileStorageService = builder.build();
        fileStorageService.setSelf(fileStorageService);
        MyFileStorageService myfileStorageService = new MyFileStorageService(fileStorageService, dataSourceInfo);
        return myfileStorageService;
    }


    /**
     * 销毁指定的文件存储服务实例
     * 调用文件存储服务的destroy方法来释放相关资源
     *
     * @param fileStorageService 需要销毁的文件存储服务实例
     */
    public void destroyFileStorageService(MyFileStorageService fileStorageService) {
        if (fileStorageService != null) {
            try {
                fileStorageService.destroy();
            } catch (Exception e) {
                log.warn("关闭FileStorageService时发生异常", e);
            }
        }
    }

    /**
     * 根据指定的key销毁文件存储服务及相关资源
     * 该方法通过key查找并销毁对应的文件存储服务实例，同时清理相关数据源信息
     *
     * @param key 文件存储服务的唯一标识符
     */
    private void destroyByKey(String key) {
        // 添加空值检查
        if (key == null) {
            log.warn("尝试销毁FileStorageService时，key为空");
            return;
        }

        // 使用原子操作获取并移除连接工厂
        MyFileStorageService fileStorageService = fileStorageServiceMap.remove(key);
        if (fileStorageService != null) {
            try {
                destroyFileStorageService(fileStorageService);
            } catch (Exception e) {
                log.error("销毁FileStorageService时发生错误: {}", e.getMessage(), e);
            }
        }
        d3DataSourceInfooMap.remove(key);
    }


    /**
     * 根据指定的key获取文件存储服务
     * 该方法从内部维护的映射表中查找并返回与指定key关联的文件存储服务实例
     *
     * @param key 文件存储服务的唯一标识符
     * @return 与指定key关联的FileStorageService实例，如果未找到则返回null
     */
    public MyFileStorageService getFileStorageService(String key) {
        return fileStorageServiceMap.get(key);
    }


    /**
     * 获取所有数据源节点信息列表
     * 该方法收集所有已注册的S3数据源信息，并在存在默认文件存储服务时添加默认数据源信息
     *
     * @return 包含所有数据源信息的列表
     */
    public List<S3DataSourceInfo> datasourceNodes() {
        List<S3DataSourceInfo>  dataSourceInfoList = d3DataSourceInfooMap.values().stream().map(it -> {
            return it;
        }).collect(Collectors.toList());
        try {
            FileStorageService fileStorageService = SpringUtils.getBean(FileStorageService.class);
            if (fileStorageService != null) {
                S3DataSourceInfo dataSourceInfo = new S3DataSourceInfo();
                dataSourceInfo.setKey("default");
                dataSourceInfo.setName("默认数据源");
                dataSourceInfoList.add(dataSourceInfo);
            }
        } catch (Exception e) {
        }
        return dataSourceInfoList;
    }


    /**
     * 根据指定的key销毁文件存储服务
     * 该方法通过key查找并销毁对应的文件存储服务实例，释放相关资源
     *
     * @param key 文件存储服务的唯一标识符
     */
    public void destroyFileStorageServiceByKey(String  key) {
        destroyByKey(key);
    }

    @Override
    public void destroy() throws Exception {
        d3DataSourceInfooMap.keySet().forEach(this::destroyByKey);
    }
}
