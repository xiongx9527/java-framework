package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataStepContext;

public interface AskDataContextCompressor {

    AskDataCompressedContext compress(AskDataStepContext context);
}
