package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptMessage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpResourceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MagicMcpResourceStorageTest {

    private MagicResourceService magicResourceService;

    private MagicMcpResourceInfoMagicResourceStorage resourceStorage;

    private MagicMcpPromptInfoMagicResourceStorage promptStorage;

    @BeforeEach
    void setUp() {
        magicResourceService = mock(MagicResourceService.class);
        when(magicResourceService.listFiles(anyString())).thenReturn(Collections.emptyList());
        when(magicResourceService.getGroupPath(anyString())).thenReturn("/");
        resourceStorage = new MagicMcpResourceInfoMagicResourceStorage();
        resourceStorage.setMagicResourceService(magicResourceService);
        promptStorage = new MagicMcpPromptInfoMagicResourceStorage();
        promptStorage.setMagicResourceService(magicResourceService);
    }

    @Test
    void resourceStorageShouldExposeFolderAndType() {
        assertThat(resourceStorage.folder()).isEqualTo("mcp-resource");
        assertThat(resourceStorage.suffix()).isEqualTo(".json");
        assertThat(resourceStorage.magicClass()).isEqualTo(MagicMcpResourceInfo.class);
        assertThat(resourceStorage.requirePath()).isFalse();
        assertThat(resourceStorage.requiredScript()).isFalse();
    }

    @Test
    void promptStorageShouldExposeFolderAndType() {
        assertThat(promptStorage.folder()).isEqualTo("mcp-prompt");
        assertThat(promptStorage.suffix()).isEqualTo(".json");
        assertThat(promptStorage.magicClass()).isEqualTo(MagicMcpPromptInfo.class);
    }

    @Test
    void resourceStorageShouldAcceptValidResource() {
        MagicMcpResourceInfo info = new MagicMcpResourceInfo();
        info.setUri("mcp://readme");
        info.setName("readme");
        info.setMimeType("text/markdown");

        resourceStorage.validate(info);

        assertThat(resourceStorage.buildMappingKey(info)).contains("mcp://readme");
    }

    @Test
    void resourceStorageShouldDefaultMimeTypeToTextPlain() {
        MagicMcpResourceInfo info = new MagicMcpResourceInfo();
        info.setUri("/docs/readme");
        info.setName("readme");

        resourceStorage.validate(info);

        assertThat(info.getMimeType()).isEqualTo("text/plain");
    }

    @Test
    void resourceStorageShouldRejectBlankUri() {
        MagicMcpResourceInfo info = new MagicMcpResourceInfo();
        info.setName("readme");

        assertThatThrownBy(() -> resourceStorage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uri");
    }

    @Test
    void resourceStorageShouldRejectBlankName() {
        MagicMcpResourceInfo info = new MagicMcpResourceInfo();
        info.setUri("mcp://readme");

        assertThatThrownBy(() -> resourceStorage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void resourceStorageShouldRejectRelativeUri() {
        MagicMcpResourceInfo info = new MagicMcpResourceInfo();
        info.setUri("readme.txt");
        info.setName("readme");

        assertThatThrownBy(() -> resourceStorage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uri");
    }

    @Test
    void resourceStorageShouldRejectIllegalMimeType() {
        MagicMcpResourceInfo info = new MagicMcpResourceInfo();
        info.setUri("mcp://readme");
        info.setName("readme");
        info.setMimeType("textplain");

        assertThatThrownBy(() -> resourceStorage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mimeType");
    }

    @Test
    void resourceStorageShouldRejectDuplicatedUri() {
        MagicMcpResourceInfo existing = new MagicMcpResourceInfo();
        existing.setId("other-id");
        existing.setUri("mcp://readme");
        when(magicResourceService.listFiles(anyString()))
                .thenReturn(Collections.singletonList((MagicEntity) existing));

        MagicMcpResourceInfo info = new MagicMcpResourceInfo();
        info.setUri("mcp://readme");
        info.setName("readme");

        assertThatThrownBy(() -> resourceStorage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    void resourceStorageShouldRejectUriMutation() {
        MagicMcpResourceInfo old = new MagicMcpResourceInfo();
        old.setUri("mcp://old");
        when(magicResourceService.file(anyString())).thenReturn(old);

        MagicMcpResourceInfo info = new MagicMcpResourceInfo();
        info.setId("resource-id");
        info.setUri("mcp://new");
        info.setName("readme");

        assertThatThrownBy(() -> resourceStorage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uri不可修改");
    }

    @Test
    void promptStorageShouldAcceptValidPrompt() {
        MagicMcpPromptInfo info = promptInfo("greet");
        info.setArguments(Collections.singletonList(argument("name", true)));

        promptStorage.validate(info);

        assertThat(promptStorage.buildMappingKey(info)).contains("greet");
    }

    @Test
    void promptStorageShouldRejectBlankName() {
        MagicMcpPromptInfo info = promptInfo(" ");

        assertThatThrownBy(() -> promptStorage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void promptStorageShouldRejectEmptyMessages() {
        MagicMcpPromptInfo info = new MagicMcpPromptInfo();
        info.setName("greet");

        assertThatThrownBy(() -> promptStorage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages");
    }

    @Test
    void promptStorageShouldRejectBlankMessageContent() {
        MagicMcpPromptInfo info = promptInfo("greet");
        info.setMessages(Collections.singletonList(new MagicMcpPromptMessage("user", " ")));

        assertThatThrownBy(() -> promptStorage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages.content");
    }

    @Test
    void promptStorageShouldRejectUnsupportedRole() {
        MagicMcpPromptInfo info = promptInfo("greet");
        info.setMessages(Collections.singletonList(new MagicMcpPromptMessage("system", "hi")));

        assertThatThrownBy(() -> promptStorage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages.role");
    }

    @Test
    void promptStorageShouldRejectDuplicatedArgumentName() {
        MagicMcpPromptInfo info = promptInfo("greet");
        info.setArguments(Arrays.asList(argument("name", true), argument("name", false)));

        assertThatThrownBy(() -> promptStorage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("重复");
    }

    @Test
    void promptStorageShouldRejectBlankArgumentName() {
        MagicMcpPromptInfo info = promptInfo("greet");
        info.setArguments(Collections.singletonList(argument(" ", true)));

        assertThatThrownBy(() -> promptStorage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("arguments.name");
    }

    @Test
    void promptStorageShouldRejectDuplicatedName() {
        MagicMcpPromptInfo existing = promptInfo("greet");
        existing.setId("other-id");
        when(magicResourceService.listFiles(anyString()))
                .thenReturn(Collections.singletonList((MagicEntity) existing));

        assertThatThrownBy(() -> promptStorage.validate(promptInfo("greet")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    void storageShouldRoundTripJson() {
        MagicMcpResourceInfo info = new MagicMcpResourceInfo();
        info.setUri("mcp://readme");
        info.setName("readme");

        MagicMcpResourceInfo parsed = resourceStorage.read(resourceStorage.write(info));

        assertThat(parsed.getUri()).isEqualTo("mcp://readme");
        assertThat(parsed.getName()).isEqualTo("readme");
    }

    private MagicMcpPromptInfo promptInfo(String name) {
        MagicMcpPromptInfo info = new MagicMcpPromptInfo();
        info.setName(name);
        info.setMessages(Collections.singletonList(new MagicMcpPromptMessage("user", "hello ${name}")));
        return info;
    }

    private MagicMcpPromptMessage.Argument argument(String name, boolean required) {
        MagicMcpPromptMessage.Argument argument = new MagicMcpPromptMessage.Argument();
        argument.setName(name);
        argument.setRequired(required);
        return argument;
    }
}
