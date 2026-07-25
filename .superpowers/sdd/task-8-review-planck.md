# Task 8 Review Feedback - Planck

## Verdict

- Spec compliance: needs fixes.
- Task quality: needs fixes.
- Critical: none.
- Important: 2.
- Minor: none.

## Strengths

- `DefaultAskDataService` correctly rejects null requests and blank questions with `MODEL_RESPONSE_INVALID`, and delegates valid requests to the flow executor.
- Auto-configuration uses the required `fw.plus.ai.askdata.enabled` condition and default implementations use `@ConditionalOnMissingBean` for many beans.
- The Task 8 diff did not introduce SQLBot naming, Java 9+ APIs, Spring AI, AgentScope, or Java 17 LangGraph4j dependencies.

## Important Findings

1. `AskDataProperties` is not replaceable as required by the global default Bean contract. `@EnableConfigurationProperties(AskDataProperties.class)` registers it unconditionally, so a user-defined same-type Bean can create ambiguous candidates for other default Bean injection. Fix by registering `AskDataProperties` through an explicit `@Bean` with `@ConfigurationProperties(prefix = "fw.plus.ai.askdata")` and `@ConditionalOnMissingBean`, and add a custom properties Bean context test.

2. Custom `AskDataSubAgent` beans are not injected into the default dispatcher/flow. `askDataAgentDispatcher()` constructs an empty `SequentialAskDataAgentDispatcher`, and `askDataFlowExecutor()` does not receive or use the dispatcher. Fix by collecting user `AskDataSubAgent` beans with `ObjectProvider`, wiring them through the dispatcher, and connecting the dispatcher into the actual flow. Add a test proving a custom sub-agent Bean is dispatched by the default flow or otherwise visible in the execution chain.
