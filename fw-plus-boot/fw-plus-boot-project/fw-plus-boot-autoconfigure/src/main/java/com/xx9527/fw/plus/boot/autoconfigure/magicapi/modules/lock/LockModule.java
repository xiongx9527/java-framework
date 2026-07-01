package com.xx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock;

import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.executor.LockExecutor;
import lombok.extern.slf4j.Slf4j;
import org.ssssssss.magicapi.core.annotation.MagicModule;

@Slf4j
@MagicModule("lock")
public class LockModule implements Lock {

    private final LockTemplate template;

    public LockModule(LockTemplate template) {
        this.template = template;
    }

    public LockInfo lock(String key) {
        return this.template.lock(key);
    }

    public LockInfo lock(String key, long expire, long acquireTimeout) {
        return this.template.lock(key, expire, acquireTimeout);
    }

    public LockInfo lock(String key, long expire, long acquireTimeout, Class<? extends LockExecutor> executor) {
        return this.template.lock(key, expire, acquireTimeout, executor);
    }

    public boolean releaseLock(LockInfo lockInfo) {
        return this.template.releaseLock(lockInfo);
    }

    @Override
    public boolean release(Object lockInfo) {
        if (lockInfo instanceof LockInfo) {
            return releaseLock((LockInfo) lockInfo);
        }
        log.warn("release called with non-LockInfo type: {}", lockInfo.getClass().getName());
        return false;
    }

}
