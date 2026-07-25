package com.xiongx9527.fw.plus.ai.askdata.metadata;

import com.xiongx9527.fw.plus.ai.askdata.AskDataRequest;

public interface AskDataMetadataProvider {

    AskDataMetadata load(AskDataRequest request);
}
