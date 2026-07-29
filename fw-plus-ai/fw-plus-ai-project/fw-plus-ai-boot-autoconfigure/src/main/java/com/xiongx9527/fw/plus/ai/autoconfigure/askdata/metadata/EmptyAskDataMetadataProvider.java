package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;

import java.util.Collections;

public class EmptyAskDataMetadataProvider implements AskDataMetadataProvider {

    @Override
    public AskDataMetadata load(AskDataRequest request) {
        return new AskDataMetadata(request == null ? null : request.getDialect(), Collections.<AskDataTable>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList());
    }

    @Override
    public AskDataMetadata load(AskDataRequest request, AskDataAuthorizationSummary authorizationSummary) {
        return AskDataMetadataProvider.super.load(request, authorizationSummary);
    }
}
