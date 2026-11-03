package com.xiongx9527.fw.plus.ai.support.moderation;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationModel;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationResult;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ModeratedLlmPortTest {

    private static final class FlaggingModeration implements ModerationModel {
        private final boolean flagged;
        FlaggingModeration(boolean flagged) { this.flagged = flagged; }
        @Override
        public ModerationResult moderate(String text) {
            return flagged ? ModerationResult.flagged(null, "blocked") : ModerationResult.clean();
        }
    }

    private static final class FixedLlm implements LlmPort {
        @Override public LlmResponse complete(LlmRequest request) {
            return LlmResponse.builder().content("ok").usage(LlmUsage.ZERO).build();
        }
        @Override public void stream(LlmRequest request, LlmStreamHandler handler) {
            handler.onToken("ok", null); handler.onComplete();
        }
    }

    @Test
    public void blocksFlaggedInput() {
        ModeratedLlmPort port = new ModeratedLlmPort(new FixedLlm(),
                new FlaggingModeration(true), ModerationMode.INPUT);
        LlmRequest req = LlmRequest.builder()
                .messages(Collections.singletonList(ChatMessage.user("bad"))).build();
        assertThrows(com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationException.class,
                () -> port.complete(req));
    }

    @Test
    public void passesCleanInput() {
        ModeratedLlmPort port = new ModeratedLlmPort(new FixedLlm(),
                new FlaggingModeration(false), ModerationMode.BOTH);
        LlmRequest req = LlmRequest.builder()
                .messages(Collections.singletonList(ChatMessage.user("hi"))).build();
        assertEquals("ok", port.complete(req).getContent());
    }
}
