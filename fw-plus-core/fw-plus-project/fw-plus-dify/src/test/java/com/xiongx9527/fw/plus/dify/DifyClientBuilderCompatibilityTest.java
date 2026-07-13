package com.xiongx9527.fw.plus.dify;

import com.xiongx9527.fw.plus.dify.app.IAppBaseClient;
import com.xiongx9527.fw.plus.dify.app.IAppChatClient;
import com.xiongx9527.fw.plus.dify.app.IAppCompletion;
import com.xiongx9527.fw.plus.dify.app.IAppFlowClient;
import com.xiongx9527.fw.plus.dify.dataset.IDatasetClient;

public class DifyClientBuilderCompatibilityTest {

    public void testShouldKeepDatasetEntryCompatible() {
        IDatasetClient client = DifyClientBuilder.dataset("https://api.dify.ai/", "dataset-key").build();

        assertNotNull(client, "dataset entry should build a client");
    }

    public void testShouldKeepAppEntriesCompatible() {
        IAppBaseClient base = DifyClientBuilder.app("https://api.dify.ai/", "app-key").base().build();
        IAppChatClient chat = DifyClientBuilder.app("https://api.dify.ai/", "app-key").chat().build();
        IAppFlowClient flow = DifyClientBuilder.app("https://api.dify.ai/", "app-key").flow().build();
        IAppCompletion completion = DifyClientBuilder.app("https://api.dify.ai/", "app-key").completion().build();

        assertNotNull(base, "base app entry should build a client");
        assertNotNull(chat, "chat app entry should build a client");
        assertNotNull(flow, "flow app entry should build a client");
        assertNotNull(completion, "completion app entry should build a client");
    }

    private static void assertNotNull(Object value, String message) {
        if (value == null) {
            throw new AssertionError(message);
        }
    }
}
