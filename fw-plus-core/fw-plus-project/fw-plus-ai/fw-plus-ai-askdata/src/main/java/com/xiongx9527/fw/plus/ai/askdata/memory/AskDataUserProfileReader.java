package com.xiongx9527.fw.plus.ai.askdata.memory;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import java.util.Map;
/** 用户画像读取 SPI，必须按项目和操作人隔离。 */
public interface AskDataUserProfileReader { Map<String,String> read(AskDataProjectContext context); }
