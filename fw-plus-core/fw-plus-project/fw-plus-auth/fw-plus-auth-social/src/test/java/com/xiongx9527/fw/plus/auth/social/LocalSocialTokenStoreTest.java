package com.xiongx9527.fw.plus.auth.social;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LocalSocialTokenStore} 的 TTL 与过期语义验证。
 */
public class LocalSocialTokenStoreTest {

    @Test
    void appTokenTtlAndExpiry() throws InterruptedException {
        LocalSocialTokenStore store = new LocalSocialTokenStore();
        store.putAppToken("k", "v", 1L);
        assertThat(store.getAppToken("k")).isEqualTo("v");
        Thread.sleep(1100L);
        assertThat(store.getAppToken("k")).isNull();
    }

    @Test
    void userTokenExpiry() {
        LocalSocialTokenStore store = new LocalSocialTokenStore();
        long past = System.currentTimeMillis() - 1000L;
        CachedUserToken expired = new CachedUserToken("a", "r", "o", "u", past);
        store.putUserToken("u", expired);
        assertThat(store.getUserToken("u")).isNull();

        long future = System.currentTimeMillis() + 600000L;
        CachedUserToken valid = new CachedUserToken("a2", "r2", "o2", "u2", future);
        store.putUserToken("u2", valid);
        CachedUserToken got = store.getUserToken("u2");
        assertThat(got).isNotNull();
        assertThat(got.getAccessToken()).isEqualTo("a2");
        assertThat(got.isExpired(0L)).isFalse();
    }
}
