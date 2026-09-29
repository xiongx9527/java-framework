package com.xiongx9527.fw.plus.ai.mcp.server.support;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.model.StreamSession;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerStreamableHttpRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultServerStreamableHttpRegistry extends ServerStreamableHttpRegistry {

    /**
     * Constructor
     * @param server
     * @param serverId
     */
    public DefaultServerStreamableHttpRegistry(McpServer server, String serverId) {
        super(server, serverId);
    }

    /**
     * Sends a message to a specific session.
     *
     * @param sessionId the session ID
     * @param json      the JSON string to send
     */
    @Override
    public void sendMsg(String sessionId, String json) {
        StreamSession stream = resolveStreams().get(sessionId);

        if (stream == null)
            throw new IllegalStateException("No Streamable HTTP GET stream for session " + sessionId);

        try {
            stream.send(json);
        } catch (RuntimeException e) {
            closeEventStream(sessionId, stream.getWriter());
            throw e;
        }
    }

    /**
     * Broadcasts a message to all sessions.
     *
     * @param json the JSON string to send
     */
    @Override
    public void broadcastMsg(String json) {
        for (String sessionId : resolveStreams().keySet()) {
            try {
                this.sendMsg(sessionId, json);
            } catch (RuntimeException e) {
                log.debug("Removing failed GET stream for {}", sessionId, e);
            }
        }
    }

}
