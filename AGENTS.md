# AGENTS.md

Single-module Spring Boot 4 monolith (hotel management), Java 21, Maven Wrapper. Package root: `src/main/java/pe/com/eclipsesuites` (entrypoint `SiwebEclipseApplication`). Code identifiers, comments, logs, docs and commit messages are in Spanish — match that when writing code or commits.

## Commands

Maven is not installed globally; always use the wrapper from the repo root:

- Compile: `.\mvnw.cmd compile` (`./mvnw compile` on Unix)
- Full test suite (the only verification gate): `.\mvnw.cmd test`
- Single test class: `.\mvnw.cmd test "-Dtest=PasswordPolicyTest"`; add a method with `-Dtest=PasswordPolicyTest#nombreMetodo`
- Run without PostgreSQL: `$env:SPRING_PROFILES_ACTIVE = "demo"; .\mvnw.cmd spring-boot:run`, then open http://localhost:8080
- No linter, formatter or typecheck plugins are configured, and there are no CI workflows (`.github/` holds only tool hook scripts). Green `mvnw test` = done.

## Gotchas that will cost you time

- **Many files are 0-byte placeholders** (~45 of them): controllers (`CheckoutController`, `RackController`, …), services (`ReservaServiceImpl`, `FacturacionServiceImpl`, …), entities, templates and even test classes (`ReservaServiceTest.java` compiles to an empty class that runs 0 tests). A filename does not mean the feature exists. `ARCHITECTURE.md` tracks exactly what is stubbed — read it before wiring anything to a "service".
- **Implemented so far**: auth (`AuthController`, `AuthPageController`, `HomeController`, `GlobalExceptionHandler` + auth services) and the room service client carta (`RoomServiceController`, `MenuSuitePageController`, `RoomServiceServiceImpl`, `roomservice/menu-suite.html` + `room-service.js`, on branch `feature/roomservice`). Active routes and JSON contracts are in `README.md`.
- **Two security chains** in `config/SecurityConfig.java`: `/api/**` (Order 1, JSON 401/403, CSRF via `CookieCsrfTokenRepository`) and the web chain (Order 2, form login). CSRF applies to API POSTs, so MockMvc tests need `.with(csrf())`, and `@WebMvcTest` slices need `@Import(SecurityConfig.class)` plus `@MockitoBean` for each service — copy the pattern from `controller/*Test.java`.
- **Boot 4 test imports differ from Boot 3**: slices live in `org.springframework.boot.webmvc.test.autoconfigure.*` (`WebMvcTest`, `AutoConfigureMockMvc`) and use `@MockitoBean`, not `@MockBean`.
- **Schema is Hibernate-managed**: `ddl-auto=update` against the default PostgreSQL profile, `create-drop` for `demo` and tests. No Flyway/Liquibase — entity changes silently alter the dev database at startup. Back up before non-trivial schema changes.
- **Tests never touch PostgreSQL**: `SiwebEclipseApplicationTests` declares H2 inline via `@SpringBootTest(properties = {...})`; there is no `src/test/resources`. No DB credentials needed for `mvnw test`.
- **Expected test-log noise** (harmless, do not chase): Mockito dynamic-agent warning, the "generated security password" banner, and the two-`UserDetailsService` warning. (`Habitacion`'s old `columnDefinition = "timestamptz"` used to break H2 DDL; it was removed so all tables — including `habitacion` — now create fine in H2 while PostgreSQL still gets `timestamp with time zone`.)
- **`MxRecordVerifier` performs real DNS lookups** (`DnsMxRecordVerifier`). Any context-loading test must replace it with `@MockitoBean MxRecordVerifier` (see `SiwebEclipseApplicationTests`) or registration tests hit the network.

## Profiles and seed data

- Default profile → PostgreSQL `localhost:5432/siweb_eclipse`, user `postgres`. The password is **hardcoded in `application.properties`**; README/ARCHITECTURE say it comes from `DB_PASSWORD`, but nothing in config or code references that variable — trust the config (override with `SPRING_DATASOURCE_PASSWORD` if needed). Never commit real secrets.
- `demo` → in-memory H2, `create-drop`; all data disappears when the JVM stops.
- `dev`/`demo` activate `config/DataInitializer`: roles are always created, but the employee seed accounts (`admin`, `recepcion1`, `roomservice1`) are created **only if all three** env vars `APP_SEED_ADMIN_PASSWORD`, `APP_SEED_RECEPCION_PASSWORD`, `APP_SEED_ROOMSERVICE_PASSWORD` are set — otherwise they are skipped entirely and employee login has no users. Existing accounts are never modified. `config/RoomServiceDataInitializer` (same profiles) seeds the room service carta: 5 categories, 17 products and suite `101` with check-in token `demo-suite-101` → open `/menu-suite?token=demo-suite-101` to walk the whole guest flow.
- `prod` → session cookie gets `Secure`; must be served over HTTPS.
- Rate limits (login lockout, availability endpoint) are in-memory per instance and reset on restart; not distributed controls.

## Reference docs

- `README.md` — commands, env setup, routes, JSON contracts, validation rules.
- `ARCHITECTURE.md` — package/layer map, security, entity tables, which modules are non-functional. It is a snapshot; when it disagrees with code, trust the code.
