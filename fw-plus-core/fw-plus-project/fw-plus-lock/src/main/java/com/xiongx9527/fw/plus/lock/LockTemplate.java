package com.xiongx9527.fw.plus.lock;

import java.time.Duration;

/**
 * 统一加锁门面：在 {@link WorkerLock} 三后端（jvm / db / redis）之上提供带等待的通用加锁能力，
 * 供业务代码、Magic {@code lock} 模块、接口防抖等场景复用同一套后端，避免各自再实现一遍。
 *
 * <p>语义：</p>
 * <ul>
 *   <li>{@link #tryLock(String)}：单次尝试（与守卫语义一致，不重试）；</li>
 *   <li>{@link #tryLock(String, Duration)}：在 {@code waitTime} 内轮询重试，直到抢到或超时；</li>
 *   <li>{@link #unlock(String)}：释放。锁的「租约（TTL）」由各后端配置决定（
 *       redis 的 {@code fw.plus.lock.ttl-seconds}、db 的事务持有），本门面只负责等待与释放。</li>
 * </ul>
 */
public class LockTemplate {

    /** 轮询间隔（毫秒）。 */
    private static final long POLL_INTERVAL_MS = 50L;

    private final WorkerLock workerLock;

    public LockTemplate(WorkerLock workerLock) {
        this.workerLock = workerLock;
    }

    /**
     * 单次尝试加锁（不重试）。
     *
     * @param key 锁键
     * @return true 表示获得锁
     */
    public boolean tryLock(String key) {
        return workerLock.tryAcquire(key);
    }

    /**
     * 在等待时间内轮询重试加锁。
     *
     * @param key       锁键
     * @param waitTime  最大等待时间（{@code <=0} 时等价于单次尝试）
     * @return true 表示在超时前获得锁
     */
    public boolean tryLock(String key, Duration waitTime) {
        if (waitTime == null || waitTime.isZero() || waitTime.isNegative()) {
            return workerLock.tryAcquire(key);
        }
        long deadline = System.nanoTime() + waitTime.toNanos();
        do {
            if (workerLock.tryAcquire(key)) {
                return true;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (System.nanoTime() < deadline);
        return false;
    }

    /**
     * 释放锁。
     *
     * @param key 锁键
     */
    public void unlock(String key) {
        workerLock.release(key);
    }

    /** 当前后端名称。 */
    public String backend() {
        return workerLock.name();
    }
}
