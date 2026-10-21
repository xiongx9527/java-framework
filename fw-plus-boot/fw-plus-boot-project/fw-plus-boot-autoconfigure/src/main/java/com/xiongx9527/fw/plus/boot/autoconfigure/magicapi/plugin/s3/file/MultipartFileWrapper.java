package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * MultipartFile 文件包装类
 */
@Getter
@Setter
@NoArgsConstructor
public class MultipartFileWrapper implements FileWrapper {
    /**
     * 文件属性封装类
     * 用于存储和管理上传文件的相关信息
     */
    private MultipartFile file;

    /**
     * 文件名称
     * 存储文件的原始名称或自定义名称
     */
    private String name;

    /**
     * 文件内容类型
     * 表示文件的MIME类型，如"text/plain"、"image/jpeg"等
     */
    private String contentType;

    /**
     * 文件输入流
     * 提供文件内容的流式读取能力
     */
    private InputStream inputStream;

    /**
     * 文件大小
     * 以字节为单位存储文件的大小，可能为null表示未知大小
     */
    private Long size;

    /**
     * 构造函数，创建MultipartFileWrapper实例
     * @param file 原始MultipartFile对象
     * @param name 文件名称
     * @param contentType 文件内容类型
     * @param size 文件大小
     */
    public MultipartFileWrapper(MultipartFile file, String name, String contentType, Long size) {
        this.file = file;
        this.name = name;
        this.contentType = contentType;
        this.size = size;
    }

    /**
     * 获取文件输入流
     * @return 文件的BufferedInputStream输入流
     * @throws IOException 当获取输入流失败时抛出异常
     */
    @Override
    public InputStream getInputStream() throws IOException {
        // 延迟初始化输入流，避免重复创建
        if (inputStream == null) {
            inputStream = new BufferedInputStream(file.getInputStream());
        }
        return inputStream;
    }

    @Override
    public void transferTo(File dest) {
        // 在某些 SpringBoot 版本中，例如 2.4.6，此方法会调用失败，
        // 此时尝试手动将 InputStream 写入指定文件，
        // 根据文档来看 MultipartFile 最终都会由框架从临时目录中删除
        try {
            file.transferTo(dest);
            IoUtil.close(inputStream);
        } catch (Exception ignored) {
            try {
                FileUtil.writeFromStream(getInputStream(), dest);
            } catch (Exception e) {
                throw new FileStorageRuntimeException(I18nMessageResolver.resolveKey("fw.boot.s3.fileMoveFailed"), e);
            }
        }
    }

    @Override
    public boolean supportTransfer() {
        return true;
    }
}
