## ADDED Requirements

### Requirement: MP auto configuration activates only on base dependencies

`MpAutoConfiguration` MUST be available when MyBatis-Plus base runtime dependencies and a single `DataSource` are present, without requiring optional MP-Join classes as a global prerequisite.

#### Scenario: Optional Join dependency is not a global prerequisite

- **GIVEN** MyBatis-Plus base classes and a single `DataSource` are available
- **WHEN** optional MP-Join-specific classes are absent
- **THEN** the root MP auto configuration condition does not fail solely because of missing MP-Join classes

### Requirement: MP extension beans are conditionally registered

MP extension auto configurations MUST only register beans when their own classpath, property, and required bean conditions are satisfied.

#### Scenario: Encrypt interceptors are disabled by property

- **GIVEN** `fw.plus.mp-encrypt.enabled=false`
- **WHEN** the MP encrypt auto configuration is applied
- **THEN** encryptor and encrypt/decrypt interceptor beans are not registered by that auto configuration

#### Scenario: Data change interceptor requires handlers

- **GIVEN** data change support is enabled but no `DataChangeHandler` bean exists
- **WHEN** the MP data change auto configuration is applied
- **THEN** the `DataChangeInterceptor` bean is not registered
