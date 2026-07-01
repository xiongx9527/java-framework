package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.MultipartFileWrapperAdapter;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.MyFileStorageService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.SpringFileStorageProperties;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.SpringFileStorageProperties.SpringLocalConfig;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.SpringFileStorageProperties.SpringLocalPlusConfig;
import com.xx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.FileStorageServiceBuilder;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.file.FileWrapperAdapter;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.FileStorageClientFactory;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;
import org.springframework.beans.factory.DisposableBean;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final Map<String, S3DataSourceInfo> s3DataSourceInfoMap = new ConcurrentHashMap<>();

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
        MyFileStorageService oldService = this.fileStorageServiceMap.put(dataSourceInfo.getKey(), fileStorageService);
        this.s3DataSourceInfoMap.put(dataSourceInfo.getKey(), dataSourceInfo);
        if (oldService != null) {
            destroyFileStorageService(oldService);
        }
    }


    /**
     * 创建文件存储平台配置属性对象
     * 
     * <p>该方法根据传入的平台类型和认证信息，创建对应的云存储平台配置对象。
     * 支持多种云存储平台包括MinIO、Amazon S3、华为云OBS、阿里云OSS、七牛云Kodo、
     * 腾讯云COS、百度云BOS、又拍云USS等。同时支持从JSON格式的other参数中解析额外配置项。</p>
     *
     * @param platform 存储平台类型标识，如"minio"、"amazon-s3"、"huawei-obs"等。如果为null则默认使用"minio"
     * @param other JSON格式的额外配置参数字符串，用于设置平台特定的高级配置项
     * @param accessKey 访问密钥ID，用于身份认证。对于又拍云USS为用户名，腾讯云COS为SecretId
     * @param secretKey 访问密钥密码，用于身份认证。对于又拍云USS为密码
     * @param endPoint 服务端点地址，不同平台的含义可能不同（如地域标识、服务地址等）
     * @param domain 自定义域名，用于构建文件访问URL
     * @param bucketName 存储桶名称
     * @param basePath 基础路径，用于指定文件在存储桶中的存放目录
     * @param defaultAcl 默认访问控制列表权限设置（部分平台不支持）
     * @param enableStorageStr 是否启用存储的字符串标识，会被解析为布尔值
     * @return SpringFileStorageProperties 配置属性对象，包含指定平台的完整配置信息
     */
    private SpringFileStorageProperties createProperties(String platform,String other,
                                                         String accessKey,String secretKey,
                                                         String  endPoint,String domain,String bucketName,String basePath,String defaultAcl,String enableStorageStr) {
        SpringFileStorageProperties properties = new SpringFileStorageProperties();
        if (platform == null) {
            platform = "minio";
        }
        
        Map<String, Object> otherProperties = new HashMap<>();
        if (StringUtils.isNotBlank(other)) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                otherProperties = objectMapper.readValue(other, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                otherProperties = new HashMap<>();
            }
        }

        properties.setDefaultPlatform(platform);
        
        setAdditionalProperties(properties, otherProperties);
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
                qiniuKodoConfig.setBucketName(bucketName);
                qiniuKodoConfig.setDomain(domain);
                qiniuKodoConfig.setBasePath(basePath);
                qiniuKodoConfig.setEnableStorage(enableStorage);

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
                upyunUssConfig.setBucketName(bucketName);
                upyunUssConfig.setDomain(domain);
                upyunUssConfig.setBasePath(basePath);
                upyunUssConfig.setEnableStorage(enableStorage);

                setAdditionalProperties(upyunUssConfig, otherProperties);

                upyunUssConfigs.add(upyunUssConfig);
                properties.setUpyunUss(upyunUssConfigs);
                break;

            default:
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

                setAdditionalProperties(defaultConfig, otherProperties);

                defaultConfigs.add(defaultConfig);
                properties.setMinio(defaultConfigs);
                break;
        }

        return properties;
    }

    /**
     * 根据S3数据源信息创建Spring文件存储属性配置
     * 该方法根据platform类型将S3DataSourceInfo中的配置信息转换为相应的SpringFileStorageProperties对象
     *
     * @param dataSourceInfo S3数据源信息
     * @return SpringFileStorageProperties Spring文件存储属性配置
     */
    private SpringFileStorageProperties createProperties(S3DataSourceInfo dataSourceInfo) {
        String platform = dataSourceInfo.getPlatform();
        platform = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "platform", platform);

        // 解析 other 字段中的额外属性
        String other = dataSourceInfo.getOther();
        other = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "other", other);
        
        // 应用 other 中的额外属性到 SpringFileStorageProperties 对象
        String accessKey = dataSourceInfo.getAccessKey();
        accessKey = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "accessKey", accessKey);
        String secretKey = dataSourceInfo.getSecretKey();
        secretKey = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "secretKey", secretKey);
        String endPointSchema = dataSourceInfo.getEndPointSchema();
        endPointSchema = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "endPointSchema", endPointSchema);
        String  endPoint = dataSourceInfo.getEndPoint();
        endPoint = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "endPoint", endPoint);
        String bucketName = dataSourceInfo.getBucketName();
        bucketName = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "bucketName", bucketName);
        String basePath = dataSourceInfo.getBasePath();
        basePath = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "basePath", basePath);
        String defaultAcl = dataSourceInfo.getDefaultAcl();
        defaultAcl = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "defaultAcl", defaultAcl);

        String enableStorageStr = dataSourceInfo.getEnableStorage() ;
        enableStorageStr = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "enableStorage", enableStorageStr);
        String domain = endPointSchema + "://" + bucketName + "." + endPoint;
        return createProperties(platform,other,accessKey,secretKey,endPoint,domain,bucketName,basePath,defaultAcl,enableStorageStr);
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
     * 创建文件存储服务实例
     * 
     * <p>该方法根据提供的配置属性构建并初始化文件存储服务。包括配置各种文件包装适配器、
     * 内容类型检测器、文件记录器等组件。同时会检查运行环境，确保在合适的上下文中加载相应功能。</p>
     *
     * @param properties 文件存储配置属性对象，包含各存储平台的配置信息和全局设置
     * @return FileStorageService 构建完成的文件存储服务实例，可用于执行文件上传、下载等操作
     */
    public  FileStorageService createFileStorageService(SpringFileStorageProperties properties) {
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
            if (doesNotExistClass("javax.servlet.http.HttpServletRequest")
                    && doesNotExistClass("jakarta.servlet.http.HttpServletRequest")) {
                log.warn(
                        "当前未检测到 Servlet 环境，无法加载 HttpServletRequest 的文件包装适配器，请将参数【dromara.x-file-storage.enable-http-servlet-request-file-wrapper】设置为 【false】来消除此警告");
            } else {
                builder.addHttpServletRequestFileWrapperAdapter();
            }
        }
        
        if (properties.getEnableMultipartFileWrapper()) {
            if (doesNotExistClass("org.springframework.web.multipart.MultipartFile")) {
                log.warn(
                        "当前未检测到 SpringWeb 环境，无法加载 MultipartFile 的文件包装适配器，请将参数【dromara.x-file-storage.enable-multipart-file-wrapper】设置为 【false】来消除此警告");
            } else {
                builder.addFileWrapperAdapter(new MultipartFileWrapperAdapter());
            }
        }

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
        return fileStorageService;
    }

    /**
     * 根据S3数据源信息和指定路径创建文件存储服务
     *
     * <p>该方法基于给定的S3数据源配置信息和基础路径，构建并初始化一个文件存储服务实例。
     * 方法会从数据源信息中提取各项配置（如platform、accessKey、secretKey、endPoint等），
     * 并结合Spring应用配置进行覆盖，最终创建可使用的文件存储服务。</p>
     *
     * @param dataSourceInfo S3数据源信息，包含创建文件存储服务所需的配置参数，不能为null
     * @param basePath 基础路径，用于指定文件的存储目录。如果为空或空白，则使用数据源信息中的basePath
     * @return FileStorageService 构建完成的文件存储服务实例，可用于文件上传、下载等操作
     * @throws RuntimeException 当创建文件存储服务失败时可能抛出运行时异常
     */
    public FileStorageService createFileStorageService(S3DataSourceInfo dataSourceInfo, String basePath) {
        String platform = dataSourceInfo.getPlatform();
        platform = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "platform", platform);

        String other = dataSourceInfo.getOther();
        other = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "other", other);
        
        // 获取S3认证和连接配置信息，支持通过Spring应用配置覆盖
        String accessKey = dataSourceInfo.getAccessKey();
        accessKey = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "accessKey", accessKey);
        String secretKey = dataSourceInfo.getSecretKey();
        secretKey = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "secretKey", secretKey);
        String  endPoint = dataSourceInfo.getEndPoint();
        endPoint = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "endPoint", endPoint);
        String domain = dataSourceInfo.getEndPointSchema() + "://" + dataSourceInfo.getBucketName() + "." + dataSourceInfo.getEndPoint();
        String bucketName = dataSourceInfo.getBucketName();
        bucketName = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "bucketName", bucketName);
        String defaultAcl = dataSourceInfo.getDefaultAcl();
        defaultAcl = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "defaultAcl", defaultAcl);

        // 如果未提供basePath，则使用数据源配置中的默认值
        if (StringUtils.isBlank(basePath)) {
            basePath = dataSourceInfo.getBasePath();
            basePath = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "basePath", basePath);
        }

        String enableStorageStr = dataSourceInfo.getEnableStorage() ;
        enableStorageStr = MagicAPIConfigUtils.getS3Config(dataSourceInfo.getKey(), "enableStorage", enableStorageStr);
        
        // 创建配置属性对象并构建文件存储服务
        SpringFileStorageProperties properties = createProperties(platform,other,accessKey,secretKey,endPoint,domain,bucketName,basePath,defaultAcl,enableStorageStr);
        FileStorageService fileStorageService = createFileStorageService(properties);
        return fileStorageService;
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
        FileStorageService fileStorageService = createFileStorageService(properties);
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
        s3DataSourceInfoMap.remove(key);
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
        List<S3DataSourceInfo> dataSourceInfoList = new ArrayList<>(s3DataSourceInfoMap.values());
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
        new ArrayList<>(s3DataSourceInfoMap.keySet()).forEach(this::destroyByKey);
    }
}
