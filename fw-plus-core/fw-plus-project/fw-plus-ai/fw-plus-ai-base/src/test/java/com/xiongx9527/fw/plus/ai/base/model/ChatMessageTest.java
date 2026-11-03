package com.xiongx9527.fw.plus.ai.base.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.model.content.AudioContent;
import com.xiongx9527.fw.plus.ai.base.model.content.ContentPart;
import com.xiongx9527.fw.plus.ai.base.model.content.FileContent;
import com.xiongx9527.fw.plus.ai.base.model.content.ImageContent;
import com.xiongx9527.fw.plus.ai.base.model.content.TextContent;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatMessageTest {

    @Test
    void textOnlyMessageKeepsLegacyContent() {
        ChatMessage message = ChatMessage.user("hello");

        assertEquals(ChatRole.USER, message.getRole());
        assertEquals("hello", message.getContent());
        assertFalse(message.hasMedia());
        assertEquals(1, message.getParts().size());
    }

    @Test
    void multimodalMessageExposesPartsAndConcatenatedText() {
        List<ContentPart> parts = Arrays.asList(
                TextContent.of("what is this"),
                ImageContent.ofUrl("https://example.com/a.png"),
                AudioContent.ofBytes(new byte[]{1, 2}, "audio/wav"),
                FileContent.ofBytes(new byte[]{3}, "application/pdf", "a.pdf"));

        ChatMessage message = ChatMessage.user(parts);

        assertTrue(message.hasMedia());
        assertEquals("what is this", message.getContent());
        assertEquals(4, message.getParts().size());
    }

    @Test
    void mediaOnlyMessageHasNoText() {
        ChatMessage message = ChatMessage.of(ChatRole.USER, ImageContent.ofUrl("https://example.com/a.png"));

        assertEquals(null, message.getContent());
        assertTrue(message.hasMedia());
    }
}
