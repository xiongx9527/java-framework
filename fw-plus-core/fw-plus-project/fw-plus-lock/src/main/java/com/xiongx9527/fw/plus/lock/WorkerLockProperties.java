package com.xiongx9527.fw.plus.lock;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 单 worker 守卫锁配置（{@code fw.plus.lock.*}）。
 *
 * <p>后端选择：{@code fw.plus.lock.type = db | redis}，缺省 {@code db}。</p>
 */
@ConfigurationProperties(prefix = "fw.plus.lock")
public class WorkerLockProperties {

    /** 锁后端：jvm（进程内）/ db（缺省，元数据库行锁）/ redis（Redis SET NX PX）。 */
    private String type = "db";

    /** Redis 锁键前缀；缺省 {@code fw:plus:lock:}。 */
    private String prefix;

    /** Redis 锁 TTL（秒）；进程异常退出时靠它自动过期，缺省 1800。 */
    private long ttlSeconds = 1800L;

    /** 防抖窗口（秒）；一次成功执行后该窗口内的后续启动不再重复执行，缺省 300。 */
    private long debounceSeconds = 300L;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public long getDebounceSeconds() {
        return debounceSeconds;
    }

    public void setDebounceSeconds(long debounceSeconds) {
        this.debounceSeconds = debounceSeconds;
    }

    /** 锁 TTL 的 Duration 表示。 */
    public Duration getTtl() {
        return Duration.ofSeconds(Math.max(0L, ttlSeconds));
    }

    /** 防抖窗口的 Duration 表示。 */
    public Duration getDebounce() {
        return Duration.ofSeconds(Math.max(0L, debounceSeconds));
    }
}
