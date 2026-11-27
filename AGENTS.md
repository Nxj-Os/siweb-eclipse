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

- **Many files are 0-byte placeholders** (~45 of them): controllers (`CheckoutController`, `RackController`, …), services (`FacturacionServiceImpl`, `LimpiezaServiceImpl`, …), entities, templates and even test classes. A filename does not mean the feature exists. `ReservaServiceImpl` and `ReservaServiceTest` now have real content, so the old "empty class" note no longer applies. `ARCHITECTURE.md` tracks what is stubbed — read it before wiring anything to a "service".
- **Implemented so far**: auth (`AuthController`, `AuthPageController`, `HomeController`, `GlobalExceptionHandler` + auth services) and room service end to end: guest carta (`RoomServiceController`, `MenuSuitePageController`, `RoomServiceServiceImpl`, `roomservice/menu-suite.html` + `room-service.js`), employee panel (`EmpleadoFrontendController` → `/empleado/roomservice` → `roomservice/comandas.html` + `room-service-comandas.js`, listado `GET /api/roomservice/pedidos`), and the reservation link (`registrarCheckIn` marks the suite `OCUPADA` and binds `habitacion.tokenQrDinamico`; `POST /cliente/reservas/{id}/check-in` is the guest entry point). Reservations are created `CONFIRMADA` with a QR immediately by default (`app.reserva.pago-directo=true` / `APP_RESERVA_PAGO_DIRECTO`) — no 5-minute hold; `false` restores the old `PENDIENTE_PAGO` flow. Check-in is allowed early as long as the stay has not ended. Employee dashboard feed: `EmpleadoNotificacionesApiController` at `GET /api/empleado/novedades` (reservas + comandas), consumed by `empleado-notificaciones.js` in `recepcion/rack.html` and `roomservice/comandas.html`. Active routes and JSON contracts are in `README.md`.
- **Endpoint collision**: `GET /api/empleado/notificaciones` belongs to `AgendaEmpleadoController` (feed `List<AgendaNotificacion>` used by `static/js/agenda-eclipse.js`). Mapping the new feed there too causes an application-context `Ambiguous mapping` failure; keep it at `/api/empleado/novedades`.
- **Two security chains** in `config/SecurityConfig.java`: `/api/**` (Order 1, JSON 401/403, CSRF via `CookieCsrfTokenRepository`) and the web chain (Order 2, form login). CSRF applies to API POSTs, so MockMvc tests need `.with(csrf())`, and `@WebMvcTest` slices need `@Import(SecurityConfig.class)` plus `@MockitoBean` for each service — copy the pattern from `controller/*Test.java`.
- **Boot 4 test imports differ from Boot 3**: slices live in `org.springframework.boot.webmvc.test.autoconfigure.*` (`WebMvcTest`, `AutoConfigureMockMvc`) and use `@MockitoBean`, not `@MockBean`.
- **Schema is Hibernate-managed**: `ddl-auto=update` against the default PostgreSQL profile, `create-drop` for `demo` and tests. No Flyway/Liquibase — entity changes silently alter the dev database at startup. Back up before non-trivial schema changes.
- **Tests never touch PostgreSQL**: `SiwebEclipseApplicationTests`, `ChatbotApiIntegrationTest` and `DemoCatalogInitializerTest` declare H2 inline via `@SpringBootTest(properties = {...})`; there is no `src/test/resources`. No DB credentials needed for `mvnw test`. Keep it that way — a `@SpringBootTest` without an inline `spring.datasource.url` silently hits the real PostgreSQL.
- **Expected test-log noise** (harmless, do not chase): Mockito dynamic-agent warning, the "generated security password" banner, and the two-`UserDetailsService` warning. (`Habitacion`'s old `columnDefinition = "timestamptz"` used to break H2 DDL; it was removed so all tables — including `habitacion` — now create fine in H2 while PostgreSQL still gets `timestamp with time zone`.)
- **`MxRecordVerifier` performs real DNS lookups** (`DnsMxRecordVerifier`). Any context-loading test must replace it with `@MockitoBean MxRecordVerifier` (see `SiwebEclipseApplicationTests`) or registration tests hit the network.

## Profiles and seed data

- Default profile → PostgreSQL `localhost:5432/siweb_eclipse`, user `postgres`. The password is **hardcoded in `application.properties`**; README/ARCHITECTURE say it comes from `DB_PASSWORD`, but nothing in config or code references that variable — trust the config (override with `SPRING_DATASOURCE_PASSWORD` if needed). Never commit real secrets.
- `demo` → in-memory H2, `create-drop`; all data disappears when the JVM stops.
- `dev`/`demo`/default activate `config/DataInitializer`: roles are always created, but the employee seed accounts (`admin`, `recepcion1`, `roomservice1`) are created **only if all three** env vars `APP_SEED_ADMIN_PASSWORD`, `APP_SEED_RECEPCION_PASSWORD`, `APP_SEED_ROOMSERVICE_PASSWORD` are set — otherwise they are skipped entirely and employee login has no users. Existing accounts are never modified. `config/DemoCatalogInitializer` (`demo`/default, an `ApplicationRunner` with `HIGHEST_PRECEDENCE` so other seeders see its rooms) seeds 7 suites, 3 modalities, 21 tarifas and 16 rooms `LIBRE`. `config/RoomServiceDataInitializer` (same profiles) seeds the room service carta: **7 categories, 25 products** and suite `101` with check-in token `demo-suite-101` → open `/menu-suite?token=demo-suite-101` to walk the whole guest flow. `config/ChatbotDemoDataInitializer` (`demo`/`dev`) only acts when the DB is empty, so it is skipped once the catalog exists.
- `prod` → session cookie gets `Secure`; must be served over HTTPS.
- Rate limits (login lockout, availability endpoint) are in-memory per instance and reset on restart; not distributed controls.

## Reference docs

- `README.md` — commands, env setup, routes, JSON contracts, validation rules.
- `ARCHITECTURE.md` — package/layer map, security, entity tables, which modules are non-functional. It is a snapshot; when it disagrees with code, trust the code.
