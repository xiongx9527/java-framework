package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.MagicDynamicS3DataSource;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo;
import com.xx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.x.file.storage.core.Downloader;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.get.ListFilesPretreatment;
import org.dromara.x.file.storage.core.get.ListFilesResult;
import org.dromara.x.file.storage.core.get.RemoteFileInfo;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Slf4j
public class MyFileStorageService {

    /** 文件存储服务，用于处理文件的存储操作 */
    private final FileStorageService fileStorageService ;

    /** S3数据源信息，包含S3存储的相关配置和连接信息 */
    private final S3DataSourceInfo s3DataSourceInfo;

    /**
     * 文件存储服务映射，用于根据路径获取对应的文件存储服务
     */
    private Map<String,FileStorageService> fileStorageServiceMap = new ConcurrentHashMap<>();

    /**
     * 构造函数，初始化文件存储服务和S3数据源信息
     * @param fileStorageService 文件存储服务实例
     * @param s3DataSourceInfo S3数据源配置信息
     */
    public MyFileStorageService(FileStorageService fileStorageService, S3DataSourceInfo s3DataSourceInfo) {
        this.fileStorageService = fileStorageService;
        this.s3DataSourceInfo = s3DataSourceInfo;
    }

    /**
     * 根据路径获取对应的文件存储服务实例
     *
     * <p>该方法用于判断给定路径是否属于当前数据源的基础路径范围。
     * 如果路径匹配当前数据源的basePath，则返回默认的文件存储服务；
     * 否则，从动态数据源中创建并返回一个新的文件存储服务实例，并使用缓存避免重复创建。</p>
     *
     * @param path 要检查的文件路径，用于确定使用哪个文件存储服务。如果为空或null则返回默认服务
     * @return FileStorageService 对应的文件存储服务实例
     */
    private FileStorageService getFileStorageServiceByPath(String path) {
        String basePath = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "basePath", this.s3DataSourceInfo.getBasePath());

        // 判断路径是否属于当前数据源的基础路径范围
        if (StringUtils.isBlank(path)
                || path.startsWith(basePath)
                || path.startsWith("/"+basePath)
                || path.startsWith(basePath+"/")
                || path.startsWith("/"+basePath+"/")
                || ("/"+path).startsWith(basePath)
                || (path+"/").startsWith(basePath)
                || ("/"+path+"/").startsWith(basePath)
        ) {
            return this.fileStorageService ;
        }

        // 标准化路径格式，提取第一级目录作为新的basePath
        if (!path.endsWith("/")) {
            path += "/";
        }

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        int slashIndex = path.indexOf("/");
        if (slashIndex == -1) {
            return this.fileStorageService;
        }

        String myBasePath = path.substring(0, slashIndex);

        // 从缓存中获取已创建的文件存储服务实例
        FileStorageService cachedService = this.fileStorageServiceMap.get(myBasePath);
        if (cachedService != null) {
            return cachedService;
        }

        // 双重检查锁定，确保线程安全地创建文件存储服务实例
        synchronized (this){
            cachedService = this.fileStorageServiceMap.get(myBasePath);
            if (cachedService != null) {
                return cachedService;
            }

            // 从动态数据源创建新的文件存储服务实例
            MagicDynamicS3DataSource magicDynamicS3DataSource = SpringUtils.getBean(MagicDynamicS3DataSource.class);
            FileStorageService myFileStorageService = magicDynamicS3DataSource.createFileStorageService(this.s3DataSourceInfo, myBasePath);

            if (myFileStorageService == null) {
                log.warn("Failed to create FileStorageService for basePath: {}, using default service", myBasePath);
                return this.fileStorageService;
            }

            this.fileStorageServiceMap.put(myBasePath, myFileStorageService);
            return myFileStorageService;
        }
    }

    /**
     * 构建S3文件的完整访问URL
     *
     * <p>该方法根据基础路径、子路径和文件名，结合S3数据源配置信息（endpoint、bucket、协议等），
     * 构建完整的S3文件访问URL。支持从配置中动态获取各项参数，并自动处理路径格式标准化。</p>
     *
     * @param basePath 基础路径，用于指定文件的存储目录。如果为空则使用数据源配置的默认值
     * @param path 子路径，用于进一步限定文件在基础路径下的具体位置。如果为空则直接使用basePath
     * @param fileName 文件名，不能为空，是构建URL的必要参数
     * @return String 完整的S3文件访问URL，格式为：schema://bucket.endpoint/basePath/path/fileName；
     *         如果fileName为空则返回null
     */
    private String getUrl(String basePath,String path,String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return null;
        }

        // 标准化basePath格式，确保以斜杠结尾且不以斜杠开头
        if (StringUtils.isNotBlank(basePath)) {
            if (basePath.startsWith("/")) {
                basePath = basePath.substring(1);
            }
            if (!basePath.endsWith("/")) {
                basePath +="/";
            }
        } else {
            basePath = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "basePath", this.s3DataSourceInfo.getBasePath());
        }

        // 标准化path格式，确保以斜杠结尾且不以斜杠开头
        if (StringUtils.isNotBlank(path)) {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (!path.endsWith("/")) {
                path +="/";
            }
        }

        // 获取S3数据源配置信息
        String endPoint = this.s3DataSourceInfo.getEndPoint();
        endPoint = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "endPoint", endPoint);
        if (!endPoint.endsWith("/")) {
            endPoint += "/";
        }
        String bucketName = this.s3DataSourceInfo.getBucketName();
        bucketName = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "bucketName", bucketName);
        String endPointSchema = this.s3DataSourceInfo.getEndPointSchema();
        endPointSchema = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "endPointSchema", endPointSchema);

        // 构建完整的S3访问URL
        return endPointSchema + "://" + bucketName + "." + endPoint + basePath + path + fileName;
    }

    /**
     * 根据URL和路径信息构建文件信息对象
     *
     * <p>该方法用于解析S3文件的URL地址，提取文件名、基础路径、子路径等信息，并构建完整的FileInfo对象。
     * 支持从URL中自动提取文件名，验证URL的有效性，并根据配置动态获取endpoint、bucket等参数。
     * 如果URL为空，则根据提供的参数构建完整的S3访问URL。</p>
     *
     * @param url 文件的完整URL地址，如果为空则根据其他参数构建URL
     * @param basePath 基础路径，用于指定文件的存储目录。如果为空则使用数据源配置的默认值或临时路径
     * @param path 子路径，用于进一步限定文件在基础路径下的具体位置。如果为空则从URL中提取
     * @param fileName 文件名，如果为空则从URL中自动提取
     * @return FileInfo 构建的文件信息对象，包含URL、文件名、路径、扩展名和Content-Type等信息；
     *         如果URL无效或参数不匹配则返回null
     */
    private FileInfo getFileInfoByUrl(String url,String basePath,String path,String fileName) {
        if (StringUtils.isBlank(url) && StringUtils.isBlank(fileName)) {
            return null;
        }
        basePath = (String)ObjectUtils.defaultIfNull(basePath, "");
        path = (String)ObjectUtils.defaultIfNull(path, "");
        fileName = (String)ObjectUtils.defaultIfNull(fileName, "");

        // 获取S3数据源配置信息
        String endPoint = this.s3DataSourceInfo.getEndPoint();
        endPoint = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "endPoint", endPoint);
        if (!endPoint.endsWith("/")) {
            endPoint += "/";
        }
        String bucketName = this.s3DataSourceInfo.getBucketName();
        bucketName = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "bucketName", bucketName);
        String endPointSchema = this.s3DataSourceInfo.getEndPointSchema();
        endPointSchema = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "endPointSchema", endPointSchema);

        // 确定基础路径，优先使用传入的basePath，否则从配置中获取
        if (StringUtils.isBlank(basePath)) {
            basePath = this.s3DataSourceInfo.getBasePath();
            basePath = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "basePath", basePath);
            if (StringUtils.isNotBlank(url) && !url.contains(basePath)){
                basePath = this.s3DataSourceInfo.getTmpPath();
                basePath = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "tmpPath", basePath);
            }
        } else {
            if (basePath.startsWith("/")) {
                basePath = basePath.substring(1);
            }
            if (!basePath.endsWith("/")) {
                basePath +="/";
            }
        }

        if (StringUtils.isNotBlank(path)) {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (!path.endsWith("/")) {
                path +="/";
            }
        }

        // 处理URL：去除查询参数和协议头
        String cleanUrl = url;
        if (StringUtils.isNotBlank(cleanUrl)) {
            if(cleanUrl.contains("?")) {
                cleanUrl = cleanUrl.substring(0, cleanUrl.indexOf("?"));
            }
        }

        // 从URL中提取文件名
        if (StringUtils.isBlank(fileName) && StringUtils.isNotBlank(cleanUrl)) {
            fileName = cleanUrl.replace("http://", "")
                    .replace("https://", "")
                    .replace(bucketName + "." + endPoint, "")
                    .replace(basePath, "")
                    .replace(path, "");

            // 如果path为空且文件名中包含路径分隔符，提取path
            if(StringUtils.isBlank(path) && fileName.lastIndexOf("/") != -1) {
                path = fileName.substring(0, fileName.lastIndexOf("/") + 1);
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
        }

        // 验证URL是否包含必要的配置信息
        if (StringUtils.isNotBlank(cleanUrl)) {
            boolean isValid = true;
            if (!cleanUrl.contains(endPoint) || !cleanUrl.contains(bucketName)) {
                isValid = false;
            }
            if (StringUtils.isNotBlank(basePath) && !cleanUrl.contains(basePath)) {
                isValid = false;
            }
            if (StringUtils.isNotBlank(path) && !cleanUrl.contains(path)) {
                isValid = false;
            }
            if (StringUtils.isNotBlank(fileName) && !cleanUrl.contains(fileName)) {
                isValid = false;
            }

            if (!isValid) {
                return null;
            }

            // 从URL中提取path（如果path为空但basePath存在）
            if (StringUtils.isBlank(path) && StringUtils.isNotBlank(basePath) && StringUtils.isNotBlank(fileName)) {
                int basePathIndex = cleanUrl.indexOf(basePath);
                int fileNameIndex = cleanUrl.indexOf(fileName);
                if (basePathIndex != -1 && fileNameIndex != -1 && fileNameIndex > basePathIndex) {
                    path = cleanUrl.substring(basePathIndex + basePath.length(), fileNameIndex);
                }
            }
        }

        // 如果URL为空，根据配置信息构建完整URL
        if (StringUtils.isBlank(url)) {
            url = endPointSchema + "://" + bucketName + "." + endPoint + basePath + path + fileName;
        } else {
            url = cleanUrl;
        }

        // 构建文件信息对象并设置相关信息
        FileInfo fileInfo = new FileInfo();
        String platform = this.s3DataSourceInfo.getPlatform();
        platform = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "platform", platform);
        fileInfo.setPlatform(platform);
        fileInfo.setUrl(url);
        fileInfo.setBasePath(basePath);
        fileInfo.setPath(path);
        fileInfo.setFilename(fileName);
        if (StringUtils.isNotBlank(fileName) && fileName.lastIndexOf(".") != -1) {
            fileInfo.setExt(fileName.substring(fileName.lastIndexOf(".") + 1));
            fileInfo.setContentType(getContentTypeByFilename(fileName));
        }
        return fileInfo;
    }


    /**
     * 列出指定路径下的文件列表
     *
     * @param basePath 基础路径，用于指定要查询的文件目录。如果以"/"开头会自动去除，
     *                 如果包含多级路径则只取第一级目录
     * @param path 子路径，用于进一步限定文件在基础路径下的具体位置。如果为空则从URL中提取
     * @param filenamePrefix 文件名前缀，用于过滤符合条件的文件。如果为null则不进行前缀过滤
     * @param marker 分页标记，用于标识查询的起始位置，实现分页功能
     * @param maxFiles 最大返回文件数量，取值范围为1-100。如果小于1或大于100则使用默认值100
     * @return ListFilesResult 文件列表结果对象，包含文件信息列表和分页信息。
     *         如果查询失败或无文件则返回空结果
     */
    public ListFilesResult listFiles(String basePath, String path,String filenamePrefix, String marker, Integer maxFiles) {
        ListFilesPretreatment listFilesPretreatment = this.getFileStorageServiceByPath(basePath).listFiles();
        listFilesPretreatment.setMarker(marker);
        if (StringUtils.isNotBlank(filenamePrefix)) {
            listFilesPretreatment.setFilenamePrefix(filenamePrefix);
        }
        if (StringUtils.isNotBlank(path)) {
            if (!path.endsWith("/")) {
                path +="/";
            }

            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            if (path.startsWith(basePath)) {
                path = path.substring(basePath.length());
            }
            listFilesPretreatment.setPath(path);
        }

        Integer defaultMaxFiles = 100;
        maxFiles = (Integer) ObjectUtils.defaultIfNull(maxFiles,defaultMaxFiles);
        if(maxFiles < 1 || maxFiles > defaultMaxFiles) {
            maxFiles = defaultMaxFiles;
        }
        listFilesPretreatment.setMaxFiles(maxFiles);

        ListFilesResult listFilesResult = listFilesPretreatment.listFiles();

        if (null != listFilesResult && CollectionUtils.isNotEmpty(listFilesResult.getFileList())) {
            for (RemoteFileInfo fileInfo : listFilesResult.getFileList()) {
                fileInfo.setUrl(getUrl(fileInfo.getBasePath(),fileInfo.getPath(),fileInfo.getFilename()));
            }
        }
        return listFilesResult;
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
            HttpResponse response = request.execute();
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
     * 上传文件到指定基础路径和子路径
     *
     * <p>该方法用于将文件上传到文件存储系统的指定位置，支持自定义基础路径和相对路径。
     * 会根据basePath自动选择对应的文件存储服务实例进行上传，并自动构建完整的访问URL。</p>
     *
     * @param basePath 基础路径，用于确定使用哪个文件存储服务。如果为null或空则使用默认服务
     * @param path 文件存储的相对路径，用于指定文件在存储桶中的具体位置。如果为null或空则不设置路径
     * @param source 数据源对象，可以是File、InputStream、byte[]等类型，不能为null
     * @param name 文件名称，包含文件扩展名
     * @param contentType 文件内容类型（MIME类型），如"image/jpeg"、"application/pdf"等，如果为null则自动识别
     * @return FileInfo 上传后的文件信息对象，包含文件的URL、路径、大小等信息
     * @throws IllegalArgumentException 当source参数为null时抛出
     * @throws RuntimeException 当获取文件存储服务失败或文件上传时发生异常抛出
     */
    public FileInfo upload(String basePath , String path , Object source, String name, String contentType) {
        FileInfo fileInfo =  this.of(basePath,path,source, name, contentType).upload();
        fileInfo.setUrl(getUrl(fileInfo.getBasePath(),fileInfo.getPath(),fileInfo.getFilename()));
        return fileInfo;
    }

    /**
     * 上传文件到指定路径
     *
     * <p>该方法用于将文件上传到文件存储系统的指定路径，并自动构建完整的访问URL。
     * 支持指定文件存储的相对路径、数据源、文件名和内容类型。</p>
     *
     * @param path 文件存储的相对路径，用于指定文件在存储桶中的具体位置。如果为null或空则不设置路径
     * @param source 数据源对象，可以是File、InputStream、byte[]等类型，不能为null
     * @param name 文件名称，包含文件扩展名
     * @param contentType 文件内容类型（MIME类型），如"image/jpeg"、"application/pdf"等，如果为null则自动识别
     * @return FileInfo 上传后的文件信息对象，包含文件的URL、路径、大小等信息
     * @throws IllegalArgumentException 当source参数为null时抛出
     * @throws RuntimeException 当文件上传失败或处理文件信息时发生异常抛出
     */
    public FileInfo upload(String path , Object source, String name, String contentType) {
        return this.upload(null,path,source,name,contentType);
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
        return this.upload(null,source,name,contentType);
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
     * 临时上传文件到指定子路径
     *
     * <p>该方法用于将文件上传到配置的临时目录下的指定子路径，支持自定义相对路径、数据源、文件名和内容类型。
     * 临时路径从配置中获取，可通过magic.api.s3.{key}.tmpPath进行覆盖。</p>
     *
     * @param path 文件存储的相对路径（相对于临时目录），用于指定文件在临时目录中的具体位置。如果为null或空则不设置子路径
     * @param source 数据源对象，可以是File、InputStream、byte[]等类型，不能为null
     * @param name 文件名称，包含文件扩展名
     * @param contentType 文件内容类型（MIME类型），如"image/jpeg"、"application/pdf"等，如果为null则自动识别
     * @return FileInfo 上传后的文件信息对象，包含文件的URL、路径、大小等信息
     * @throws IllegalArgumentException 当source参数为null时抛出
     * @throws RuntimeException 当获取临时路径配置失败或文件上传时发生异常抛出
     */
    public FileInfo tmpUpload(String path , Object source, String name, String contentType) {
        String tmpPath = this.s3DataSourceInfo.getTmpPath();
        tmpPath = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "tmpPath", tmpPath);
        return this.upload(tmpPath,path,source,name,contentType);
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
        return this.tmpUpload(null, source, name, contentType);
    }

    /**
     * 将临时文件移动到正常存储路径
     *
     * <p>该方法用于将临时上传的文件移动到指定的正常存储路径，支持重命名文件。
     * 实现原理是先下载临时文件，然后上传到目标路径，最后删除临时文件。</p>
     *
     * @param url 临时文件的URL地址，用于定位要移动的源文件
     * @param newPath 目标路径，文件将被移动到的新位置。如果为null或空则使用原文件路径
     * @param newFilename 新文件名，如果为null或空则保持原文件名不变
     * @return FileInfo 移动后的文件信息对象，包含文件的新路径、URL等信息
     * @throws RuntimeException 当获取临时路径配置失败、下载或上传文件时发生异常抛出
     */
    public FileInfo moveTmpToNormal(String url, String newPath,String newFilename) {
        String tmpPath = this.s3DataSourceInfo.getTmpPath();
        tmpPath = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "tmpPath", tmpPath);
        FileStorageService tmpFileStorageService = this.getFileStorageServiceByPath(tmpPath);
        FileInfo fileInfo = tmpFileStorageService.getFileInfoByUrl(url);
        if (fileInfo == null) {
            throw new FileStorageRuntimeException("无法获取临时文件信息，URL可能无效: " + url);
        }
        if (StringUtils.isBlank(newPath)) {
            newPath = fileInfo.getPath();
        }
        if (StringUtils.isBlank(newFilename)) {
            newFilename = fileInfo.getFilename();
        }
        Downloader downloader = tmpFileStorageService.download(fileInfo.getUrl());
        FileInfo newFileInfo = this.upload(newPath,downloader.bytes(),newFilename,fileInfo.getContentType());
        tmpFileStorageService.delete(fileInfo);
        return newFileInfo;
    }


    /**
     * 将临时文件移动到正常存储路径（保持原路径和文件名）
     *
     * <p>该方法是{@link #moveTmpToNormal(String, String, String)}的便捷重载版本，
     * 移动文件时保持原有的路径和文件名不变。</p>
     *
     * @param url 临时文件的URL地址，用于定位要移动的源文件
     * @return FileInfo 移动后的文件信息对象，包含文件的新路径、URL等信息
     */
    public FileInfo moveTmpToNormal(String url) {
        return this.moveTmpToNormal(url,null,null);
    }

    /**
     * 根据文件信息将临时文件移动到正常存储路径
     *
     * <p>该方法从FileInfo对象中提取文件信息，然后将临时文件移动到指定的正常存储路径，
     * 支持重命名文件。实现原理是先下载临时文件，然后上传到目标路径，最后删除临时文件。</p>
     *
     * @param fileInfo 文件信息对象，包含要移动的临时文件的基本信息
     * @param newPath 目标路径，文件将被移动到的新位置。如果为null或空则使用原文件路径
     * @param newFilename 新文件名，如果为null或空则保持原文件名不变
     * @return FileInfo 移动后的文件信息对象，包含文件的新路径、URL等信息
     * @throws IllegalArgumentException 当fileInfo参数为null时抛出
     * @throws RuntimeException 当获取临时路径配置失败、下载或上传文件时发生异常抛出
     */
    public FileInfo moveTmpToNormal(FileInfo fileInfo, String newPath,String newFilename) {
        if (fileInfo == null) {
            throw new FileStorageRuntimeException("fileInfo不能为空");
        }
        String tmpPath = this.s3DataSourceInfo.getTmpPath();
        tmpPath = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "tmpPath", tmpPath);
        FileStorageService tmpFileStorageService = this.getFileStorageServiceByPath(tmpPath);
        if (StringUtils.isBlank(newPath)) {
            newPath = fileInfo.getPath();
        }
        if (StringUtils.isBlank(newFilename)) {
            newFilename = fileInfo.getFilename();
        }
        Downloader downloader = tmpFileStorageService.download(fileInfo.getUrl());
        FileInfo newFileInfo = this.upload(newPath,downloader.bytes(),newFilename,fileInfo.getContentType());
        tmpFileStorageService.delete(fileInfo);
        return newFileInfo;
    }

    /**
     * 根据文件信息将临时文件移动到正常存储路径（保持原路径和文件名）
     *
     * <p>该方法是{@link #moveTmpToNormal(FileInfo, String, String)}的便捷重载版本，
     * 移动文件时保持原有的路径和文件名不变。</p>
     *
     * @param fileInfo 文件信息对象，包含要移动的临时文件的基本信息
     * @return FileInfo 移动后的文件信息对象，包含文件的新路径、URL等信息
     */
    public FileInfo moveTmpToNormal(FileInfo fileInfo) {
        return this.moveTmpToNormal(fileInfo, null, null);
    }

    /**
     * 移动文件到指定路径并重命名
     *
     * <p>该方法用于将文件从当前位置移动到目标路径，并可选择性地重命名文件。
     * 会根据basePath自动选择对应的文件存储服务实例进行移动操作。</p>
     *
     * @param basePath 基础路径，用于确定使用哪个文件存储服务。如果为null或空则使用默认服务
     * @param url 文件的URL路径，指定要移动的源文件位置
     * @param newPath 目标路径，文件将被移动到的新位置
     * @param newFilename 新文件名，如果为null或空则保持原文件名不变
     * @return FileInfo 移动后的文件信息对象，包含文件的新路径、URL等信息
     * @throws RuntimeException 当获取文件存储服务失败或移动文件时发生异常抛出
     */
    public FileInfo move(String basePath , String url, String newPath,String newFilename) {
        FileStorageService fileStorageService = this.getFileStorageServiceByPath(basePath);
        return fileStorageService.move(url).setPath(newPath).setFilename(newFilename).move();
    }

    /**
     * 移动文件到指定路径并重命名（使用默认基础路径）
     *
     * <p>该方法是{@link #move(String, String, String, String)}的便捷重载版本，
     * 使用默认的基础路径进行文件移动操作。</p>
     *
     * @param url 文件的URL路径，指定要移动的源文件位置
     * @param newPath 目标路径，文件将被移动到的新位置
     * @param newFilename 新文件名，如果为null或空则保持原文件名不变
     * @return FileInfo 移动后的文件信息对象，包含文件的新路径、URL等信息
     */
    public FileInfo move( String url, String newPath,String newFilename) {
        return this.move(null,url,newPath,newFilename);
    }

    /**
     * 移动文件到指定路径（保持原文件名）
     *
     * <p>该方法是{@link #move(String, String, String, String)}的便捷重载版本，
     * 移动文件时不改变文件名，使用默认的基础路径。</p>
     *
     * @param url 文件的URL路径，指定要移动的源文件位置
     * @param newPath 目标路径，文件将被移动到的新位置
     * @return FileInfo 移动后的文件信息对象，包含文件的新路径、URL等信息
     */
    public FileInfo move( String url, String newPath) {
        return this.move(url,newPath,null);
    }

    /**
     * 根据文件信息移动文件到新路径并重命名
     *
     * <p>该方法从FileInfo对象中提取基础路径和URL，然后执行文件移动操作。
     * 支持指定新的路径和文件名。</p>
     *
     * @param fileInfo 文件信息对象，包含要移动文件的基本信息（basePath和url）
     * @param newPath 目标路径，文件将被移动到的新位置
     * @param newFilename 新文件名，如果为null或空则保持原文件名不变
     * @return FileInfo 移动后的文件信息对象，包含文件的新路径、URL等信息
     * @throws IllegalArgumentException 当fileInfo参数为null时抛出
     */
    public FileInfo move(FileInfo fileInfo, String newPath,String newFilename) {
        return this.move(fileInfo.getBasePath(),fileInfo.getUrl(),newPath,newFilename);
    }

    /**
     * 移动文件到指定路径
     *
     * <p>该方法用于将文件从当前位置移动到目标路径。
     * 会根据basePath自动选择对应的文件存储服务实例进行移动操作。</p>
     *
     * @param fileInfo 需要移动的文件信息对象
     * @param targetPath 目标路径
     * @return 移动后的文件信息对象
     */
    public FileInfo move(FileInfo fileInfo, String targetPath) {
        return this.move(fileInfo, targetPath, null);
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
     * 生成指定路径下文件上传的预签名URL
     *
     * <p>该方法用于为指定基础路径、子路径和文件名的文件生成预签名URL，支持客户端直接上传文件。
     * 方法会根据basePath自动选择对应的文件存储服务，请求方法根据配置决定使用POST或PUT方式，
     * URL包含过期时间和Content-Type头部信息。</p>
     *
     * @param basePath 基础路径，用于确定使用哪个文件存储服务实例
     * @param path 文件存储的子路径，不能为空
     * @param filename 文件名，用于确定文件类型和设置Content-Type，不能为空
     * @return GeneratePresignedUrlResult 预签名URL生成结果对象，包含完整的上传URL和相关配置
     * @throws NumberFormatException 当配置的过期时间不是有效整数时抛出异常
     */
    public GeneratePresignedUrlResult generateUploadPresignedUrl(String basePath, String path, String filename) {
        if(StringUtils.isBlank(path)) {
            path = null;
        }
        // 根据配置确定请求方法，支持POST和PUT两种方式
        String method =
                "POST".equalsIgnoreCase(this.s3DataSourceInfo.getPresignedUrlMethod()) ?
                        Constant.GeneratePresignedUrl.Method.POST :
                        Constant.GeneratePresignedUrl.Method.PUT;

        // 获取并解析URL过期时间配置
        String expirationStr = this.s3DataSourceInfo.getExpirationSeconds();
        expirationStr = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "expirationSeconds", expirationStr);
        int expiration = Integer.parseInt(expirationStr);

        // 根据basePath选择对应的文件存储服务并生成预签名URL
        return this.getFileStorageServiceByPath(basePath).generatePresignedUrl()
                .setPath(path)
                .setFilename(filename)
                .setMethod(method)
                .setExpiration(DateUtil.offsetSecond(new Date(), expiration))
                .putHeaders(org.dromara.x.file.storage.core.constant.Constant.Metadata.CONTENT_TYPE, getContentTypeByFilename(filename))
                .generatePresignedUrl();
    }


    /**
     * 生成文件上传的预签名URL
     *
     * <p>该方法是一个便捷的重载方法，内部调用三参数版本的generateUploadPresignedUrl方法。
     * 通过传入null作为basePath，将使用默认的文件存储服务实例来生成预签名URL。</p>
     *
     * @param path 文件存储的路径，不能为空
     * @param filename 文件名，用于确定文件类型和设置Content-Type，不能为空
     * @return GeneratePresignedUrlResult 预签名URL生成结果对象，包含完整的上传URL和相关配置
     */
    public GeneratePresignedUrlResult generateUploadPresignedUrl(String path, String filename) {
        return this.generateUploadPresignedUrl(null, path, filename);
    }

    /**
     * 生成文件上传的预签名URL
     *
     * @param filename 文件名
     * @return 生成预签名URL的结果对象
     */
    public GeneratePresignedUrlResult generateUploadPresignedUrl(String filename) {
        return this.generateUploadPresignedUrl(null, null, filename);
    }

    /**
     * 生成文件的预签名URL
     *
     * @param fileInfo 文件信息对象，包含文件的基本信息
     * @return 返回生成的预签名URL字符串
     */
    public String generatePresignedUrl(FileInfo fileInfo) {
        String expirationStr = this.s3DataSourceInfo.getExpirationSeconds();
        expirationStr = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "expirationSeconds", expirationStr);
        int expiration = Integer.parseInt(expirationStr);
        // 调用文件存储服务生成预签名URL，设置过期时间为当前时间加上配置的过期秒数
        return this.getFileStorageServiceByPath(fileInfo.getBasePath()).generatePresignedUrl(fileInfo,
                DateUtil.offsetSecond(new Date(), expiration));
    }

    /**
     * 根据给定的URL和路径信息生成预签名URL
     *
     * <p>该方法用于为指定的文件URL生成预签名URL，支持临时访问控制。
     * 方法会验证URL的有效性，去除查询参数，并检查URL是否包含正确的endpoint、bucket和basePath信息。
     * 如果提供了path参数，还会验证URL中是否包含该path。验证失败则返回原始URL。</p>
     *
     * @param url 原始文件URL地址，如果为空或空白则直接返回
     * @param basePath 基础路径，用于指定文件的存储目录。如果为空或空白则使用数据源配置中的默认值
     * @param path 子路径，可选参数，用于进一步限定文件位置。如果提供则会在验证时检查URL中是否包含该路径
     * @param fileName 文件名，用于构建文件信息对象，如果为空或空白则直接返回原始URL
     * @return 预签名URL字符串，如果输入参数无效或URL不包含正确的endpoint、bucket、basePath等信息则返回原始URL
     */
    public String generatePresignedUrlByUrl(String url,String basePath,String path,String fileName) {
        FileInfo fileInfo = this.getFileInfoByUrl(url,basePath,path,fileName);
        if (fileInfo == null) {
            return url;
        }
        return this.generatePresignedUrl(fileInfo);
    }

    /**
     * 根据给定的URL和路径信息生成预签名URL
     *
     * <p>该方法是一个便捷的重载方法，内部调用四参数版本的generatePresignedUrlByUrl方法。
     * basePath参数传null，将使用数据源配置中的默认basePath。</p>
     *
     * @param url 原始文件URL地址，如果为空或空白则直接返回
     * @param path 子路径，用于进一步限定文件位置
     * @param fileName 文件名，用于构建文件信息对象，如果为空或空白则直接返回原始URL
     * @return 预签名URL字符串，如果输入参数无效或URL不包含必要的配置信息则返回原始URL
     */
    public String generatePresignedUrlByUrl(String url,String path,String fileName) {
        return generatePresignedUrlByUrl(url,null,path,fileName);
    }

    /**
     * 根据给定的URL生成预签名URL
     *
     * @param url 原始文件URL地址
     * @return 预签名URL字符串，如果输入URL为空则返回null
     */
    public String generatePresignedUrlByUrl(String url,String fileName) {
        return generatePresignedUrlByUrl(url, null, fileName);
    }

    /**
     * 根据文件名生成预签名URL
     *
     * @param fileName 文件名
     * @return 预签名URL字符串
     */
    public String generatePresignedUrlByFileName(String fileName) {
        return generatePresignedUrlByUrl(null, fileName);
    }

    /**
     * 根据 file_key 生成可预览的预签名URL
     *
     * <p>file_key 是文件在存储桶中的完整路径（含 basePath），如 "dev/NH/xxx.jpg"。
     * 方法自动去除 basePath 前缀，分离路径和文件名后生成预签名URL。</p>
     *
     * @param fileKey 文件键，如 "dev/NH/T3hFJFxQZMXPzSVD1RLBfHGSVGaNx7.jpg"
     * @return 预签名URL字符串，可直接预览或下载；fileKey为空时返回null
     */
    public String generatePresignedUrlByFileKey(String fileKey) {
        if (StringUtils.isBlank(fileKey)) {
            return null;
        }
        // 去掉 basePath 前缀，避免与配置中的 basePath 重复拼接
        String basePath = this.s3DataSourceInfo.getBasePath();
        basePath = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "basePath", basePath);
        if (StringUtils.isNotBlank(basePath)) {
            if (!basePath.endsWith("/")) basePath += "/";
            if (fileKey.startsWith(basePath)) {
                fileKey = fileKey.substring(basePath.length());
            } else if (fileKey.startsWith("/" + basePath)) {
                fileKey = fileKey.substring(basePath.length() + 1);
            }
        }
        if (fileKey.startsWith("/")) {
            fileKey = fileKey.substring(1);
        }
        // 分离路径和文件名，委托给 generatePresignedUrlByFileName
        return this.generatePresignedUrlByFileName(fileKey);
    }


    /**
     * 根据给定的URL生成预签名URL
     *
     * @param url 原始URL地址，用于生成预签名URL
     * @return 返回生成的预签名URL字符串
     * @throws IllegalArgumentException 当URL或s3DataSourceInfo为空，或S3数据源信息属性为空时抛出异常
     */
    public String generatePresignedUrlByUrl(String url) {
        return this.generatePresignedUrlByUrl(url, null);
    }

    /**
     * 对文件生成可以签名访问的 URL，无法生成则返回 null
     *
     * @param expiration 到期时间
     */
    public String generatePresignedUrl(FileInfo fileInfo, Date expiration) {
        return this.getFileStorageServiceByPath(fileInfo.getBasePath()).generatePresignedUrl(fileInfo, expiration);
    }

    /**
     * 生成文件的预签名URL（缩略图版本）
     *
     * @param fileInfo 文件信息对象，包含文件的基本信息
     * @return 返回生成的预签名URL字符串，用于临时访问文件
     */
    public String generateThPresignedUrl(FileInfo fileInfo) {
        String expirationStr = this.s3DataSourceInfo.getExpirationSeconds();
        expirationStr = MagicAPIConfigUtils.getS3Config(this.s3DataSourceInfo.getKey(), "expirationSeconds", expirationStr);
        int expiration = Integer.parseInt(expirationStr);
        // 调用文件存储服务生成预签名URL，设置过期时间为当前时间加上配置的过期秒数
        return this.getFileStorageServiceByPath(fileInfo.getPath()).generateThPresignedUrl(fileInfo,
                DateUtil.offsetSecond(new Date(), expiration));
    }

    /**
     * 对缩略图文件生成可以签名访问的 URL，无法生成则返回 null
     *
     * @param expiration 到期时间
     */
    public String generateThPresignedUrl(FileInfo fileInfo, Date expiration) {
        return this.getFileStorageServiceByPath(fileInfo.getPath()).generateThPresignedUrl(fileInfo, expiration);
    }

    /**
     * 创建文件上传预处理器（支持指定路径）
     *
     * <p>该方法用于初始化一个上传预处理对象，支持指定基础路径、存储子路径、数据源、文件名和内容类型。
     * 会根据basePath自动选择对应的文件存储服务实例进行处理，并可选择性地设置文件存储的相对路径。</p>
     *
     * @param basePath 基础路径，用于确定使用哪个文件存储服务。如果为null或空则使用默认服务
     * @param path 文件存储的相对路径，用于指定文件在存储桶中的具体位置。如果为null或空则不设置路径
     * @param source 数据源对象，可以是File、InputStream、byte[]等类型，不能为null
     * @param name 文件名称，包含文件扩展名
     * @param contentType 文件内容类型（MIME类型），如"image/jpeg"、"application/pdf"等
     * @return UploadPretreatment 上传预处理器对象，用于配置和执行文件上传操作
     * @throws IllegalArgumentException 当source参数为null时抛出
     * @throws RuntimeException 当获取文件存储服务失败或创建上传预处理器失败时抛出
     */
    public UploadPretreatment of(String basePath , String path , Object source, String name, String contentType) {
        FileStorageService fileStorageService = this.getFileStorageServiceByPath(basePath);
        UploadPretreatment uploadPretreatment = fileStorageService.of(source, name, contentType);
        if (StringUtils.isNotBlank( path)) {
            uploadPretreatment.setPath(path);
        }
        return uploadPretreatment;
    }

    /**
     * 创建文件上传预处理器（支持指定路径，使用默认基础路径）
     *
     * <p>该方法是{@link #of(String, String, Object, String, String)}的便捷重载版本，
     * 使用默认的基础路径进行文件上传预处理器的创建，但允许指定文件存储的相对路径。</p>
     *
     * @param path 文件存储的相对路径，用于指定文件在存储桶中的具体位置。如果为null或空则不设置路径
     * @param source 数据源对象，可以是File、InputStream、byte[]等类型，不能为null
     * @param name 文件名称，包含文件扩展名
     * @param contentType 文件内容类型（MIME类型），如"image/jpeg"、"application/pdf"等
     * @return UploadPretreatment 上传预处理器对象，用于配置和执行文件上传操作
     * @throws IllegalArgumentException 当source参数为null时抛出
     * @throws RuntimeException 当获取文件存储服务失败或创建上传预处理器失败时抛出
     */
    public UploadPretreatment of(String path , Object source, String name, String contentType) {
        return this.of(null,path,source,name,contentType);
    }

    /**
     * 将文件设置为私有访问（private）
     */
    public boolean setPrivate(String url) {
        FileInfo fileInfo = getFileInfoByUrl(url, null, null, null);
        if (fileInfo == null) return false;
        return this.fileStorageService.setFileAcl(fileInfo, Constant.ACL.PRIVATE);
    }

    /**
     * 将文件设置为公开读取（public-read），外部可直接通过URL访问，无需签名。
     */
    public boolean setPublicRead(String url) {
        FileInfo fileInfo = getFileInfoByUrl(url, null, null, null);
        if (fileInfo == null) return false;
        return this.fileStorageService.setFileAcl(fileInfo, Constant.ACL.PUBLIC_READ);
    }

    /**
     * 将文件设置为公开读写（public-read-write），允许外部读取和写入。
     */
    public boolean setPublicReadWrite(String url) {
        FileInfo fileInfo = getFileInfoByUrl(url, null, null, null);
        if (fileInfo == null) return false;
        return this.fileStorageService.setFileAcl(fileInfo, Constant.ACL.PUBLIC_READ_WRITE);
    }

    /**
     * 设置文件的访问控制列表（ACL）
     *
     * <p>指定自定义ACL字符串，适用于特殊权限场景。
     * 常用值见 {@link Constant.ACL}，或使用平台专属常量（如 {@code Constant.AwsS3ACL}）。</p>
     *
     * @param url  文件的完整URL地址
     * @param acl  访问控制列表字符串
     * @return boolean 设置成功返回true，失败返回false
     */
    public boolean setFileAcl(String url, String acl) {
        FileInfo fileInfo = getFileInfoByUrl(url, null, null, null);
        if (fileInfo == null) {
            return false;
        }
        return this.fileStorageService.setFileAcl(fileInfo, acl);
    }

    /**
     * 设置文件的访问控制列表（ACL）
     *
     * <p>根据文件信息对象和ACL字符串设置文件对象的访问权限。</p>
     *
     * @param fileInfo 文件信息对象
     * @param acl      访问控制列表字符串
     * @return boolean 设置成功返回true，失败返回false
     */
    public boolean setFileAcl(FileInfo fileInfo, String acl) {
        if (fileInfo == null) {
            return false;
        }
        return this.fileStorageService.setFileAcl(fileInfo, acl);
    }

    /**
     * 根据条件删除指定路径下的文件
     *
     * <p>该方法用于删除文件存储系统中指定路径和URL的文件，支持通过谓词条件进行过滤判断。
     * 会根据basePath自动选择对应的文件存储服务实例进行删除操作。</p>
     *
     * @param basePath 基础路径，用于确定使用哪个文件存储服务。如果为null或空则使用默认服务
     * @param url 文件的URL路径，指定要删除的文件位置
     * @param predicate 文件信息过滤条件，用于判断是否满足删除条件。如果为null则不进行条件过滤
     * @return boolean 删除成功返回true，删除失败或文件不存在返回false
     * @throws RuntimeException 当获取文件存储服务失败或删除文件时发生异常抛出
     */
    public boolean delete(String basePath,String url, Predicate<FileInfo> predicate) {
        FileStorageService fileStorageService = this.getFileStorageServiceByPath(basePath);
        FileInfo fileInfo = this.getFileInfoByUrl(url,basePath,null,null);
        return fileStorageService.delete(fileInfo, predicate);
    }

    /**
     * 删除文件
     *
     * @param url 文件存储路径
     * @param predicate 文件信息过滤条件
     * @return 删除成功返回true，否则返回false
     */
    public boolean delete(String url, Predicate<FileInfo> predicate) {
        return this.delete(null,url, predicate);
    }

    /**
     * 删除指定URL对应的文件
     *
     * @param url 文件的URL路径
     * @return 删除成功返回true，否则返回false
     */
    public boolean delete(String url) {
        return this.delete(url,null);
    }

    /**
     * 根据条件删除文件
     * @param fileInfo 文件信息对象，包含要删除文件的相关信息
     * @return boolean 删除结果，true表示删除成功，false表示删除失败
     */
    public boolean delete(FileInfo fileInfo) {
        return this.getFileStorageServiceByPath(fileInfo.getPath()).delete(fileInfo);
    }

    /**
     * 根据条件删除文件
     *
     * @param fileInfo 文件信息对象，包含要删除的文件的基本信息
     * @param predicate 删除条件谓词，用于判断是否满足删除条件
     * @return boolean 删除操作是否成功执行
     */
    public boolean delete(FileInfo fileInfo, Predicate<FileInfo> predicate) {
        return this.getFileStorageServiceByPath(fileInfo.getPath()).delete(fileInfo, predicate);
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
        return this.getFileStorageServiceByPath(fileInfo.getBasePath()).delete(fileInfo, fileStorage, fileRecorder, aspectList);
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

        // 对 key 做快照，避免并发写入导致遗漏
        for (String key : new ArrayList<>(this.fileStorageServiceMap.keySet())) {
            FileStorageService service = this.fileStorageServiceMap.remove(key);
            if (service != null) {
                try {
                    service.destroy();
                } catch (Exception e) {
                    log.warn("关闭FileStorageService时发生异常", e);
                }
            }
        }
    }

}
