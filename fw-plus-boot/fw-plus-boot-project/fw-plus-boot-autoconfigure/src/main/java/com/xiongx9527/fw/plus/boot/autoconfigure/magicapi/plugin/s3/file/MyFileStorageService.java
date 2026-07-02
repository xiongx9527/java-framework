package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpDownloader;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Slf4j
public class MyFileStorageService {

    /** 文件存储服务，用于处理文件的存储操作 */
    private final FileStorageService fileStorageService ;

    /** S3数据源信息，包含S3存储的相关配置和连接信息 */
    private final S3DataSourceInfo s3DataSourceInfo;

    /**
     * 构造函数，初始化文件存储服务和S3数据源信息
     * @param fileStorageService 文件存储服务实例
     * @param s3DataSourceInfo S3数据源配置信息
     */
    public MyFileStorageService(FileStorageService fileStorageService, S3DataSourceInfo s3DataSourceInfo) {
        this.fileStorageService = fileStorageService;
        this.s3DataSourceInfo = s3DataSourceInfo;

        Map<String, Object> other = new HashMap<>();
        String otherConfig = s3DataSourceInfo.getOther();

        if (otherConfig != null && !otherConfig.isEmpty()) {
            try {
                other.putAll(JSON.parseObject(otherConfig));
            } catch (com.alibaba.fastjson.JSONException e) {
                log.warn("other json parse error for dataSource: {}", s3DataSourceInfo, e);
            }
        }
    }

    /**
     * 从配置映射中解析指定键的配置值并转换为目标类型
     *
     * @param configMap 配置映射表，包含键值对形式的配置信息
     * @param key 要解析的配置项键名
     * @param clazz 目标类型的Class对象，用于类型转换
     * @return 解析成功时返回转换后的对象，解析失败或值不存在时返回null
     */
    @SuppressWarnings("unused")
    private <T> T parseConfig(Map<String, Object> configMap, String key, Class<T> clazz) {
        Object value = configMap.get(key);
        // 处理值为null的情况
        if (value == null) {
            value = new HashMap<>();
        }

        try {
            // 避免多余序列化：直接使用原始对象进行类型转换
            return JSON.parseObject(JSON.toJSONString(value), clazz);
        } catch (Exception e) {
            log.warn("Failed to parse config key '{}': {}", key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 通过URL上传文件
     *
     * @param url 文件的网络地址
     * @return 上传后的文件信息对象
     */
    public FileInfo uploadByUrl(String url) {
        String name = null;
        try {
            URI uri = new URI(url);
            name = FileUtil.getName(uri.getPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        if ((StringUtils.isBlank(name) || name.split("\\.").length != 2)) {
            // 当文件名为空或格式不正确时，通过HTTP请求获取文件内容和文件名
            HttpRequest request = HttpUtil.createGet(url, true);
            HttpResponse response = request.executeAsync();
            if (response.isOk()) {
                name = response.getFileNameFromDisposition();
                byte[] bytes = response.bodyBytes();
                return this.upload(bytes,name);
            } else {
                throw new FileStorageRuntimeException("Failed to download file from URL: " + url);
            }
        }
        // 从指定URL下载文件字节数据并上传
        return this.upload(HttpUtil.downloadBytes(url),name);
    }


    /**
     * 上传文件
     *
     * @param source 上传的源对象，可以是文件路径、字节数组或其他支持的源类型
     * @return FileInfo 包含上传文件信息的对象，如果上传失败则返回null
     */
    public FileInfo upload(Object source) {
        return this.upload(source, null);
    }

    /**
     * 通过URL上传文件
     *
     * @param url 文件的网络地址
     * @param name 文件名称
     * @return 上传后的文件信息对象
     */
    public FileInfo uploadByUrl(String url, String name) {
        // 从指定URL下载文件字节数据并上传
        return this.upload(HttpUtil.downloadBytes(url),name);
    }

    /**
     * 修改文件信息中的URL地址
     * @param fileInfo 文件信息对象，用于获取文件名并设置新的URL
     * @param basePath 基础路径，用于构建完整的文件访问路径
     */
    private void changeUrl(FileInfo fileInfo , String basePath) {
        String fileName = fileInfo.getFilename();
        // 确保基础路径以斜杠结尾，便于拼接文件名
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }
        String endPointSchema = s3DataSourceInfo.getEndPointSchema();
        endPointSchema = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.endPointSchema",this.s3DataSourceInfo.getKey()),endPointSchema);
        String bucketName = s3DataSourceInfo.getBucketName();
        bucketName = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.bucketName",this.s3DataSourceInfo.getKey()),bucketName);
        String endPoint = s3DataSourceInfo.getEndPoint();
        endPoint = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.endPoint",this.s3DataSourceInfo.getKey()),endPoint);
        if (!endPoint.endsWith("/")) {
            endPoint += "/";
        }
        // 构建完整的文件访问URL并设置到文件信息中
        fileInfo.setUrl(endPointSchema + "://" + bucketName + "." + endPoint + basePath + fileName);
    }

    /**
     * 上传文件
     *
     * @param source 文件源对象，可以是文件路径、字节数组或其他支持的源类型
     * @param name 文件名称
     * @return FileInfo 包含上传文件信息的对象
     */
    public FileInfo upload(Object source, String name) {
        return upload(source, name, null);
    }

    /**
     * 通过URL上传文件
     *
     * @param url 文件的网络地址
     * @param name 文件名称
     * @param contentType 文件内容类型
     * @return 文件信息对象
     */
    public FileInfo uploadByUrl(String url, String name, String contentType) {
        // 从指定URL下载文件字节数据并上传
        return this.upload(HttpUtil.downloadBytes(url),name,contentType);
    }

    /**
     * 创建上传预处理器
     *
     * @param source      源
     * @param name        文件名
     * @param contentType 文件的 MIME 类型
     * @return 文件信息对象
     * @throws IllegalArgumentException 当source、name或contentType为null时抛出
     * @throws IllegalStateException 当fileStorageService未初始化时抛出
     * @throws RuntimeException 当创建上传预处理器失败时抛出
     */
    public FileInfo upload(Object source, String name, String contentType) {
        FileInfo fileInfo =  this.off(source, name, contentType).upload();
        changeUrl(fileInfo, this.s3DataSourceInfo.getBasePath());
        return fileInfo;
    }

    /**
     * 临时上传文件
     *
     * @param source 上传源对象，可以是文件路径、字节数组或其他支持的源类型
     * @return 文件信息对象，包含上传后的文件相关信息
     */
    public FileInfo tmpUpload(Object source) {
        return this.tmpUpload(source, null);
    }

    /**
     * 临时上传文件
     *
     * @param source 文件源对象，可以是文件路径、字节数组或其他文件来源
     * @param name 文件名称
     * @return FileInfo 文件信息对象，包含上传后的文件相关信息
     */
    public FileInfo tmpUpload(Object source, String name) {
        return this.tmpUpload(source, name, null);
    }

    /**
     * 临时上传文件
     *
     * @param source 文件源对象，可以是文件路径、InputStream等
     * @param name 文件名称
     * @param contentType 文件内容类型
     * @return FileInfo 上传后的文件信息对象
     * @throws FileStorageRuntimeException 当临时上传配置为空或未启用时抛出异常
     */
    public FileInfo tmpUpload(Object source, String name, String contentType) {
        String tmpPath = this.s3DataSourceInfo.getTmpPath();
        tmpPath = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.tmpPath",this.s3DataSourceInfo.getKey()),tmpPath);
        // 执行文件上传操作，设置上传路径并返回文件信息
        FileInfo fileInfo = this.off(source, name, contentType).setPath(tmpPath).upload();
        changeUrl(fileInfo, tmpPath);
        return fileInfo;
    }

    /**
     * 移动文件到指定路径
     *
     * @param fileInfo 需要移动的文件信息对象
     * @param targetPath 目标路径
     * @return 移动后的文件信息对象
     */
    public FileInfo move(FileInfo fileInfo, String targetPath) {
        // 调用文件存储服务执行移动操作，设置目标路径并完成移动
        return this.fileStorageService.move(fileInfo).setPath(targetPath).move();
    }

    /**
     * 根据文件名获取文件的MIME类型
     *
     * @param filename 文件名
     * @return 文件对应的MIME类型字符串
     */
    private String getContentTypeByFilename(String filename) {
        return FileUtil.getMimeType(filename);
    }

    /**
     * 生成文件上传的预签名URL
     *
     * @param filename 文件名
     * @return 生成预签名URL的结果对象
     */
    public GeneratePresignedUrlResult generateUploadPresignedUrl(String filename) {
        // 根据配置确定请求方法，支持POST和PUT两种方式
        String method =
                "POST".equalsIgnoreCase(this.s3DataSourceInfo.getPresignedUrlMethod()) ?
                        Constant.GeneratePresignedUrl.Method.POST :
                        Constant.GeneratePresignedUrl.Method.PUT;
        // 构建预签名URL生成器，设置临时路径、文件名、请求方法为PUT、过期时间和Content-Type头部
        String tmpPath = this.s3DataSourceInfo.getTmpPath();
        tmpPath = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.tmpPath",this.s3DataSourceInfo.getKey()),tmpPath);
        String expirationStr = this.s3DataSourceInfo.getExpirationSeconds();
        expirationStr = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.expirationSeconds",this.s3DataSourceInfo.getKey()),expirationStr);
        int expiration = Integer.parseInt(expirationStr);
        return this.fileStorageService.generatePresignedUrl()
                .setPath(tmpPath)
                .setFilename(filename)
                .setMethod(method)
                .setExpiration(DateUtil.offsetSecond(new Date(), expiration))
                .putHeaders(org.dromara.x.file.storage.core.constant.Constant.Metadata.CONTENT_TYPE, getContentTypeByFilename(filename))
                .generatePresignedUrl();
    }

    /**
     * 生成文件的预签名URL
     *
     * @param fileInfo 文件信息对象，包含文件的基本信息
     * @return 返回生成的预签名URL字符串
     */
    public String generatePresignedUrl(FileInfo fileInfo) {
        String expirationStr = this.s3DataSourceInfo.getExpirationSeconds();
        expirationStr = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.expirationSeconds",this.s3DataSourceInfo.getKey()),expirationStr);
        int expiration = Integer.parseInt(expirationStr);
        // 调用文件存储服务生成预签名URL，设置过期时间为当前时间加上配置的过期秒数
        return this.fileStorageService.generatePresignedUrl(fileInfo,
                DateUtil.offsetSecond(new Date(), expiration));
    }

    /**
     * 根据给定的URL生成预签名URL
     *
     * @param url 原始文件URL地址
     * @return 预签名URL字符串，如果输入URL为空则返回null
     */
    public String generatePresignedUrlByUrl(String url,String fileName) {
        if (StringUtils.isBlank(url) || StringUtils.isBlank(fileName)) {
            return url;
        }
        if(url.contains("?")) {
            url = url.substring(0,url.indexOf("?"));
        }
        String endPoint = this.s3DataSourceInfo.getEndPoint();
        endPoint = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.endPoint",this.s3DataSourceInfo.getKey()),endPoint);
        String bucketName = this.s3DataSourceInfo.getBucketName();
        bucketName = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.bucketName",this.s3DataSourceInfo.getKey()),bucketName);
        if (!url.contains(endPoint) || !url.contains(bucketName)) {
            return url;
        }
        FileInfo fileInfo = new FileInfo();
        fileInfo.setUrl(url);
        fileInfo.setFilename(fileName);
        String basePath = this.s3DataSourceInfo.getBasePath();
        basePath = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.basePath",this.s3DataSourceInfo.getKey()),basePath);
        fileInfo.setBasePath(basePath);
        return this.generatePresignedUrl(fileInfo);
    }

    /**
     * 根据文件名生成预签名URL
     *
     * @param fileName 文件名
     * @return 预签名URL字符串
     */
    public String generatePresignedUrlByFileName(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return null;
        }
        // 获取S3数据源配置信息
        String endPointSchema = this.s3DataSourceInfo.getEndPointSchema();
        endPointSchema = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.endPointSchema",this.s3DataSourceInfo.getKey()),endPointSchema);
        String endPoint = this.s3DataSourceInfo.getEndPoint();
        endPoint = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.endPoint",this.s3DataSourceInfo.getKey()),endPoint);
        String bucketName = this.s3DataSourceInfo.getBucketName();
        bucketName = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.bucketName",this.s3DataSourceInfo.getKey()),bucketName);
        String basePath = this.s3DataSourceInfo.getBasePath();
        basePath = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.basePath",this.s3DataSourceInfo.getKey()),basePath);

        // 构建基础URL
        String url = endPointSchema + "://" + bucketName + "." + endPoint ;

        // 确保basePath格式正确，以"/"开头并以"/"结尾
        if (!basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }

        // 拼接完整URL并生成预签名URL
        url += basePath + fileName;
        return this.generatePresignedUrlByUrl(url, fileName);
    }


    /**
     * 根据给定的URL生成预签名URL
     *
     * @param url 原始URL地址，用于生成预签名URL
     * @return 返回生成的预签名URL字符串
     * @throws IllegalArgumentException 当URL或s3DataSourceInfo为空，或S3数据源信息属性为空时抛出异常
     */
    public String generatePresignedUrlByUrl(String url) {
        // 空值检查
        if (StringUtils.isBlank(url)) {
            return url;
        }
        if(url.contains("?")) {
            url = url.substring(0,url.indexOf("?"));
        }
        String endPoint = this.s3DataSourceInfo.getEndPoint();
        endPoint = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.endPoint",this.s3DataSourceInfo.getKey()),endPoint);
        String bucketName = this.s3DataSourceInfo.getBucketName();
        bucketName = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.bucketName",this.s3DataSourceInfo.getKey()),bucketName);
        String basePath = this.s3DataSourceInfo.getBasePath();
        basePath = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.basePath",this.s3DataSourceInfo.getKey()),basePath);
        String tmpPath = this.s3DataSourceInfo.getTmpPath();
        tmpPath = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.tmpPath",this.s3DataSourceInfo.getKey()),tmpPath);
        if (!url.contains(endPoint) || !url.contains(bucketName)) {
            return url;
        }
        // 提取文件名，去除协议头和端点信息
        String fileName = url.replace("http://", "");
        fileName = fileName.replace("https://", "");
        fileName = fileName.replace(bucketName + "."+ endPoint, "");

        // 处理basePath
        fileName = processPath(fileName, basePath);

        // 处理tmpPath
        fileName = processPath(fileName, tmpPath);

        return this.generatePresignedUrlByUrl(url, fileName);
    }

    /**
     * 处理文件路径，移除指定的路径前缀
     *
     * @param fileName 原始文件名或路径
     * @param path 需要被移除的路径前缀
     * @return 处理后的文件名，如果path为空则返回原文件名
     */
    private String processPath(String fileName, String path) {
        // 如果路径前缀为空，直接返回原文件名
        if (StringUtils.isBlank(path) || StringUtils.isBlank(path)) {
            return fileName;
        }

        // 移除文件名中的路径前缀
        if (fileName.startsWith(path)) {
            fileName = fileName.substring(path.length());
        } else if (fileName.startsWith("/" + path)) {
            fileName = fileName.substring(path.length() + 1);
        }
        return fileName;
    }

    /**
     * 对文件生成可以签名访问的 URL，无法生成则返回 null
     *
     * @param expiration 到期时间
     */
    public String generatePresignedUrl(FileInfo fileInfo, Date expiration) {
        return this.fileStorageService.generatePresignedUrl(fileInfo, expiration);
    }

    /**
     * 生成文件的预签名URL（缩略图版本）
     *
     * @param fileInfo 文件信息对象，包含文件的基本信息
     * @return 返回生成的预签名URL字符串，用于临时访问文件
     */
    public String generateThPresignedUrl(FileInfo fileInfo) {
        String expirationStr = this.s3DataSourceInfo.getExpirationSeconds();
        expirationStr = SpringUtils.getAppConfig(String.format("magic.api.s3.%s.expirationSeconds",this.s3DataSourceInfo.getKey()),expirationStr);
        int expiration = Integer.parseInt(expirationStr);
        // 调用文件存储服务生成预签名URL，设置过期时间为当前时间加上配置的过期秒数
        return this.fileStorageService.generateThPresignedUrl(fileInfo,
                DateUtil.offsetSecond(new Date(), expiration));
    }

    /**
     * 对缩略图文件生成可以签名访问的 URL，无法生成则返回 null
     *
     * @param expiration 到期时间
     */
    public String generateThPresignedUrl(FileInfo fileInfo, Date expiration) {
        return this.fileStorageService.generateThPresignedUrl(fileInfo, expiration);
    }

    /**
     * 关闭当前上传预处理并创建新的上传预处理器
     * @param source 数据源对象，不能为null
     * @param name 文件名称
     * @param contentType 文件内容类型
     * @return 新创建的上传预处理器
     * @throws IllegalStateException 当文件存储服务未初始化时抛出
     * @throws IllegalArgumentException 当source参数为null时抛出
     * @throws RuntimeException 当创建上传预处理器失败时抛出
     */
    public UploadPretreatment off(Object source, String name, String contentType) {
        if (this.fileStorageService == null) {
            throw new IllegalStateException("fileStorageService is not initialized");
        }
        // 参数校验
        if (source == null) {
            throw new IllegalArgumentException("source cannot be null");
        }
        // 调用文件存储服务创建上传预处理器
        try {
            return this.fileStorageService.of(source, name, contentType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create upload pretreatment", e);
        }
    }

    /**
     * 删除指定URL对应的文件
     *
     * @param url 文件的URL路径
     * @return 删除成功返回true，否则返回false
     */
    public boolean delete(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        try {
            return this.fileStorageService.delete(url);
        } catch (Exception e) {
            log.warn("删除文件失败", e);
            return false;
        }
    }

    /**
     * 删除文件
     *
     * @param url 文件存储路径
     * @param predicate 文件信息过滤条件
     * @return 删除成功返回true，否则返回false
     */
    public boolean delete(String url, Predicate<FileInfo> predicate) {
        if (url == null || predicate == null) {
            return false;
        }

        try {
            return this.fileStorageService.delete(url, predicate);
        } catch (Exception e) {
            log.warn("删除文件失败", e);
            return false;
        }
    }

    /**
     * 根据条件删除文件
     * @param fileInfo 文件信息对象，包含要删除文件的相关信息
     * @return boolean 删除结果，true表示删除成功，false表示删除失败
     */
    public boolean delete(FileInfo fileInfo) {
        if (fileInfo == null) {
            return false;
        }

        try {
            return this.fileStorageService.delete(fileInfo);
        } catch (Exception e) {
            log.warn("删除文件失败", e);
            return false;
        }
    }

    /**
     * 根据条件删除文件
     *
     * @param fileInfo 文件信息对象，包含要删除的文件的基本信息
     * @param predicate 删除条件谓词，用于判断是否满足删除条件
     * @return boolean 删除操作是否成功执行
     */
    public boolean delete(FileInfo fileInfo, Predicate<FileInfo> predicate) {
        // 参数校验
        if (fileInfo == null || predicate == null) {
            return false;
        }

        try {
            return this.fileStorageService.delete(fileInfo, predicate);
        } catch (Exception e) {
            log.warn("删除文件失败", e);
            return false;
        }
    }

    /**
     * 删除文件
     *
     * @param fileInfo 文件信息对象，包含要删除文件的相关信息
     * @param fileStorage 文件存储对象，指定文件存储的位置和配置
     * * @param fileRecorder 文件记录器对象，用于记录文件操作日志
     * @param aspectList 文件存储切面列表，包含删除操作的前置和后置处理逻辑
     * @return 删除成功返回true，删除失败返回false
     */
    public boolean delete(
            FileInfo fileInfo, FileStorage fileStorage, FileRecorder fileRecorder, List<FileStorageAspect> aspectList) {
        if (fileInfo == null || fileStorage == null || fileRecorder == null) {
            return false;
        }
        try {
            return this.fileStorageService.delete(fileInfo, fileStorage, fileRecorder, aspectList);
        } catch (Exception e) {
            log.warn("删除文件失败", e);
            return false;
        }
    }

    /**
     * 销毁当前对象，释放相关资源
     *
     * 该方法调用文件存储服务的destroy方法来清理资源
     */
    public void destroy() {
        if (this.fileStorageService == null) {
            return;
        }

        try {
            this.fileStorageService.destroy();
        } catch (Exception e) {
            log.warn("关闭FileStorageService时发生异常", e);
        }
    }

    /**
     * 临时上传信息配置类
     * 用于配置临时文件上传的相关参数
     */
    @Data
    public static class TmpUploadInfo {
        /**
         * 是否启用临时上传功能
         * 默认值为true，表示启用
         */
        private boolean enable =  true;

        /**
         * 临时文件存储目录路径
         * 用于指定上传文件的存储位置
         */
        private String dir = "tmp";
    }


}
