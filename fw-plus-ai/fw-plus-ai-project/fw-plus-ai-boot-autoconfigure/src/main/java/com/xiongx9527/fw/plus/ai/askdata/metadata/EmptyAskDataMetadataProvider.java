package com.xiongx9527.fw.plus.ai.askdata.metadata;

import com.xiongx9527.fw.plus.ai.askdata.AskDataRequest;

import java.util.Collections;

public class EmptyAskDataMetadataProvider implements AskDataMetadataProvider {

    @Override
    public AskDataMetadata load(AskDataRequest request) {
        return new AskDataMetadata(request == null ? null : request.getDialect(), Collections.<AskDataTable>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList());
    }
}
