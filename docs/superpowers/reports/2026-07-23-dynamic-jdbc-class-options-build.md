# Dynamic JDBC Class Options Build Report

- Change: dynamic-jdbc-class-options
- Date: 2026-07-23

## Verification

- PASS: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicJdbcClassOptionProviderTest,MagicAPIControllerJdbcOptionsTest test`
- PASS: `npm run build` in `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console`
- PASS: Standard review findings were addressed before build guard.

## Notes

- `fw-plus-boot/mvnw` could not be used because `.mvn/wrapper/maven-wrapper.properties` and wrapper jar are missing in the current checkout. Verification used system Maven with explicit JDK 8 instead.
- The dynamic options endpoint is implemented without `@Valid(requireLogin = false)` and checks `Authorization.VIEW`, so unauthenticated requests follow Magic-API console authorization rules.
- Existing datasource JSON shape is unchanged; frontend only updates dropdown candidates after login and keeps `magic-select inputable`.
- Review fix: successful empty arrays from `/datasource/options` now intentionally replace the default dropdown arrays, so "no matching class on classpath" is represented accurately.
- Review fix: frontend business exceptions while loading dropdown candidates are logged and downgraded to fallback behavior instead of showing a blocking error dialog.
- Review fix: configured DataSource type candidates are shown when they are present, implement `DataSource`, and have a no-arg constructor compatible with the default datasource builder path.
- Review fix: `/datasource/options` now declares `@Valid(requireLogin = true)` explicitly, and the controller test requires that contract.
- Review fix: `MagicJdbcClassOptionProvider` auto-configuration is guarded with `@ConditionalOnMissingBean` so applications can provide their own candidate provider.
