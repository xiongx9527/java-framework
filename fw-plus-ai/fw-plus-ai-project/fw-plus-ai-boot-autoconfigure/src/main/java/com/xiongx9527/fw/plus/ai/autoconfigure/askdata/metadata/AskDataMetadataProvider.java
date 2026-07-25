package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;

public interface AskDataMetadataProvider {

    AskDataMetadata load(AskDataRequest request);
}
