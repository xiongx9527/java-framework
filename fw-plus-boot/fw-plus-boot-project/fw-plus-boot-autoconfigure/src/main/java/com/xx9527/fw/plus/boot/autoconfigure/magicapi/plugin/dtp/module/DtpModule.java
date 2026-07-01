package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.module;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.MagicDynamicDtpDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.springframework.util.CollectionUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@MagicModule("dtp")
public class DtpModule implements DynamicAttribute<DtpModule, DtpModule>, DynamicModule<DtpModule> {
    /**
     * 动态线程池数据源
     */
    private final MagicDynamicDtpDataSource magicDynamicDtpDataSource;

    /**
     * 线程池执行器
     */
    private final DtpExecutor dtpExecutor;

    /**
     * 构造函数，初始化DtpModule实例
     *
     * @param magicDynamicDtpDataSource 动态线程池数据源，用于获取线程池配置信息
     * @param dtpExecutor 线程池执行器，用于执行具体的任务
     */
    public DtpModule(MagicDynamicDtpDataSource magicDynamicDtpDataSource, DtpExecutor dtpExecutor) {
        this.magicDynamicDtpDataSource = magicDynamicDtpDataSource;
        this.dtpExecutor = dtpExecutor;
    }

    /**
     * 执行指定的命令任务
     *
     * @param command 要执行的任务命令，不能为空
     */
    public void execute(Runnable command) {
        this.dtpExecutor.execute(command);
    }

    /**
     * 并行执行多个任务并收集所有结果
     *
     * @param tasks 任务列表
     * @param <T> 返回值类型
     * @return CompletableFuture 对象，包含所有任务的结果列表
     */
    public <T> CompletableFuture<List<T>> allOfWithAllResults(List<Supplier<T>> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return null;
        }
        List<CompletableFuture<T>> futures = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(task, this.dtpExecutor))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    /**
     * 并行执行多个任务并返回第一个完成的结果
     *
     * @param tasks 任务列表
     * @param <T> 返回值类型
     * @return CompletableFuture 对象，包含第一个完成任务的结果
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> anyOfWithFirstResult(List<Supplier<T>> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return null;
        }

        List<CompletableFuture<T>> futures = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(task, this.dtpExecutor))
                .collect(Collectors.toList());

        return CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(result -> (T) result);
    }

    /**
     * 并行执行多个任务并等待第一个完成（不关心结果）
     *
     * @param tasks 任务列表
     * @return CompletableFuture 对象，表示第一个完成的任务
     */
    public CompletableFuture<Void> anyOfWithoutResult(List<Runnable> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return null;
        }

        CompletableFuture<?>[] futures = tasks.stream()
                .map(task -> CompletableFuture.runAsync(task, this.dtpExecutor))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.anyOf(futures).thenRun(() -> {});
    }

    /**
     * 获取DtpExecutor实例
     *
     * @return DtpExecutor对象
     */
    public DtpExecutor getDtpExecutor() {
        return this.dtpExecutor;
    }

    /**
     * 根据上下文获取动态模块
     *
     * @param context MagicScript上下文对象，用于获取数据源配置
     * @return DtpModule对象，如果未指定数据源则返回当前模块实例，否则返回基于指定数据源的新模块实例
     */
    @Override
    public DtpModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) {
            return this;
        }
        DtpExecutor client = this.magicDynamicDtpDataSource.getClient(dataSourceKey);
        if (client == null) {
            log.warn("DTP数据源 '{}' 不存在，使用默认线程池", dataSourceKey);
            return this;
        }
        return new DtpModule(this.magicDynamicDtpDataSource, client);
    }

    @Override
    public DtpModule getDynamicAttribute(String key) {
        DtpExecutor client = this.magicDynamicDtpDataSource.getClient(key);
        if (client == null) {
            log.warn("DTP数据源 '{}' 不存在，使用默认线程池", key);
            return this;
        }
        return new DtpModule(this.magicDynamicDtpDataSource, client);
    }

}
