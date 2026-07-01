package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class S3DataSourceInfo extends MagicEntity {

    /**
     * 数据源key
     */
    private String key;

    /**
     * 平台
     */
    private String platform;

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 密钥
     */
    private String secretKey;

    /**
     * 端点协议
     */
    private String endPointSchema = "https";

    /**
     * 端点
     */
    private String endPoint;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 基础路径
     */
    private String basePath = "";

    /**
     * 临时路径
     */
    private String tmpPath = "tmp";

    /**
     * 签名过期时间，默认 1 小时
     */
    private String expirationSeconds = "3600";


    // 在 S3DataSourceInfo 类中添加新字段
    private String presignedUrlMethod = "PUT"; // 默认使用 PUT 方法

    /**
     * 自动分片上传阈值，达到此大小则使用分片上传，默认 128MB。
     * 在获取不到文件大小或达到这个阈值的情况下，会使用这里提供的分片大小，否则 MinIO 会自动分片大小
     */
    private int multipartThreshold = 128 * 1024 * 1024;

    /**
     * 自动分片上传时每个分片大小，默认 32MB
     */
    private int multipartPartSize = 32 * 1024 * 1024;

    /**
     * 是否启用存储
     */
    private String enableStorage =  "true";

    /**
     * 默认访问权限
     */
    private String defaultAcl;

    /**
     * 其它配置
     */
    private String other;

    @Override
    public MagicEntity simple() {
        S3DataSourceInfo dataSourceInfo = new S3DataSourceInfo();
        super.simple(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setPlatform(this.platform);
        dataSourceInfo.setAccessKey(this.accessKey);
        dataSourceInfo.setSecretKey(this.secretKey);
        dataSourceInfo.setEndPoint(this.endPoint);
        dataSourceInfo.setBucketName(this.bucketName);
        dataSourceInfo.setBasePath(this.basePath);
        dataSourceInfo.setMultipartThreshold(this.multipartThreshold);
        dataSourceInfo.setMultipartPartSize(this.multipartPartSize);
        dataSourceInfo.setEnableStorage(this.enableStorage);
        dataSourceInfo.setDefaultAcl(this.defaultAcl);
        dataSourceInfo.setTmpPath(this.tmpPath);
        dataSourceInfo.setExpirationSeconds(this.expirationSeconds);
        dataSourceInfo.setPresignedUrlMethod(this.presignedUrlMethod);
        dataSourceInfo.setEndPointSchema(this.endPointSchema);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }

    @Override
    public MagicEntity copy() {
        S3DataSourceInfo dataSourceInfo = new S3DataSourceInfo();
        super.copyTo(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setPlatform(this.platform);
        dataSourceInfo.setAccessKey(this.accessKey);
        dataSourceInfo.setSecretKey(this.secretKey);
        dataSourceInfo.setEndPoint(this.endPoint);
        dataSourceInfo.setBucketName(this.bucketName);
        dataSourceInfo.setBasePath(this.basePath);
        dataSourceInfo.setMultipartThreshold(this.multipartThreshold);
        dataSourceInfo.setMultipartPartSize(this.multipartPartSize);
        dataSourceInfo.setEnableStorage(this.enableStorage);
        dataSourceInfo.setDefaultAcl(this.defaultAcl);
        dataSourceInfo.setTmpPath(this.tmpPath);
        dataSourceInfo.setExpirationSeconds(this.expirationSeconds);
        dataSourceInfo.setPresignedUrlMethod(this.presignedUrlMethod);
        dataSourceInfo.setEndPointSchema(this.endPointSchema);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }





}
