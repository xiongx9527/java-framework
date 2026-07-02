package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.MyFileStorageService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.MagicDynamicS3DataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

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
