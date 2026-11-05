package com.xiongx9527.fw.plus.lock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link JvmWorkerLock} 进程内锁语义测试。
 */
class JvmWorkerLockTest {

    @Test
    void acquireAndReleaseCycle() {
        JvmWorkerLock lock = new JvmWorkerLock();
        String key = "task:jvm";
        assertTrue(lock.tryAcquire(key));
        lock.release(key);
        // 释放后可再次获取（key 清理后重新创建）
        assertTrue(lock.tryAcquire(key));
        lock.release(key);
        assertTrue(lock.name().equals("jvm"));
    }

    @Test
    void differentKeysAreIndependent() {
        JvmWorkerLock lock = new JvmWorkerLock();
        assertTrue(lock.tryAcquire("a"));
        assertTrue(lock.tryAcquire("b"));
        lock.release("a");
        lock.release("b");
    }

    @Test
    void blankKeyIsRejected() {
        JvmWorkerLock lock = new JvmWorkerLock();
        assertFalse(lock.tryAcquire(null));
        assertFalse(lock.tryAcquire(""));
    }
}
