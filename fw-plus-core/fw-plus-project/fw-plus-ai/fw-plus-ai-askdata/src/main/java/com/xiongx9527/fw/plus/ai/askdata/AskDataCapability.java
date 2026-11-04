package com.xiongx9527.fw.plus.ai.askdata;

/**
 * End-to-end data-QA capability: recall schema -> generate SQL -> (validate) -> execute -> present.
 * Reusable both inline and as a tool (askdata.ask_data) for AiServices.
 */
public interface AskDataCapability {
    AskDataResult ask(AskDataInput input);
}
