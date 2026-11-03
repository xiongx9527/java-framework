package com.xiongx9527.fw.plus.ai.base.guard;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DefaultOutboundUrlGuardTest {

    private final DefaultOutboundUrlGuard guard = new DefaultOutboundUrlGuard();

    @Test
    void rejectsLoopbackAndPrivateTargets() {
        assertThrows(OutboundUrlBlockedException.class, () -> guard.check("http://127.0.0.1:8080/x"));
        assertThrows(OutboundUrlBlockedException.class, () -> guard.check("http://localhost/x"));
        assertThrows(OutboundUrlBlockedException.class, () -> guard.check("http://10.0.0.1/x"));
        assertThrows(OutboundUrlBlockedException.class, () -> guard.check("http://192.168.1.1/x"));
        assertThrows(OutboundUrlBlockedException.class, () -> guard.check("http://172.16.0.1/x"));
    }

    @Test
    void rejectsMetadataAndBadScheme() {
        assertThrows(OutboundUrlBlockedException.class, () -> guard.check("http://169.254.169.254/latest"));
        assertThrows(OutboundUrlBlockedException.class, () -> guard.check("file:///etc/passwd"));
        assertThrows(OutboundUrlBlockedException.class, () -> guard.check("ftp://example.com/x"));
        assertThrows(OutboundUrlBlockedException.class, () -> guard.check(null));
    }

    @Test
    void allowsPublicHttps() {
        assertDoesNotThrow(() -> guard.check("https://api.openai.com/v1/chat"));
    }
}
