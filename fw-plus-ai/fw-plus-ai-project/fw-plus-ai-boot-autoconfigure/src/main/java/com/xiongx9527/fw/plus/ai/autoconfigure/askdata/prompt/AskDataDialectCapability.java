package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt;

/** SQLBot/WrenAI 连接器在当前 Starter 中的可执行边界描述。 */
public class AskDataDialectCapability {

    private final AskDataDialect dialect;
    private final boolean directExecutionSupported;
    private final String connectorDescription;

    public AskDataDialectCapability(AskDataDialect dialect, boolean directExecutionSupported,
            String connectorDescription) {
        this.dialect = dialect;
        this.directExecutionSupported = directExecutionSupported;
        this.connectorDescription = connectorDescription;
    }

    public AskDataDialect getDialect() { return dialect; }
    public boolean isDirectExecutionSupported() { return directExecutionSupported; }
    public boolean isReadOnlyDescriptor() { return !directExecutionSupported; }
    public String getConnectorDescription() { return connectorDescription; }
}
