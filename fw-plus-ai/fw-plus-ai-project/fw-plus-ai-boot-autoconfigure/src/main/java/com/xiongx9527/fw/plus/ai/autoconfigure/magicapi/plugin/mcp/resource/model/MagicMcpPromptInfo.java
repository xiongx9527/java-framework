package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.ArrayList;
import java.util.List;

@Data
public class MagicMcpPromptInfo extends MagicEntity {

    private String name;

    private String description;

    private boolean enabled = true;

    private List<MagicMcpPromptMessage.Argument> arguments = new ArrayList<MagicMcpPromptMessage.Argument>();

    private List<MagicMcpPromptMessage> messages = new ArrayList<MagicMcpPromptMessage>();
}
