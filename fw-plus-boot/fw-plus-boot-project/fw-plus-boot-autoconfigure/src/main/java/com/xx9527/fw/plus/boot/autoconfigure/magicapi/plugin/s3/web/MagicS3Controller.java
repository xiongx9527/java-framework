package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.web;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.common.BaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.MyFileStorageService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.MagicDynamicS3DataSource;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.module.S3Module;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.service.S3DataSourceInfoMagicResourceStorage;
import com.xx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.get.ListFilesResult;
import org.springframework.web.bind.annotation.*;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class MagicS3Controller extends MagicController implements MagicExceptionHandler , BaseService {

    /**
     * 构造函数，用于创建MagicS3Controller实例
     *
     * @param configuration 魔法配置对象，包含S3相关的配置信息
     */
    public MagicS3Controller(MagicConfiguration configuration) {
        super(configuration);
    }

    /**
     * 获取所有可用的S3数据源存储信息
     * 
     * @return 包含所有S3数据源信息的JSON响应，每个数据源包含key、name和basePath字段，
     *         其中basePath会从配置中动态获取并确保以"/"结尾（如果不为空）
     */
    @RequestMapping("/s3/datasource/availableStorages")
    @ResponseBody
    public JsonBean<Object> availableStorages() {
        S3DataSourceInfoMagicResourceStorage s3DataSourceInfoMagicResourceStorage = SpringUtils.getBean(S3DataSourceInfoMagicResourceStorage.class);
        List<S3DataSourceInfo> all = ListUtils.emptyIfNull(s3DataSourceInfoMagicResourceStorage.findAll());
        all = all.stream().map(e -> {
            S3DataSourceInfo a = new S3DataSourceInfo();
            a.setKey(e.getKey());
            a.setName(e.getName());
            String basePath = MagicAPIConfigUtils.getS3Config(e.getKey(), "basePath", e.getBasePath());
            if (StringUtils.isNotBlank(basePath) && !basePath.endsWith("/")) {
                basePath += "/";
            }
            a.setBasePath(basePath);
            return a;
        }).collect(Collectors.toList());
        return new JsonBean<>(all);
    }

    /**
     * 列出指定S3数据源中的文件列表
     * 
     * @param key S3数据源的唯一标识符
     * @param basePath 基础路径，可选参数，用于指定查询的起始目录
     * @param path     如果未提供，则使用根目录
     * @param filenamePrefix 文件名前缀，可选参数，用于过滤文件名
     * @param marker 分页标记，可选参数，用于分页查询的起始位置
     * @param maxFiles 最大文件数量，可选参数，限制返回的文件数量
     * @return 包含文件列表结果的JSON响应，如果查询失败则返回空的结果对象
     */
    @RequestMapping("/s3/datasource/{key}/listFilesResult")
    @ResponseBody
    public JsonBean<ListFilesResult> listFilesResult(@PathVariable String key,
                                                     @RequestParam(required = false) String basePath,
                                                     @RequestParam(required = false) String path,
                                                     @RequestParam(required = false) String filenamePrefix,
                                                     @RequestParam(required = false) String marker,
                                                     @RequestParam(required = false) Integer maxFiles) {
        S3Module s3Module = SpringUtils.getBean(S3Module.class).getDynamicAttribute(key);
        try {
            return new JsonBean<>(s3Module.getFileStorageService().listFiles(basePath, path, filenamePrefix, marker, maxFiles));
        } catch (Exception e) {
            return new JsonBean<>(new ListFilesResult());
        }
    }

    /**
     * 删除指定S3数据源中的文件
     * @param key
     * @param url
     * @return
     */
    @PostMapping("/s3/datasource/{key}/delete")
    @ResponseBody
    public JsonBean<Object> delete(@PathVariable String key, @RequestParam String url) {
        S3Module s3Module = SpringUtils.getBean(S3Module.class).getDynamicAttribute(key);
        try {
            s3Module.getFileStorageService().delete(url);
        } catch (Exception e) {
            log.error("删除S3文件失败, key={}, url={}", key, url, e);
            return new JsonBean<>(e.getMessage());
        }
        return new JsonBean<>();
    }

    /**
     * 生成上传预签名URL
     * @param key
     * @param url
     * @return
     */
    @RequestMapping("/s3/datasource/{key}/generateUploadPresignedUrl")
    @ResponseBody
    public JsonBean<String> generateUploadPresignedUrl(@PathVariable String key,
                                                       @RequestParam(required = false) String url,
                                                       @RequestParam(required = false) String basePath,
                                                       @RequestParam(required = false) String path,
                                                       @RequestParam(required = false) String fileName) {
        S3Module s3Module = SpringUtils.getBean(S3Module.class).getDynamicAttribute(key);
        return new JsonBean<>(s3Module.getFileStorageService().generatePresignedUrlByUrl(url,basePath,path,fileName));
    }

    /**
     * 测试S3数据源配置是否正确
     * 通过创建一个临时的S3引擎并尝试连接数据库来验证配置
     *
     * @param properties S3数据源配置信息
     * @return JsonBean 包含测试结果的对象，成功时返回"OK"，失败时返回错误信息
     */
    @RequestMapping("/s3/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody S3DataSourceInfo properties) {
        MyFileStorageService fileStorageService = null;
        try {
            validateField(properties);
            fileStorageService = SpringUtils.getBean(MagicDynamicS3DataSource.class).createFileStorageService(properties);
            FileInfo fileInfo = null;
            // 示例：上传一个测试文件
            try (InputStream testInputStream = new ByteArrayInputStream("test content".getBytes())) {
                fileInfo = fileStorageService.upload(testInputStream, "test.txt");
            }
            if (Objects.isNull(fileInfo)) {
                return new JsonBean<>("上传文件失败");
            }
            fileStorageService.delete(fileInfo);
            return new JsonBean<>("ok");
        } catch (Exception e) {
            log.error("S3数据源连接测试失败", e);
            return new JsonBean<>(e.getMessage());
        } finally {
            if (fileStorageService != null) {
                SpringUtils.getBean(MagicDynamicS3DataSource.class).destroyFileStorageService(fileStorageService);
            }
        }
    }
}
