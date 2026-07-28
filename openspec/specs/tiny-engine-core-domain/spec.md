# tiny-engine-core-domain Specification

## Purpose
定义 fw-plus-core 中 TinyEngine 低代码核心领域能力的可复用契约，包括领域模型、DTO、Mapper/XML、SQL 初始化资源、核心服务、动态模型操作和安全边界，供后续 Boot starter 与 AI 扩展复用。
## Requirements
### Requirement: TinyEngine core module shall expose reusable domain artifacts

The system SHALL provide a fw-plus-core TinyEngine core module that exposes TinyEngine low-code domain entities, DTOs, common result and exception types, TypeHandlers, Mapper interfaces, Mapper XML resources, SQL initialization resources, and core service interfaces/implementations for upper fw-plus modules to reference.

#### Scenario: Upper module references core artifacts
- **WHEN** an upper fw-plus module adds a Maven dependency on the TinyEngine core artifact
- **THEN** the module can compile against TinyEngine core domain entities, DTOs, Mapper interfaces, Mapper XML resources, SQL resources, and core service contracts without depending on the original `tiny-engine-backend-java` repository

#### Scenario: Core excludes application runtime components
- **WHEN** the TinyEngine core module is built
- **THEN** it MUST NOT require TinyEngine application startup classes, Web Controllers, OpenAPI configuration, servlet filters, scheduled cleanup tasks, Docker deployment files, or Spring Boot starter auto-configuration

### Requirement: TinyEngine core shall be compatible with fw-plus-core runtime constraints

The system SHALL adapt migrated TinyEngine code to Java 8, Spring Boot 2.7.18, and the fw-plus-core Maven/version conventions.

#### Scenario: Java 8 compilation compatibility
- **WHEN** the fw-plus-core TinyEngine core module is compiled with the fw-plus-core Maven Wrapper and Java 8 source/target settings
- **THEN** compilation MUST succeed without Java 9+ APIs, Java 17 language features, Spring Boot 3-only APIs, or Jakarta EE 9+ imports

#### Scenario: Dependency governance compatibility
- **WHEN** the module declares dependencies and participates in fw-plus-core dependency management
- **THEN** it MUST use fw-plus-core compatible dependency versions, `${revision}` / `0.1.1-SNAPSHOT` versioning conventions, and MUST NOT modify the maintained Magic-API 2.2.2 dependency version

### Requirement: TinyEngine core shall preserve low-code domain coverage

The system SHALL preserve the main TinyEngine low-code backend domain capabilities from the reference `base` module, including application/page/material/resource/i18n/platform/tenant/user/task/model/datasource domains and their persistence mappings.

#### Scenario: Main domain mappings are available
- **WHEN** a caller uses TinyEngine core persistence components
- **THEN** Mapper interfaces and XML mappings MUST be available for the main TinyEngine domains, including app, page, page history, page template, canvas, block, block group, material, material history, component, component library, resource, resource group, business category, i18n entry/lang, platform, tenant, user, extension, task record, model, datasource, and relation tables

#### Scenario: SQL initialization resources are distributed
- **WHEN** a downstream module scans TinyEngine core classpath resources
- **THEN** MySQL, H2, and PostgreSQL TinyEngine initialization SQL resources MUST be present in a documented module resource location and MUST NOT be auto-executed by the core module itself

### Requirement: Dynamic model operations shall retain safe data access boundaries

The system SHALL provide dynamic model metadata, dynamic CRUD DTOs, dynamic table lifecycle services, dynamic query/write services, and a centralized SQL identifier validation boundary.

#### Scenario: Valid dynamic model operation
- **WHEN** a caller creates, updates, queries, or deletes dynamic model data using valid model names, field names, condition keys, sort fields, and allowed sort directions
- **THEN** the operation MUST use validated SQL identifiers and parameter-bound values while returning the expected dynamic result structure

#### Scenario: Dangerous table or field input is rejected
- **WHEN** a caller submits a table name, model name, field name, condition key, sort expression, DDL column, or order direction containing SQL operators, separators, comments, whitespace injection, function calls, or other non-identifier content
- **THEN** TinyEngine core MUST reject the operation before SQL execution with a clear validation error

#### Scenario: Dynamic table lifecycle is guarded
- **WHEN** dynamic table create, alter, drop, initialize, batch insert, or structure query operations are invoked
- **THEN** every generated table name and column identifier MUST pass the same centralized identifier validation rules before SQL is sent to JDBC or MyBatis

### Requirement: AI and RAG capabilities shall remain outside this core change

The system SHALL exclude TinyEngine AI Chat, RAG, vector storage, model runtime extension, and LangChain4j 1.x integration from this core capability.

#### Scenario: AI-related reference code is encountered during migration
- **WHEN** implementation reviews reference files such as `AiChatService`, `AiChatController`, `VectorStorageController`, `rag/*`, or LangChain4j 1.x dependencies
- **THEN** those files and dependencies MUST NOT be added to the TinyEngine core module as part of this change
