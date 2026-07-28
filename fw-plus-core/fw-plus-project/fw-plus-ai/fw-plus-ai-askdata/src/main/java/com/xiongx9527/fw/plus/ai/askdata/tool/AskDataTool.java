package com.xiongx9527.fw.plus.ai.askdata.tool;

/** 工具执行 SPI。 */
public interface AskDataTool { String getKey(); AskDataToolResult execute(AskDataToolRequest request); }
