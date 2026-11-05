package com.xiongx9527.fw.plus.lock;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SingleWorkerGuard} 纯逻辑测试（不依赖 Spring / 数据库 / Redis）。
 */
class SingleWorkerGuardTest {

    /** 可记录调用、可控返回的固定锁后端桩。 */
    private static class StubLock implements WorkerLock {
        private final AtomicBoolean acquired = new AtomicBoolean(false);
        private final boolean wantAcquire;

        StubLock(boolean wantAcquire) {
            this.wantAcquire = wantAcquire;
        }

        @Override
        public boolean tryAcquire(String key) {
            if (wantAcquire && acquired.compareAndSet(false, true)) {
                return true;
            }
            return false;
        }

        @Override
        public void release(String key) {
            acquired.set(false);
        }

        @Override
        public String name() {
            return "stub";
        }
    }

    @Test
    void tryAcquireReturnsTrueWhenBackendGrants() {
        SingleWorkerGuard guard = new SingleWorkerGuard(new StubLock(true));
        assertTrue(guard.tryAcquire("task:1"));
        // 同一 key 重复获取应直接命中已持有，不再走后端
        assertTrue(guard.tryAcquire("task:1"));
    }

    @Test
    void tryAcquireReturnsFalseWhenBackendDenies() {
        SingleWorkerGuard guard = new SingleWorkerGuard(new StubLock(false));
        assertFalse(guard.tryAcquire("task:1"));
    }

    @Test
    void releaseClearsHeldState() {
        SingleWorkerGuard guard = new SingleWorkerGuard(new StubLock(true));
        assertTrue(guard.tryAcquire("task:1"));
        guard.release("task:1");
        // 释放后可再次获取
        assertTrue(guard.tryAcquire("task:1"));
    }

    @Test
    void inProcessFallbackWhenBackendThrows() {
        WorkerLock throwing = new StubLock(true) {
            @Override
            public boolean tryAcquire(String key) {
                throw new RuntimeException("backend down");
            }
        };
        SingleWorkerGuard guard = new SingleWorkerGuard(throwing);
        // 后端异常 -> 退化为进程内守卫，仍允许执行
        assertTrue(guard.tryAcquire("task:1"));
        // 进程内守卫对同 key 只放行一次
        assertTrue(guard.tryAcquire("task:1"));
    }

    @Test
    void blankKeyIsRejected() {
        SingleWorkerGuard guard = new SingleWorkerGuard(new StubLock(true));
        assertFalse(guard.tryAcquire(null));
        assertFalse(guard.tryAcquire(""));
    }
}
