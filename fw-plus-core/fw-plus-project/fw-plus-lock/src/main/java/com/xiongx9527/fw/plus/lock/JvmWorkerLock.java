package com.xiongx9527.fw.plus.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JVM（进程内）版单 worker 守卫锁：以每个 key 一把 {@link ReentrantLock} 实现进程内互斥。
 *
 * <p><b>适用场景</b>：单实例部署（无共享数据库 / Redis）时，用进程内锁保证「同一 JVM 内
 * 一个启动任务只执行一次」。多实例场景应改用 {@link DbWorkerLock} 或 {@link RedisWorkerLock}
 * 以获得跨实例互斥。</p>
 *
 * <p><b>语义</b>：{@link #tryAcquire(String)} 非阻塞抢占，key 已被任意线程持有时返回 false；
 * 释放调用 {@link #release(String)}。无防抖窗口概念（进程内不存在「重启风暴」），
 * 故忽略 {@code debounce} 参数。</p>
 */
public class JvmWorkerLock implements WorkerLock {

    private static final Logger log = LoggerFactory.getLogger(JvmWorkerLock.class);

    /** 每个 key 一把可重入锁。 */
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<String, ReentrantLock>();

    @Override
    public boolean tryAcquire(String key) {
        // 空 key 无法定位锁对象，直接拒绝（与 Db/Redis 后端对空 key 的语义保持一致）
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        // 非阻塞抢占：已被本 JVM 其它线程持有时返回 false（单 worker 守卫语义）
        return lock.tryLock();
    }

    @Override
    public void release(String key) {
        ReentrantLock lock = locks.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            // 完全空闲后清理，避免 guard key 长期驻留
            if (!lock.isLocked()) {
                locks.remove(key, lock);
            }
        }
    }

    @Override
    public String name() {
        return "jvm";
    }
}
