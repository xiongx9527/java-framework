package com.xiongx9527.fw.plus.ai.interactive.confirm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmationServiceTest {

    @Test
    void issueProducesToken() {
        ConfirmationService s = new ConfirmationService();
        ConfirmToken t = s.issue("ACTION", "p=1", 1L, 1000L);
        assertNotNull(t.getToken());
        assertTrue(t.getExpiresAt() > System.currentTimeMillis());
    }

    @Test
    void verifyOkThenReuseRejected() {
        ConfirmationService s = new ConfirmationService();
        ConfirmToken t = s.issue("ACTION", "p=1", 1L, 60000L);
        s.verify(t, "ACTION", "p=1", 1L);
        ConfirmationException ex = assertThrows(ConfirmationException.class,
                () -> s.verify(t, "ACTION", "p=1", 1L));
        assertTrue(ex.getMessage().contains("used"));
    }

    @Test
    void expiredRejected() {
        ConfirmationService s = new ConfirmationService();
        String digest = ConfirmationService.digest("ACTION|p=1");
        ConfirmToken t = ConfirmToken.builder().actionDigest(digest).uid(1L).expiresAt(1L).build();
        ConfirmationException ex = assertThrows(ConfirmationException.class,
                () -> s.verify(t, "ACTION", "p=1", 1L));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    void uidMismatchRejected() {
        ConfirmationService s = new ConfirmationService();
        ConfirmToken t = s.issue("ACTION", "p=1", 5L, 60000L);
        ConfirmationException ex = assertThrows(ConfirmationException.class,
                () -> s.verify(t, "ACTION", "p=1", 6L));
        assertTrue(ex.getMessage().contains("uid"));
    }

    @Test
    void digestMismatchRejected() {
        ConfirmationService s = new ConfirmationService();
        ConfirmToken t = s.issue("ACTION", "p=1", 1L, 60000L);
        ConfirmationException ex = assertThrows(ConfirmationException.class,
                () -> s.verify(t, "ACTION", "p=2", 1L));
        assertTrue(ex.getMessage().contains("digest"));
    }
}
