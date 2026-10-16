package com.xiongx9527.fw.plus.dify.routes;

import com.xiongx9527.fw.plus.dify.types.DifyRoute;
import com.xiongx9527.fw.plus.dify.types.HttpMethod;

public class AppRoutes {
    public static final DifyRoute INFO = new DifyRoute(
            "/info",
            HttpMethod.GET
    );

    public static final DifyRoute PARAMETERS = new DifyRoute(
            "/parameters",
            HttpMethod.GET
    );

    public static final DifyRoute META_INFO = new DifyRoute(
            "/meta",
            HttpMethod.GET
    );

    public static final DifyRoute WEBAPP_SETTINGS = new DifyRoute(
            "/site",
            HttpMethod.GET
    );

    public static final DifyRoute END_USER = new DifyRoute(
            "/end-users/${endUserId}",
            HttpMethod.GET
    );

    public static final DifyRoute FILE_PREVIEW = new DifyRoute(
            "/files/${fileId}/preview",
            HttpMethod.GET
    );

    public static final DifyRoute APP_FEEDBACKS = new DifyRoute(
            "/app/feedbacks",
            HttpMethod.GET
    );

    public static final DifyRoute CONVERSATION_VARIABLES = new DifyRoute(
            "/conversations/${conversationId}/variables",
            HttpMethod.GET
    );

    public static final DifyRoute CONVERSATION_VARIABLE_SET = new DifyRoute(
            "/conversations/${conversationId}/variables/${variableId}",
            HttpMethod.PUT
    );

    public static final DifyRoute FILE_UPLOAD = new DifyRoute(
            "/files/upload",
            HttpMethod.POST
    );

    public static final DifyRoute MESSAGES = new DifyRoute(
            "/messages",
            HttpMethod.GET
    );

    public static final DifyRoute CHAT_CONVERSATIONS = new DifyRoute(
            "/conversations",
            HttpMethod.GET
    );

    public static final DifyRoute CHAT_MESSAGES = new DifyRoute(
            "/chat-messages",
            HttpMethod.POST
    );

    public static final DifyRoute AUDIO_TO_TEXT = new DifyRoute(
            "/audio-to-text",
            HttpMethod.POST
    );

    public static final DifyRoute TEXT_TO_AUDIO = new DifyRoute(
            "/text-to-audio",
            HttpMethod.POST
    );

    public static final DifyRoute WORKFLOW_RUN = new DifyRoute(
            "/workflows/run",
            HttpMethod.POST
    );

    public static final DifyRoute WORKFLOW_RUN_BY_ID = new DifyRoute(
            "/workflows/${workflowId}/run",
            HttpMethod.POST
    );

    /**
     * 停止工作流任务（仅流式发起的任务可停止）。
     * 文档路径为复数 tasks：POST /workflows/tasks/{task_id}/stop
     */
    public static final DifyRoute WORKFLOW_TASK_STOP = new DifyRoute(
            "/workflows/tasks/${taskId}/stop",
            HttpMethod.POST
    );

    public static final DifyRoute WORKFLOW_EVENTS = new DifyRoute(
            "/workflow/${workflowRunId}/events",
            HttpMethod.GET
    );

    public static final DifyRoute HUMAN_INPUT_FORM_GET = new DifyRoute(
            "/form/human_input/${formToken}",
            HttpMethod.GET
    );

    public static final DifyRoute HUMAN_INPUT_FORM_SUBMIT = new DifyRoute(
            "/form/human_input/${formToken}",
            HttpMethod.POST
    );

    public static final DifyRoute WORKFLOW_LOGS = new DifyRoute(
            "/workflows/logs",
            HttpMethod.GET
    );

    public static final DifyRoute COMPLETION_MESSAGES = new DifyRoute(
            "/completion-messages",
            HttpMethod.POST
    );

    public static final DifyRoute ANNOTATION_GET = new DifyRoute(
            "/apps/annotations",
            HttpMethod.GET
    );

    public static final DifyRoute ANNOTATION_ADD = new DifyRoute(
            "/apps/annotations",
            HttpMethod.POST
    );

    public static final DifyRoute ANNOTATION_SET = new DifyRoute(
            "/apps/annotations/${annotationId}",
            HttpMethod.PUT
    );

    public static final DifyRoute ANNOTATION_DEL = new DifyRoute(
            "/apps/annotations/${annotationId}",
            HttpMethod.DELETE
    );

    public static final DifyRoute ANNOTATION_REPLY_SET = new DifyRoute(
            "/apps/annotation-reply/${action}",
            HttpMethod.POST
    );

    public static final DifyRoute ANNOTATION_REPLY_GET = new DifyRoute(
            "/apps/annotation-reply/${action}/status/${jobId}",
            HttpMethod.GET
    );
}
