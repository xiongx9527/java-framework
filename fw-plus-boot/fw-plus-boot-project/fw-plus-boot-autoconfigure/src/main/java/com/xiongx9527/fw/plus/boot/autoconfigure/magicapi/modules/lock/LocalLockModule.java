package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import lombok.extern.slf4j.Slf4j;
import org.ssssssss.magicapi.core.annotation.MagicModule;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@MagicModule("lock")
public class LocalLockModule implements Lock {

    private final Cache<String, ReentrantLock> localLockCache;

    public LocalLockModule() {
        this.localLockCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .concurrencyLevel(16)
                .removalListener((RemovalListener<String, ReentrantLock>) notification -> {
                    ReentrantLock lock = notification.getValue();
                    if (Objects.nonNull(lock) && lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }).build();
    }

    public ReentrantLock lock(String key) {
        try {
            ReentrantLock lock = this.localLockCache.get(key, ReentrantLock::new);
            lock.lock();
            return lock;
        } catch (Exception e) {
            throw new RuntimeException(I18nMessageResolver.resolveKey("fw.boot.lock.acquireFailed", e.getMessage()), e);
        }
    }

    public ReentrantLock lock(String key, long expire, long acquireTimeout) {
        try {
            if (expire > 0) {
                log.warn("LocalLockModule does not support expire, ignore expire={}", expire);
            }
            ReentrantLock lock = this.localLockCache.get(key, ReentrantLock::new);
            if (lock.tryLock(acquireTimeout, TimeUnit.MILLISECONDS)) {
                return lock;
            }
            throw new RuntimeException(I18nMessageResolver.resolveKey("fw.boot.lock.acquireTimeout"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(I18nMessageResolver.resolveKey("fw.boot.lock.acquireInterrupted"));
        } catch (Exception e) {
            throw new RuntimeException(I18nMessageResolver.resolveKey("fw.boot.lock.acquireFailed", e.getMessage()));
        }
    }

    @Override
    public boolean release(Object lockInfo) {
        if (lockInfo instanceof ReentrantLock) {
            ReentrantLock lock = (ReentrantLock) lockInfo;
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                return true;
            }
            return false;
        }
        log.warn("release called with non-ReentrantLock type: {}", lockInfo.getClass().getName());
        return false;
    }

}
