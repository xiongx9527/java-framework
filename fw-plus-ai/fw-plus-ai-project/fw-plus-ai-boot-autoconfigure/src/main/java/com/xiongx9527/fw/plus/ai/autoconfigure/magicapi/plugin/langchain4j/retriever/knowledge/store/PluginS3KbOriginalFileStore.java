package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSettingService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.MyFileStorageService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.module.S3Module;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.net.URL;

/**
 * 复用 fw-plus-boot magicapi s3 插件的原始文件存储（storageType=s3）：
 * 按知识库设置里的 {@code s3Key} 指定 s3 数据源资源（空则取默认数据源），
 * 上传/读取/删除经 {@link MyFileStorageService} 完成，读取走 S3 预签名 URL。
 *
 * <p>{@code s3Key} 每次 {@link #resolve()} 都从 {@link KbSettingService#current()} 现读，
 * 因此控制台改完 s3Key 立即生效（原实现把它固化在构造参数里，只有重启才生效）。
 */
public class PluginS3KbOriginalFileStore implements KbOriginalFileStore {

	private final ObjectProvider<S3Module> s3Modules;

	private final KbSettingService settings;

	private final String path;

	public PluginS3KbOriginalFileStore(ObjectProvider<S3Module> s3Modules, KbSettingService settings, String path) {
		this.s3Modules = s3Modules;
		this.settings = settings;
		this.path = StringUtils.hasText(path) ? path : "kb";
	}

	private MyFileStorageService resolve() {
		S3Module module = s3Modules == null ? null : s3Modules.getIfAvailable();
		if (module == null) {
			throw new IllegalStateException("storageType=s3 需要启用 fw-plus-boot 的 magicapi s3 插件"
					+ "（fw.plus.magicapi.s3.enabled）并配置 s3 数据源");
		}
		String s3Key = settings == null ? null : settings.current().getS3Key();
		return (StringUtils.hasText(s3Key) ? module.getDynamicAttribute(s3Key) : module)
				.getFileStorageService();
	}

	@Override
	public String save(String documentId, String fileName, InputStream content) {
		byte[] bytes = readAll(content);
		String name = documentId + (StringUtils.hasText(fileName) && fileName.contains(".")
				? fileName.substring(fileName.lastIndexOf('.')) : "");
		try {
			return resolve().upload(path, bytes, name, null).getUrl();
		} catch (Exception e) {
			throw new IllegalStateException("保存知识库原始文件失败（s3 插件）: " + e.getMessage(), e);
		}
	}

	@Override
	public String saveText(String documentId, String fileName, String text) {
		return save(documentId, fileName,
				new java.io.ByteArrayInputStream((text == null ? "" : text).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
	}

	@Override
	public InputStream read(String path) {
		try {
			// 读取走 S3 预签名 URL（s3 插件按数据源凭证生成临时访问链接）
			String presigned = resolve().generatePresignedUrlByUrl(path);
			if (!StringUtils.hasText(presigned)) {
				throw new IllegalStateException("无法生成原始文件的预签名访问链接: " + path);
			}
			return new URL(presigned).openStream();
		} catch (Exception e) {
			throw new IllegalStateException("读取知识库原始文件失败（s3 插件）: " + path, e);
		}
	}

	@Override
	public void delete(String path) {
		try {
			resolve().delete(path);
		} catch (Exception e) {
			throw new IllegalStateException("删除知识库原始文件失败（s3 插件）: " + path, e);
		}
	}

	private static byte[] readAll(InputStream in) {
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int read;
		try {
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			in.close();
		} catch (java.io.IOException e) {
			throw new IllegalStateException("读取文件内容失败", e);
		}
		return out.toByteArray();
	}
}
