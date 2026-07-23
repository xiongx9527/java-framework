## ADDED Requirements

### Requirement: Dynamic connector option loading
Debezium datasource editing SHALL load connector class options from the Debezium plugin backend instead of relying on a connector option list hardcoded in the Vue datasource info component.

#### Scenario: Opening the datasource form
- **WHEN** a user opens the Debezium datasource create or edit form
- **THEN** the connector class select SHALL request connector options from the backend and render the returned options

#### Scenario: Backend support changes
- **WHEN** the backend connector option response changes because available connector support or configuration changes
- **THEN** the connector class select SHALL reflect the backend response without requiring a source edit to the Vue component connector list

### Requirement: Default connector compatibility
The backend SHALL provide default Debezium connector options for the connector classes currently offered by the console when those classes are available at runtime.

#### Scenario: Existing default connectors are available
- **WHEN** the runtime contains the existing MySQL, PostgreSQL, SQL Server, Oracle, or MongoDB Debezium connector classes
- **THEN** the backend connector option response SHALL include the corresponding connector class values

#### Scenario: Connector class is unavailable
- **WHEN** a default connector class cannot be loaded from the runtime classpath
- **THEN** the backend connector option response SHALL omit that connector class from the selectable defaults

### Requirement: Preserve existing custom connector values
The datasource form SHALL preserve and display an existing `connectorClass` value even when that value is not present in the dynamically loaded option list.

#### Scenario: Editing a datasource with a custom connector
- **WHEN** a user edits an existing Debezium datasource whose `connectorClass` is not returned by the backend connector option response
- **THEN** the connector class select SHALL still display the existing value as a selectable option for that form instance

#### Scenario: Dynamic option request fails
- **WHEN** the datasource form cannot load connector options from the backend
- **THEN** the form SHALL continue to display and preserve the current `connectorClass` value if one exists
