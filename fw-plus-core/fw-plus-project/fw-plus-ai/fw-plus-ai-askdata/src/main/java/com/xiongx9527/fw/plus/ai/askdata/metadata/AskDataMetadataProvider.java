package com.xiongx9527.fw.plus.ai.askdata.metadata;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
/** 元数据读取 SPI。 */
public interface AskDataMetadataProvider { AskDataMetadata load(AskDataProjectContext context, String datasourceKey); }
