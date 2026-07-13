## ADDED Requirements

### Requirement: FwPlus EasyPoi export SHALL tolerate non-FwPlus export parameters
The system SHALL preserve EasyPoi-compatible export behavior when callers use standard `ExportParams` instead of `FwPlusExportParams`.

#### Scenario: Standard export params are used
- **WHEN** an export path receives a standard `ExportParams` instance
- **THEN** the FwPlus extension SHALL avoid dereferencing missing FwPlus-specific parameters

#### Scenario: FwPlus export translation is disabled
- **WHEN** export parameters are `FwPlusExportParams` and `needTrans` is `false`
- **THEN** the export conversion step SHALL return the original collection without applying field filtering

### Requirement: FwPlus EasyPoi import SHALL tolerate empty or disabled translation settings
The system SHALL handle FwPlus import conversion safely when import parameters are absent, translation is disabled, or field filters are empty.

#### Scenario: Import params are absent
- **WHEN** an import conversion path receives no FwPlus import parameters
- **THEN** the conversion step SHALL return the original collection without throwing a parameter-related exception

#### Scenario: Import translation is disabled
- **WHEN** import parameters are `FwPlusImportParams` and `needTrans` is `false`
- **THEN** the import conversion step SHALL return the original collection without applying field filtering

### Requirement: FwPlus EasyPoi template export SHALL preserve standard template behavior
The system SHALL keep standard EasyPoi template export behavior when callers use standard `TemplateExportParams`.

#### Scenario: Standard template params are used
- **WHEN** template export receives a standard `TemplateExportParams` instance
- **THEN** the FwPlus extension SHALL skip FwPlus-specific translation handling and continue with the standard EasyPoi template export flow

### Requirement: POI stability changes SHALL be covered by targeted verification
The system SHALL include targeted verification for the FwPlus EasyPoi stability boundaries introduced by this change.

#### Scenario: Boundary tests run
- **WHEN** the POI module verification command is executed
- **THEN** tests or compilation SHALL validate the relevant import/export boundary behavior without requiring unrelated modules to change
