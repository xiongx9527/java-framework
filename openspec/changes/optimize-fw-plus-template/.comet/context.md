# Comet Spec Context

- Change: optimize-fw-plus-template
- Phase: design
- Mode: beta
- Context hash: 1c9f804a6d2197291c669bcd2f4f08b01962cce246d3a66b773f657220410d9e

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This beta context pack verbatim-projects spec files and references supporting artifacts by hash, not an agent-authored summary.

## Source References

- Source: openspec/changes/optimize-fw-plus-template/proposal.md
- SHA256: f255169c7c490ac34dda9551d6c3fd38a47f1507f16f2614ea69d084b5103952
- Source: openspec/changes/optimize-fw-plus-template/design.md
- SHA256: 33dd5f2089b02d79b4202596abf6dd89191d16366082548abd6dcc3d06f0d45c
- Source: openspec/changes/optimize-fw-plus-template/tasks.md
- SHA256: a8a8162a50c96e89137ba0f2ce2d178b735776ed5b5bf3ee1a4d82f2c91435a3
- Source: openspec/changes/optimize-fw-plus-template/specs/template-parser-optimization/spec.md
- SHA256: 96477ac2fba81a5eb03dd3299c819ea8449cc264a42655935a7899847737907a

## Acceptance Projection

## openspec/changes/optimize-fw-plus-template/specs/template-parser-optimization/spec.md

- Source: openspec/changes/optimize-fw-plus-template/specs/template-parser-optimization/spec.md
- Lines: 1-90
- SHA256: 96477ac2fba81a5eb03dd3299c819ea8449cc264a42655935a7899847737907a

```md
## ADDED Requirements

### Requirement: TemplateParser provides stable parser entrypoints
The system SHALL provide stable Word, Excel, and HTML template parsing entrypoints through `TemplateParser` without requiring callers to know the concrete parser implementations.

#### Scenario: Word template is parsed through TemplateParser
- **WHEN** a caller invokes the Word data parsing entrypoint with valid Word template bytes and parameters
- **THEN** the system delegates to the configured Word parser and returns the parsed Word bytes

#### Scenario: Excel template is parsed through TemplateParser
- **WHEN** a caller invokes the Excel data parsing entrypoint with valid Excel template bytes and parameters
- **THEN** the system delegates to the configured Excel parser and returns the parsed Excel bytes

#### Scenario: HTML template is parsed through TemplateParser
- **WHEN** a caller invokes the HTML data parsing entrypoint with valid HTML template bytes and parameters
- **THEN** the system delegates to the configured HTML parser and returns UTF-8 encoded parsed HTML bytes

### Requirement: Word URL parsing keeps backward compatibility
The system SHALL expose a correctly spelled `parseWordByUrl` entrypoint while preserving the existing `parsWordByUrl` entrypoint as a compatibility alias.

#### Scenario: Correctly spelled Word URL entrypoint is used
- **WHEN** a caller invokes `parseWordByUrl` with a valid Word template URL and parameters
- **THEN** the system parses the remote Word template through the configured Word parser

#### Scenario: Legacy misspelled Word URL entrypoint remains available
- **WHEN** existing code invokes `parsWordByUrl` with a valid Word template URL and parameters
- **THEN** the system preserves compatibility by producing the same result as `parseWordByUrl`

### Requirement: Missing parser failures are explicit
The system SHALL fail with a clear capability-specific error when a requested parser is unavailable.

#### Scenario: Word parser is unavailable
- **WHEN** a caller invokes a Word parsing entrypoint and no Word parser is configured
- **THEN** the system fails with an error message that identifies the Word parser capability as unavailable

#### Scenario: Excel parser is unavailable
- **WHEN** a caller invokes an Excel parsing entrypoint and no Excel parser is configured
- **THEN** the system fails with an error message that identifies the Excel parser capability as unavailable

#### Scenario: HTML parser is unavailable
- **WHEN** a caller invokes an HTML parsing entrypoint and no HTML parser is configured
- **THEN** the system fails with an error message that identifies the HTML parser capability as unavailable

### Requirement: Invalid input failures are explicit
The system SHALL reject invalid template inputs with clear error information instead of surfacing ambiguous null pointer failures.

#### Scenario: Null template bytes are provided
- **WHEN** a caller invokes a data parsing entrypoint with null template bytes
- **THEN** the system fails with an error message that identifies the input as invalid

#### Scenario: Blank template URL is provided
- **WHEN** a caller invokes a URL parsing entrypoint with a blank template URL
- **THEN** the system fails with an error message that identifies the URL as invalid

### Requirement: Optional conversion capabilities fail predictably
The system SHALL treat optional conversion capabilities as unsupported when the required converter is not configured.

#### Scenario: Word to PDF converter is unavailable
- **WHEN** a caller invokes Word to PDF conversion without a configured Word-to-PDF converter
- **THEN** the system fails with an unsupported-operation error that identifies Word to PDF conversion as unavailable

#### Scenario: HTML to PDF converter is unavailable
- **WHEN** a caller invokes HTML to PDF conversion without a configured HTML-to-PDF converter
- **THEN** the system fails with an unsupported-operation error that identifies HTML to PDF conversion as unavailable

### Requirement: Spring Boot auto-configuration respects user parser beans
The system SHALL create default template parser beans only when required classes are present and the user has not provided an equivalent bean.

#### Scenario: Default parser beans are created
- **WHEN** the required parser implementation classes are available and no user parser beans exist
- **THEN** the auto-configuration creates default Word, Excel, HTML, and TemplateParser beans for the available capabilities

#### Scenario: User parser bean takes precedence
- **WHEN** the application context already contains a user-provided parser bean for a template capability
- **THEN** the auto-configuration does not replace that user-provided parser bean with the default implementation

#### Scenario: TemplateParser uses configured parser beans
- **WHEN** Word, Excel, or HTML parser beans are available in the application context
- **THEN** the auto-configured `TemplateParser` uses those configured parser beans

### Requirement: Magic-API template module remains compatible
The system SHALL keep the Magic-API `template` module methods compatible with existing script calls while delegating to the stabilized `TemplateParser` entrypoints.

#### Scenario: Magic-API Word URL parsing remains available
- **WHEN** a Magic-API script calls `template.parseWordByUrl`
- **THEN** the module delegates to `TemplateParser.parseWordByUrl`

#### Scenario: Magic-API data parsing methods remain available
- **WHEN** a Magic-API script calls existing Word, Excel, or HTML data parsing methods on `template`
- **THEN** the module preserves the existing method names and returns the parser results from `TemplateParser`

```

Full source files remain canonical. If a required heading or scenario is missing here, regenerate the handoff or read the source spec directly. Supporting files (proposal, design, tasks) are referenced by hash only.