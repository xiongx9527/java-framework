## ADDED Requirements

### Requirement: Servlet environment uses existing Magic-API path
The system MUST keep Servlet web applications on the existing Magic-API Servlet/MVC auto-configuration path.

#### Scenario: Servlet application enters current implementation
- **WHEN** a Spring Boot Servlet application starts with Magic-API enabled
- **THEN** it uses the current Magic-API Servlet/MVC auto-configuration, controller mappings, auth filter registration, and plugin controller registration path

#### Scenario: Servlet behavior remains compatible
- **WHEN** a Servlet application calls existing Magic-API management endpoints
- **THEN** paths, permission checks, response structures, and plugin controller registration behavior remain compatible with the current implementation

### Requirement: WebFlux environment uses new management endpoint path
The system SHALL route Spring WebFlux applications into a new Magic-API management endpoint adaptation path when Magic-API is enabled and required Magic-API classes are present.

#### Scenario: WebFlux management endpoints are registered through new path
- **WHEN** a Spring Boot WebFlux application starts with Magic-API enabled
- **THEN** the Magic-API management endpoints for console options, resource reload, single resource reload, datasource options, and datasource test are registered through WebFlux-compatible handling rather than the Servlet/MVC controller path

#### Scenario: WebFlux management endpoints are disabled
- **WHEN** a Spring Boot WebFlux application starts with `fw.plus.magicapi.enabled=false`
- **THEN** Magic-API WebFlux management endpoint handlers are not registered

### Requirement: Controller response compatibility
The system MUST keep WebFlux Magic-API management endpoint responses compatible with the equivalent Servlet controller responses.

#### Scenario: Option list response keeps existing shape
- **WHEN** a WebFlux client requests the Magic-API option list endpoint
- **THEN** the response uses the same `JsonBean` result shape and option values as the existing Servlet controller contract

#### Scenario: Datasource option response keeps existing shape
- **WHEN** a WebFlux client requests datasource class options with valid authorization
- **THEN** the response returns the same `MagicJdbcClassOptions` payload shape as the existing Servlet controller contract

#### Scenario: Datasource test response keeps existing shape
- **WHEN** a WebFlux client submits datasource test input
- **THEN** the response returns the same success or error `JsonBean<String>` shape as the existing Servlet controller contract

### Requirement: Servlet-only bean isolation
The system MUST prevent Servlet-only Magic-API beans from being created in a WebFlux-only application context.

#### Scenario: WebFlux context excludes Servlet filter registration
- **WHEN** a Spring Boot WebFlux application starts without a Servlet web application context
- **THEN** Magic-API auto-configuration does not create Servlet `FilterRegistrationBean` instances for Magic-API auth filtering

#### Scenario: WebFlux context excludes MVC mapping dependencies
- **WHEN** a Spring Boot WebFlux application starts without Spring MVC infrastructure
- **THEN** Magic-API auto-configuration does not require Spring MVC `RequestMappingHandlerMapping` or Servlet-only controller registration beans

### Requirement: Scope boundary for dynamic API runtime
The system MUST NOT claim full WebFlux dynamic API runtime support as part of this change.

#### Scenario: Dynamic API runtime remains out of scope
- **WHEN** this change is implemented and verified
- **THEN** acceptance is based on Magic-API management/controller endpoints and Servlet/WebFlux path isolation, not on complete WebFlux support for arbitrary dynamic script API execution
