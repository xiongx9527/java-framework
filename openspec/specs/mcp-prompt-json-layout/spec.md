# mcp-prompt-json-layout Specification

## Purpose
Define the MCP Prompt console layout and backend JSON contract for editing prompt arguments and messages with structured JSON components.
## Requirements
### Requirement: Prompt arguments use JSON component layout
MCP Prompt console SHALL edit prompt arguments through a JSON component/editor layout rather than plain textarea fields.

#### Scenario: Edit prompt arguments
- **WHEN** a user opens an MCP Prompt resource in the console
- **THEN** the arguments field SHALL be presented in an arguments tab as JSON structured data suitable for an array of argument definitions
- **AND** the active JSON editor SHALL fill the available prompt info panel height instead of using a fixed small height

### Requirement: Prompt messages use JSON component layout
MCP Prompt console SHALL edit prompt messages through a JSON component/editor layout and use MCP-compatible structured message content.

#### Scenario: Edit prompt messages
- **WHEN** a user opens an MCP Prompt resource in the console
- **THEN** the messages field SHALL be presented in a messages tab as JSON structured data suitable for an array of `{role, content}` message objects
- **AND** the active JSON editor SHALL fill the available prompt info panel height instead of using a fixed small height

#### Scenario: Default prompt message layout
- **WHEN** a new MCP Prompt resource is created
- **THEN** the default messages SHALL use structured content such as `{ "type": "text", "text": "..." }` instead of an ambiguous plain string layout

### Requirement: Prompt provider preserves structured JSON
The backend prompt provider SHALL preserve structured JSON arguments and messages and SHALL render nested string placeholders from prompt arguments.

#### Scenario: Render nested content placeholder
- **WHEN** a prompt message contains nested text `{{content}}` inside `content.text`
- **THEN** `prompts/get` SHALL return the nested value rendered with the provided argument

#### Scenario: List prompt arguments
- **WHEN** `prompts/list` is called
- **THEN** the provider SHALL return argument definitions parsed from the JSON argument layout
