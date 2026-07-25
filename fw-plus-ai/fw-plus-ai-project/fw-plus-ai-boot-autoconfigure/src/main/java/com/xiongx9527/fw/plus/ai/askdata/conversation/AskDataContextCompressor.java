package com.xiongx9527.fw.plus.ai.askdata.conversation;

import com.xiongx9527.fw.plus.ai.askdata.flow.AskDataStepContext;

public interface AskDataContextCompressor {

    AskDataCompressedContext compress(AskDataStepContext context);
}
