## 1. Protocol Contract Tests

- [x] 1.1 Add tests for handshake validation, token/appId failure, and Origin/subprotocol behavior
- [x] 1.2 Add tests for Ping/Pong control frames, text ping compatibility, binary rejection, and invalid text messages
- [x] 1.3 Add tests for lifecycle script once-only execution and failed-handshake no-script behavior
- [x] 1.4 Add tests for server push, multi-session delivery, and Redis/self-origin broadcast behavior where practical

## 2. WebSocket Standardization

- [x] 2.1 Refactor handler message dispatch to separate control frames, text business messages, unsupported messages, and close handling
- [x] 2.2 Standardize close statuses and structured error responses for invalid JSON, missing type, unknown type, script failure, unsupported data, overload, and shutdown
- [x] 2.3 Ensure session registration, cleanup, send locks, cache eviction, and close script execution are idempotent and resource-safe
- [x] 2.4 Review handshake interceptor/registration configuration for token, trusted Origin, and subprotocol negotiation correctness

## 3. Documentation and Verification

- [x] 3.1 Update WS plugin README with standard WebSocket contract, compatibility notes, error response shape, close code mapping, and browser Ping limitations
- [x] 3.2 Run targeted autoconfigure tests and fix regressions within the WS plugin scope
- [x] 3.3 Run the selected Maven verification command for `fw-plus-boot-project/fw-plus-boot-autoconfigure`
