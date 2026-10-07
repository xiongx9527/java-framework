package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSettingService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.module.S3Module;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * 按当前设置委派的原始文件存储：让 {@code storageType} / {@code storagePath} / {@code s3Key}
 * 三个设置项在控制台改完立即生效，而不必重启。
 *
 * <p>原先 {@code KnowledgeConfiguration} 用 {@code @Bean} 方法里的 if/else 在<b>启动期</b>
 * 决定装哪个实现，装完就固定，页面上改设置无效。现在每次调用都读
 * {@link KbSettingService#current()}：
 * <ul>
 *   <li>{@code storageType=s3} → 委派给 {@link PluginS3KbOriginalFileStore}（它本身每次
 *       调用都从 {@link ObjectProvider} 现取 module 并读当前 {@code s3Key}，天然热生效）</li>
 *   <li>{@code storageType=local} → 委派给 {@link LocalKbOriginalFileStore}，仅当
 *       {@code storagePath} 与缓存实例绑定的值不同时才重建（重建会顺带建目录）</li>
 * </ul>
 *
 * <p>切换存储介质或目录后，此前已落盘文件不在新位置，属预期行为——历史文件路径记录在
 * kb_document.file_path，读不到时由业务层按「文件已迁移」报错。
 */
public class SettingsAwareKbOriginalFileStore implements KbOriginalFileStore {

	/** 知识库设置（每次调用现读，实现 storageType/storagePath 热切换） */
	private final KbSettingService settings;

	/** 本地存储实例与其绑定的 storagePath，用于判断是否需要重建 */
	private volatile LocalKbOriginalFileStore localDelegate;

	private volatile String localDelegatePath;

	/** s3 存储无状态，构造一次即可（s3Key 由它自己每次现读设置） */
	private final KbOriginalFileStore s3Delegate;

	public SettingsAwareKbOriginalFileStore(KbSettingService settings, ObjectProvider<S3Module> s3Modules) {
		this.settings = settings;
		this.s3Delegate = new PluginS3KbOriginalFileStore(s3Modules, settings, "kb");
	}

	@Override
	public String save(String documentId, String fileName, InputStream content) {
		return delegate().save(documentId, fileName, content);
	}

	@Override
	public String saveText(String documentId, String fileName, String text) {
		return delegate().saveText(documentId, fileName, text);
	}

	@Override
	public InputStream read(String path) {
		return delegate().read(path);
	}

	@Override
	public void delete(String path) {
		delegate().delete(path);
	}

	private KbOriginalFileStore delegate() {
		String storageType = settings.current().getStorageType();
		if (KbSettingService.STORAGE_TYPE_S3.equals(storageType)) {
			return s3Delegate;
		}
		return localDelegate();
	}

	private KbOriginalFileStore localDelegate() {
		String path = settings.current().getStoragePath();
		LocalKbOriginalFileStore delegate = localDelegate;
		if (delegate == null || !equal(path, localDelegatePath)) {
			synchronized (this) {
				delegate = localDelegate;
				if (delegate == null || !equal(path, localDelegatePath)) {
					delegate = new LocalKbOriginalFileStore(path);
					localDelegate = delegate;
					localDelegatePath = path;
				}
			}
		}
		return delegate;
	}

	private boolean equal(String left, String right) {
		return left == null ? right == null : left.equals(right);
	}

	/** 供诊断/测试读取当前生效介质与路径。 */
	public String describeCurrent() {
		String type = settings.current().getStorageType();
		String detail = KbSettingService.STORAGE_TYPE_S3.equals(type)
				? "s3Key=" + settings.current().getS3Key()
				: "storagePath=" + (StringUtils.hasText(settings.current().getStoragePath())
				? settings.current().getStoragePath() : "${user.home}/.fw-plus/kb");
		return type + "(" + detail + ")";
	}
}
