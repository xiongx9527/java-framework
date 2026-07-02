package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import lombok.extern.slf4j.Slf4j;
import org.ssssssss.magicapi.core.annotation.MagicModule;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@MagicModule("lock")
public class LocalLockModule implements Lock {

    /**
     * 本地锁缓存，用于存储字符串键对应的可重入锁对象
     * 该缓存提供了一种线程安全的方式来获取和管理基于字符串标识的锁
     *
     * @param <String> 锁的键类型，通常为业务标识符
     * @param <ReentrantLock> 锁的值类型，使用可重入锁确保同一线程可以多次获取同一锁
     */
    private final Cache<String, ReentrantLock> localLockCache ;

    /**
     * 构造函数，初始化本地锁模块
     *
     * <p>创建一个基于Guava Cache的本地锁缓存，用于管理ReentrantLock对象
     * 缓存配置包括过期时间、最大容量和并发级别等参数
     *
     * @since 1.0
     */
    public LocalLockModule() {
        // 配置本地锁缓存参数：访问后10分钟过期、最大容量1000、并发级别16
        // 添加移除监听器用于清理ReentrantLock对象，避免内存泄漏
        this.localLockCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .concurrencyLevel(16)
                .removalListener(new RemovalListener<String, ReentrantLock>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, ReentrantLock> notification) {
                        // 清理 ReentrantLock 对象
                        ReentrantLock lock = notification.getValue();
                        if (Objects.nonNull(lock) && lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                        lock = null;
                    }
                }).build();
    }

    /**
     * 获取指定key的可重入锁并立即加锁
     *
     * @param key 锁的唯一标识符，用于区分不同的锁实例
     * @return 获取到的ReentrantLock锁实例
     * @throws RuntimeException 当获取锁过程中发生异常时抛出
     */
    public ReentrantLock lock(String key) {
        try {
            // 使用Guava Cache自动处理锁实例的创建和缓存管理
            ReentrantLock lock = this.localLockCache.get(key, ReentrantLock::new);
            lock.lock();
            return lock;
        } catch (Exception e) {
            // 异常处理
            throw new RuntimeException("获取本地锁失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取指定key的可重入锁，在指定时间内尝试加锁
     *
     * @param key 锁的唯一标识符，用于区分不同的锁实例
     * @param expire 锁的过期时间（暂未使用，保留参数）
     * @param acquireTimeout 获取锁的超时时间，单位毫秒
     * @return 获取到的ReentrantLock锁实例
     * @throws RuntimeException 当获取锁超时、被中断或发生其他异常时抛出
     */
    public ReentrantLock lock(String key, long expire, long acquireTimeout) {
        try {
            // 使用Guava Cache自动处理锁实例的创建和缓存管理
            ReentrantLock lock = this.localLockCache.get(key, ReentrantLock::new);
            if (lock.tryLock(acquireTimeout, TimeUnit.MILLISECONDS)) {
                return lock;
            }
            throw new RuntimeException("获取本地锁超时");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取本地锁被中断");
        } catch (Exception e) {
            throw new RuntimeException("获取本地锁失败: " + e.getMessage());
        }
    }

    /**
     * 释放指定的可重入锁
     *
     * @param lock 需要释放的ReentrantLock锁实例
     * @return 释放成功返回true，锁为null或不被当前线程持有时返回false
     */
    public boolean releaseLock(ReentrantLock lock) {
        if (Objects.nonNull(lock) && lock.isHeldByCurrentThread()) {
            lock.unlock();
            return true;
        }
        return false;
    }

    /**
     * 释放锁资源
     *
     * @param lockInfo 锁信息对象，必须是ReentrantLock类型
     * @return true表示锁释放成功，false表示锁释放失败
     */
    @Override
    public boolean release(Object lockInfo) {
        return releaseLock((ReentrantLock) lockInfo);
    }

}
