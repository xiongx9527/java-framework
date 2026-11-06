package com.xiongx9527.fw.plus.ai.autoconfigure.store;

import com.xiongx9527.fw.plus.ai.base.store.Store;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * 存储后端（向量库 / 图库）通用健康检查：随 Actuator 自动装配。
 *
 * <ul>
 *     <li>端口 {@code configured()=false}（未配置占位）：返回 {@code UNKNOWN}；</li>
 *     <li>已配置且可达：返回 {@code UP}；</li>
 *     <li>已配置但不可达：返回 {@code DOWN}。</li>
 * </ul>
 */
public class StoreHealthIndicator implements HealthIndicator {

    private final Store store;

    public StoreHealthIndicator(Store store) {
        this.store = store;
    }

    @Override
    public Health health() {
        if (store == null || !store.configured()) {
            return Health.unknown()
                    .withDetail("configured", false)
                    .withDetail("reason", "store is not configured")
                    .build();
        }
        try {
            boolean reachable = store.health();
            if (reachable) {
                return Health.up()
                        .withDetail("configured", true)
                        .withDetail("store", store.name())
                        .build();
            }
            return Health.down()
                    .withDetail("configured", true)
                    .withDetail("store", store.name())
                    .withDetail("reason", "store is not reachable")
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("configured", true)
                    .withDetail("store", store.name())
                    .build();
        }
    }
}
