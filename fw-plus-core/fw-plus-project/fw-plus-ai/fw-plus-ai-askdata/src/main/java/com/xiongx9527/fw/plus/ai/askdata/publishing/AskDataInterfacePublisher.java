package com.xiongx9527.fw.plus.ai.askdata.publishing;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
/** AskData 查询接口发布 SPI。 */
public interface AskDataInterfacePublisher { AskDataPublishedInterface publish(AskDataProjectContext context, AskDataPublishRequest request); void unpublish(AskDataProjectContext context, String interfaceId); }
