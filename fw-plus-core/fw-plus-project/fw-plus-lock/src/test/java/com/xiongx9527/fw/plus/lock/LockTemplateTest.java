package com.xiongx9527.fw.plus.lock;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LockTemplate} 统一门面测试（基于桩 {@link WorkerLock}）。
 */
class LockTemplateTest {

    /** 只放行一次、之后一直拒绝的桩。 */
    private static class OneShotLock implements WorkerLock {
        private boolean granted = false;
        @Override public boolean tryAcquire(String key) { if (!granted) { granted = true; return true; } return false; }
        @Override public void release(String key) { granted = false; }
        @Override public String name() { return "stub"; }
    }

    /** 永远拒绝的桩。 */
    private static class DenyLock implements WorkerLock {
        @Override public boolean tryAcquire(String key) { return false; }
        @Override public void release(String key) { }
        @Override public String name() { return "stub"; }
    }

    @Test
    void tryLockOnceSucceeds() {
        LockTemplate template = new LockTemplate(new OneShotLock());
        assertTrue(template.tryLock("k"));
        template.unlock("k");
    }

    @Test
    void tryLockWithWaitTimesOutWhenAlwaysDenied() {
        LockTemplate template = new LockTemplate(new DenyLock());
        long start = System.currentTimeMillis();
        boolean got = template.tryLock("k", Duration.ofMillis(200));
        long cost = System.currentTimeMillis() - start;
        assertFalse(got);
        // 应在约 waitTime 后超时（容许轮询间隔误差）
        assertTrue(cost >= 150, "should wait ~200ms, actual=" + cost);
    }

    @Test
    void tryLockZeroWaitEqualsSingleAttempt() {
        LockTemplate template = new LockTemplate(new DenyLock());
        assertFalse(template.tryLock("k", Duration.ZERO));
    }
}
