package com.xiongx9527.fw.plus.ai.askdata.agent;
import java.util.List;
/** Core 图状态向外部运行时状态的稳定映射。 */
public final class AskDataAgentGraphStateMapper { private final AskDataAgentGraphState state; private AskDataAgentGraphStateMapper(AskDataAgentGraphState state){this.state=state;} public static AskDataAgentGraphStateMapper fromResults(List<String> results){return new AskDataAgentGraphStateMapper(results==null?AskDataAgentGraphState.FAILED:AskDataAgentGraphState.COMPLETED);} public AskDataAgentGraphState getState(){return state;} }
