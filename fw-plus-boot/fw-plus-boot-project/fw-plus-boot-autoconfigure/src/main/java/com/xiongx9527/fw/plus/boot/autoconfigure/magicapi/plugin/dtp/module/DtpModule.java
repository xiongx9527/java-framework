package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.MagicDynamicDtpDataSource;
import io.jsonwebtoken.lang.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dynamictp.core.executor.DtpExecutor;
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
        if (Collections.isEmpty(tasks)) {
            return null;
        }
        List<CompletableFuture<T>> futures = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(task, this.dtpExecutor)
                        .exceptionally(ex -> {
                            // 处理单个任务异常，避免影响其他任务
                            throw new RuntimeException("Task execution failed", ex);
                        }))
                .collect(Collectors.toList());

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
        return allFutures.handle((v, ex) -> {
            if (ex != null) {
                // 如果等待过程中出现异常，抛出合适的异常
                throw new RuntimeException("Failed to wait for all tasks completion", ex);
            }

            return futures.stream()
                    .map(future -> {
                        try {
                            return future.join();
                        } catch (Exception e) {
                            // 处理join可能抛出的异常
                            throw new RuntimeException("Failed to get task result", e);
                        }
                    })
                    .collect(Collectors.toList());
        });
    }

    /**
     * 并行执行多个任务并返回第一个完成的结果
     *
     * @param tasks 任务列表
     * @param <T> 返回值类型
     * @return CompletableFuture 对象，包含第一个完成任务的结果
     */
    public <T> CompletableFuture<T> anyOfWithFirstResult(List<Supplier<T>> tasks) {
        if (Collections.isEmpty(tasks)) {
            return null;
        }

        List<CompletableFuture<T>> futures = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(task, this.dtpExecutor)
                        .exceptionally(ex -> {
                            // 处理单个任务异常，避免影响其他任务
                            throw new RuntimeException("Task execution failed", ex);
                        }))
                .collect(Collectors.toList());

        // 使用 anyOf 等待任意一个任务完成
        CompletableFuture<Object> anyFuture = CompletableFuture.anyOf(
                futures.toArray(new CompletableFuture[0]));

        // 转换结果类型并返回
        return anyFuture.thenApply(result -> (T) result);
    }

    /**
     * 并行执行多个任务并等待第一个完成（不关心结果）
     *
     * @param tasks 任务列表
     * @return CompletableFuture 对象，表示第一个完成的任务
     */
    public CompletableFuture<Void> anyOfWithoutResult(List<Runnable> tasks) {
        if (Collections.isEmpty(tasks)) {
            return null;
        }

        List<CompletableFuture<Void>> futures = tasks.stream()
                .map(task -> CompletableFuture.runAsync(task, this.dtpExecutor)
                        .exceptionally(ex -> {
                            // 处理单个任务异常，避免影响其他任务
                            log.warn("Task execution failed", ex);
                            return null;
                        }))
                .collect(Collectors.toList());

        // 使用 anyOf 等待任意一个任务完成
        CompletableFuture<Object> anyFuture = CompletableFuture.anyOf(
                futures.toArray(new CompletableFuture[0]));

        // 返回Void类型的CompletableFuture
        return anyFuture.thenRun(() -> {});
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
        // 从上下文中获取数据源键值
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        // 如果数据源键值为空，返回当前模块实例
        if (StringUtils.isEmpty(dataSourceKey)) return this;
        // 根据数据源键值创建新的DtpModule实例
        return new DtpModule(this.magicDynamicDtpDataSource, this.magicDynamicDtpDataSource.getClient(dataSourceKey));
    }

    /**
     * 根据键值获取动态属性对应的模块
     *
     * @param key 属性键值，用于获取对应的数据源客户端
     * @return 基于指定键值对应数据源的新DtpModule实例
     */
    @Override
    public DtpModule getDynamicAttribute(String key) {
        // 根据键值创建新的DtpModule实例
        return new DtpModule(this.magicDynamicDtpDataSource, this.magicDynamicDtpDataSource.getClient(key));
    }

}
