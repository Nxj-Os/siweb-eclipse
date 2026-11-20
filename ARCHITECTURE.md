# Arquitectura actual de Siweb Eclipse

Este documento describe el código que existe hoy en el workspace. Es una fotografía del estado actual, no un diseño futuro ni una afirmación de que todas las pantallas del hotel estén conectadas.

## Resumen

Siweb Eclipse es una aplicación web monolítica construida con Spring Boot. El backend Java sirve páginas Thymeleaf y recursos estáticos; el navegador usa `auth-login.js` para autenticar y registrar clientes mediante una API JSON. Spring Security administra autenticación basada en sesión y Hibernate/JPA persiste el modelo disponible en PostgreSQL.

El flujo de acceso, registro de clientes, la página `/inicio` y el asistente conversacional de `/asistente` están implementados. Las áreas de reservas, recepción, room service, operaciones y reportes tienen artefactos parciales (plantillas, repositorios o archivos Java vacíos), pero no se deben considerar módulos funcionales de punta a punta.

## Vista de contexto y componentes

```mermaid
flowchart LR
    Usuario --> Browser[Navegador]
    Browser -->|GET páginas| Thymeleaf[Spring MVC y Thymeleaf]
    Browser -->|fetch JSON + X-XSRF-TOKEN| API[/api/auth y /api/chatbot]
    Browser -->|CSS, JS e imágenes| Static[Recursos estáticos]
    Thymeleaf --> Security[Spring Security]
    API --> Security
    Security --> Session[Sesión HTTP]
    API --> Controller[AuthController y ChatbotController]
    Controller --> AuthService[Servicios de autenticación y chatbot]
    AuthService --> Gemini[Cliente HTTP de Gemini]
    AuthService --> Repository[Repositorios Spring Data JPA]
    Repository --> Hibernate[Hibernate / JPA]
    Hibernate --> PostgreSQL[(PostgreSQL)]
    Tests[Pruebas] --> H2[(H2 en memoria)]
```

### Petición típica de autenticación

1. `AuthPageController` sirve `/login` o `/registro` con Thymeleaf.
2. `auth-login.js` pide `GET /api/auth/csrf`; el servidor emite la cookie `XSRF-TOKEN` y entrega el token.
3. El formulario envía JSON por `fetch` con `credentials: same-origin` y el encabezado `X-XSRF-TOKEN`.
4. La cadena de seguridad API verifica CSRF y la autorización antes de que la petición alcance `AuthController`.
5. `AuthController` delega las reglas en `IClienteAuthService` o `IEmpleadoAuthService`; el servicio valida y utiliza repositorios Spring Data JPA.
6. Al autenticar o crear un cliente, el controlador crea/rota el identificador de sesión, guarda el `SecurityContext` y devuelve un DTO JSON.
7. El navegador conserva la sesión y redirige a `/inicio`.

### Petición típica del asistente

1. `AsistentePageController` sirve `/asistente` con Thymeleaf; el widget flotante se carga en la portada y en `/inicio`.
2. El JavaScript pide `GET /api/chatbot/estado` y `GET /api/chatbot/catalogo`; el catálogo se arma desde `Habitacion`, `TipoHabitacion` y `TarifaHabitacion`.
3. El formulario envía `POST /api/chatbot/mensaje` con `credentials: same-origin` y `X-XSRF-TOKEN`.
4. `ChatbotController` identifica al usuario por sesión o por la cookie `ECLIPSE_CHAT_SID` (anónima, `HttpOnly`, `SameSite=Lax`, 30 días) y delega en `IChatbotService`.
5. `ChatbotServiceImpl` aplica el límite de mensajes, clasifica la intención, decide el modo (público, cliente o cliente con respaldo) y arma el contexto con el catálogo real y el resumen de la conversación.
6. Según la intención, responde con una respuesta predefinida o llama a `GeminiClient`, que hace una petición HTTP a la API de Gemini con `GEMINI_API_KEY`.
7. Los mensajes se persisten en `conversacion_chatbot` y `mensaje_chatbot`; la derivación a recepción solo actualiza `derivo_a_reserva` y el estado de cola en memoria.

## Capas y paquetes

Raíz Java: `src/main/java/pe/com/eclipsesuites`.

| Paquete | Archivos Java encontrados | Responsabilidad/estado |
| --- | ---: | --- |
| raíz | 1 | Punto de entrada `SiwebEclipseApplication`. |
| `config` | 5 | Cadenas de seguridad, codificador BCrypt, configuración del chatbot, carga de suites de demo y datos de desarrollo. |
| `controller` | 12 | Rutas API/MVC y manejo de errores. `AuthController`, `AuthPageController`, `ChatbotController`, `AsistentePageController`, `HomeController` y `GlobalExceptionHandler` tienen contenido; el resto son marcadores vacíos. |
| `dao` | 19 | Interfaces Spring Data JPA para los datos del hotel, incluidos `ConversacionChatbotRepository` y `MensajeChatbotRepository`. |
| `dto/request` | 8 | Contratos de solicitudes, incluidos login, registro y `ChatMensajeRequestDTO`. |
| `dto/response` | 11 | Respuestas JSON, errores, disponibilidad y los DTO del chatbot. |
| `entity` | 21 | Modelos Java. Hay 11 clases `@Entity` y un tipo `@Embeddable` con contenido; otros archivos son marcadores vacíos. |
| `service` | 9 | Interfaces, política de consulta MX e `IChatbotService`. |
| `service/impl` | 21 | Reglas de autenticación, correo, contraseña, límites y el módulo de chatbot; algunos archivos de otros dominios están vacíos. |
| `service/gemini` | 3 | Cliente HTTP de Gemini, DTO de turno y excepción de servicio no disponible. |
| `validation` | 3 | Validación de teléfono por país. |
| `util` | 2 | Generador de códigos y constantes SUNAT. |

Las cantidades se obtuvieron contando los archivos `.java` presentes, incluidos los archivos vacíos. No representan funcionalidades terminadas.

### Puntos de entrada actuales

- `SiwebEclipseApplication.main`: inicializa Spring Boot.
- `HomeController`: sirve `/inicio` con los tipos de suite y tarifas actuales; exige una sesión autenticada y, de lo contrario, redirige a `/login`.
- `CatalogoController`: sirve `/catalogo` y `/catalogo/{codTipo}` sin sesión y expone `/api/catalogo/**` como JSON público.
- `AuthPageController`: sirve los formularios Thymeleaf de login y registro.
- `AuthController`: API de autenticación bajo `/api/auth`.
- `ChatbotController`: API del asistente bajo `/api/chatbot`.
- `AsistentePageController`: sirve la página pública `/asistente`.
- `ChatbotDemoDataInitializer`: siembra tipos, tarifas y suites con los perfiles `demo`/`dev`.
- `SecurityConfig`: configura una cadena para `/api/**` y otra para las páginas web.
- `src/main/resources/static/index.html`: portada estática servida por Spring Boot.

## Rutas y seguridad

| Método | Ruta | Acceso/resultado |
| --- | --- | --- |
| `GET` | `/` y `/index.html` | Portada estática pública. |
| `GET` | `/login?tipo=empleado` o `?tipo=cliente` | Formulario público; el parámetro selecciona la identidad a pedir. |
| `GET` | `/registro` | Formulario público; Thymeleaf incluye los dominios permitidos configurados. |
| `GET` | `/inicio` | Requiere autenticación; no diferencia la vista por rol. |
| `GET` | `/catalogo` | Público; lista suites con disponibilidad, precio desde y filtros `q`, `fecha`, `hora`, `modalidadId`, `soloDisponibles` y `orden`. |
| `GET` | `/catalogo/{codTipo}` | Público; ficha de la suite con amenidades, tarifas y habitaciones. |
| `GET` | `/api/catalogo` | Público; la misma lista del catálogo en JSON. |
| `GET` | `/api/catalogo/{codTipo}` | Público; detalle JSON de una suite. |
| `GET` | `/api/catalogo/disponibilidad` | Público; disponibilidad real por fecha, hora y modalidad, con límite de consultas por IP. |
| `GET` | `/cliente/reservas/nueva?tipoId={id}` | Requiere rol `CLIENTE`; permite elegir habitación física, fecha, hora y duración. |
| `GET` | `/cliente/reservas/disponibilidad` | Requiere rol `CLIENTE`; devuelve habitaciones libres para el intervalo solicitado. |
| `POST` | `/cliente/reservas` | Requiere rol `CLIENTE`; retiene la habitación por cinco minutos y crea una reserva pendiente de pago. |
| `GET/POST` | `/cliente/reservas/{id}/pago` y `/pagar` | Requiere rol `CLIENTE`; muestra la cuenta regresiva y simula pago solo en demo. |
| `GET` | `/cliente/reservas/{id}/qr` | Requiere rol `CLIENTE`; entrega el QR de una reserva propia pagada. |
| `GET` | `/cliente/reservas` y `/cliente/reservas/{id}` | Requiere rol `CLIENTE`; lista y muestra únicamente las reservas del cliente autenticado. |
| `GET/POST` | `/cliente/reservas/{id}/extensiones/**` | Requiere rol `CLIENTE`; en demo permite solicitar y pagar una ampliación tras check-in. |
| `POST` | `/cliente/reservas/{id}/cancelar` | Requiere rol `CLIENTE`; permite cancelar una reserva pendiente propia. |
| `GET` | `/recepcion/reservas` | Requiere rol `RECEPCION` o `ADMIN`; lista pagos pendientes y reservas que esperan check-in. |
| `POST` | `/recepcion/reservas/check-in` | Requiere rol `RECEPCION` o `ADMIN`; valida el token QR y registra el ingreso. |
| `POST` | `/recepcion/reservas/{id}/confirmar` y `/cancelar` | Requiere rol `RECEPCION` o `ADMIN`; valida manualmente los pagos del perfil normal o cancela la solicitud. |
| `GET` | `/api/auth/csrf` | Público; emite el token CSRF sin caché. |
| `GET` | `/api/auth/client/disponibilidad` | Público; verifica correo/documento/teléfono y limita consultas por IP. |
| `POST` | `/api/auth/login` | Login de empleado; CSRF requerido. |
| `POST` | `/api/auth/client/register` | Registro e inicio de sesión de cliente; CSRF requerido. |
| `POST` | `/api/auth/client/login` | Login de cliente; CSRF requerido. |
| `GET` | `/api/auth/me` | Requiere sesión de empleado. |
| `GET` | `/api/auth/client/me` | Requiere sesión de cliente. |
| `POST` | `/api/auth/logout` y `/logout` | Filtros de logout de Spring Security; CSRF requerido. |
| `GET` | `/asistente` | Público; página con catálogo de suites a la izquierda y chat a la derecha. |
| `GET` | `/api/chatbot/catalogo` | Público; suites, tarifas y disponibilidad desde la base. |
| `GET` | `/api/chatbot/estado` | Público; estado de cola, modo, uso y bandera de derivación. |
| `GET` | `/api/chatbot/historico` | Público; mensajes de la conversación actual. |
| `POST` | `/api/chatbot/mensaje` | Público o cliente; CSRF requerido; limitado por IP y por conversación. |
| `POST` | `/api/chatbot/reiniciar` | Público o cliente; CSRF requerido; limpia el estado de cola. |

En `/api/**`, los errores de autenticación/autorización tienen respuestas JSON. El chatbot no exige sesión: el anónimo se identifica con la cookie `ECLIPSE_CHAT_SID` y el cliente con su sesión HTTP. Los límites y la clasificación de intenciones están en `ChatbotServiceImpl`, `ChatbotRateLimiter` e `IntencionChatbotDetector`. Los patrones web `/cliente/**`, `/admin/**`, `/reportes/**`, `/recepcion/**` y `/roomservice/**` tienen reglas de rol en Spring Security; que exista una regla no implica que haya una ruta o módulo implementado detrás.

`SecurityConfig` usa BCrypt con costo 10 y `HttpSessionSecurityContextRepository`. Las sesiones tienen 30 minutos de inactividad; la cookie es `HttpOnly` y `SameSite=Lax`. `Secure` es falso en desarrollo y se activa con el perfil `prod`. El ID de sesión cambia tras login/registro.

### Catálogo público

`CatalogoController` sirve dos vistas Thymeleaf (`catalogo/index` y `catalogo/detalle`) y tres rutas JSON. Las rutas están permitidas en ambas cadenas de `SecurityConfig`; reservar sigue exigiendo `ROLE_CLIENTE`, por lo que el botón de reserva redirige al login cuando no hay sesión.

`CatalogoServiceImpl` reutiliza `IReservaService` para no duplicar las reglas de reserva:

- `listarTiposHabitacion()`, `listarTarifas()` y `listarModalidades()` alimentan las tarjetas.
- Sin fecha/hora, la disponibilidad de la tarjeta cuenta habitaciones con `estado_rack` `LIBRE` o `DISPONIBLE`.
- Con fecha/hora y modalidad, delega en `listarHabitacionesDisponibles(...)`, que descarta cruces con reservas y extensiones activas.
- El precio "desde" es el menor de las tarifas vigentes; si no hay vigentes, usa todas las tarifas configuradas.
- Las amenidades se guardan en `tipo_habitacion.amenidades` como texto separado por `|`. La columna es nullable para que `ddl-auto=update` no falle sobre bases existentes.

`GET /api/catalogo/disponibilidad` es la versión pública de la consulta de disponibilidad y usa `DisponibilidadRateLimiter` (30 solicitudes por minuto e IP) para evitar abuso. La API no requiere CSRF porque solo expone lecturas GET.

En el perfil `demo`, `DemoCatalogInitializer` carga siete suites con las tarifas oficiales, amenidades y 16 habitaciones. Los datos se actualizan de forma idempotente al arrancar.

### Registro de cliente

El wizard actual tiene dos pasos:

1. Datos personales: nombre, apellido, documento, país emisor, edad, teléfono y aceptación de privacidad.
2. Correo electrónico, confirmación de correo, contraseña y confirmación de contraseña.

El navegador valida en cada campo, consulta disponibilidad con debounce aproximado de 500 ms, habilita el botón de avance/creación según la validez y enfoca el primer error al enviar. La confirmación de correo y contraseña se compara en el navegador y no se manda en el JSON.

En el backend:

- El correo se normaliza a minúsculas, se limita a dominios configurados, se contrasta con una lista de dominios desechables y, durante el registro, se verifica que el dominio publique MX.
- DNI requiere 8 dígitos; CE/pasaporte aceptan 6–20 caracteres alfanuméricos.
- El teléfono se normaliza y valida como E.164.
- Correo, documento y teléfono tienen comprobación de unicidad y restricciones únicas declaradas en `Cliente`.
- La edad y aceptación de privacidad se validan; la fecha de aceptación se persiste.
- La contraseña de registro acepta 8–20 caracteres, rechaza la lista local de claves comunes y fragmentos de datos personales, y se almacena mediante BCrypt. La interfaz muestra un medidor de fortaleza, no una lista de recomendaciones.
- No hay envío de OTP ni verificación de titularidad de correo/teléfono implementados en el flujo actual.

Las consultas de disponibilidad usan GET, con el valor en la URL. El limitador es local a cada proceso: permite 30 consultas por minuto e IP; no es un control distribuido.

### Login de empleado y cliente

- Empleados: autenticación por `username`; el servicio rechaza cuentas inactivas y deriva roles/permisos desde `Empleado` y `RolEmpleado`.
- Clientes: autenticación por correo, con mensaje genérico `Credenciales inválidas`; no se exige verificación de correo.
- Cinco fallos para la misma IP e identidad activan un bloqueo de 3 minutos. El mapa de intentos y el rate limit viven en memoria y se limpian al expirar/reiniciar; no sustituyen un control distribuido.
- El registro de fallos escribe una referencia hash parcial y la cantidad, no el usuario, IP ni contraseña en texto plano.

### Asistente: modos, límites y derivación

- **Modo público**: visitante sin sesión. 10 mensajes por minuto por IP y 12 por conversación; solo responde intenciones del catálogo (saludo, tarifa, disponibilidad, servicios, política) y ofrece derivar a recepción. Una consulta abierta devuelve una respuesta predefinida que invita a crear una cuenta.
- **Modo cliente**: sesión `ROLE_CLIENTE`. 30 mensajes por minuto y sin tope por conversación; puede hacer preguntas libres contra Gemini.
- **Cliente con respaldo**: si Gemini falla o no hay `GEMINI_API_KEY`, el cliente recibe la respuesta predefinida correspondiente a su intención y la conversación continúa.
- **Derivación a recepción**: `ColaDerivacionStore` marca la conversación, estima 2 minutos de espera y expira a los 30. Solo cambia `conversacion_chatbot.derivo_a_reserva` y el estado de cola; **no** existe dashboard de recepción conectado, así que no se debe anunciar como flujo de atención terminado.
- Los contadores de límite y la cola viven en memoria de cada instancia; no son controles distribuidos.

## Persistencia y modelo de datos

La configuración normal usa PostgreSQL (`siweb_eclipse`) y `spring.jpa.hibernate.ddl-auto=update`. El perfil `demo` usa H2 en memoria y `create-drop`; las pruebas también usan H2 y no necesitan acceso a la base PostgreSQL del desarrollador.

Clases JPA con `@Entity` y sus tablas actuales:

| Entidad | Tabla | Área |
| --- | --- | --- |
| `Cliente` | `cliente` | Cuenta, documento, contacto y aceptación de privacidad. |
| `Empleado` | `empleado` | Cuenta de empleado, estado y rol. |
| `RolEmpleado` | `rol_empleado` | Rol y permisos del empleado. |
| `Habitacion` | `habitacion` | Habitación y estado de rack. |
| `TipoHabitacion` | `tipo_habitacion` | Clasificación de habitación. |
| `ModalidadTurno` | `modalidad_turno` | Modalidad/duración de estadía. |
| `TarifaHabitacion` | `tarifa_habitacion` | Precio por tipo y modalidad; usa `TarifaHabitacionPK`. |
| `ComprobantePago` | `comprobante_pago` | Comprobante y datos de cobro asociados por identificador. |
| `TransaccionPago` | `transaccion_pago` | Transacciones vinculadas al comprobante. |
| `ConversacionChatbot` | `conversacion_chatbot` | Conversación de chatbot, cliente opcional y marca de derivación. |
| `MensajeChatbot` | `mensaje_chatbot` | Mensajes de la conversación, con rol e intención. |

`ConversacionChatbot` y `MensajeChatbot` son las únicas entidades del chatbot con lectura/escritura activas: las usa `ChatbotServiceImpl` junto con `Habitacion`, `TipoHabitacion` y `TarifaHabitacion` para componer el catálogo.

Relaciones JPA explícitas verificadas en el código: `Empleado` → `RolEmpleado`, `Habitacion` → `TipoHabitacion`, `TarifaHabitacion` → `TipoHabitacion` y `ModalidadTurno`, y `TransaccionPago` → `ComprobantePago`. Los datos de cliente imponen unicidad en correo, número de documento y teléfono.

`ReservaEstadia` conserva suite, habitación física, modalidad, intervalo, método de pago, estado, vencimiento de la retención, token QR y fechas de pago/check-in. Los estados incluyen `PENDIENTE_PAGO`, `CONFIRMADA`, `CHECKED_IN`, `CANCELADA` y `EXPIRADA`. La disponibilidad usa intervalos semiabiertos y excluye las reservas activas que se crucen con la habitación elegida; los holds pendientes solo bloquean mientras su vencimiento siga vigente. Un proceso periódico libera holds expirados. En demo los pagos se simulan; en el perfil normal se registra la preferencia para que recepción valide el pago por fuera del sitio. No se almacenan datos de tarjetas ni credenciales de pago.

En la carpeta `entity` hay otros nombres relacionados con pedidos, limpieza y averías, pero algunos archivos siguen vacíos y no son entidades persistidas en el código actual. No usar sus nombres como evidencia de tablas activas.

## Interfaz y movimiento visual

- `templates/inicio.html`: portada y enlaces de acceso.
- `templates/asistente.html`: página del asistente con catálogo de suites y chat en paralelo.
- `templates/auth/login.html` y `registro.html`: formularios Thymeleaf.
- `static/css/home.css`: tema oscuro, paneles, responsive layout, fondo `/img/hotel-eclipse-background.png`, estados de validación y transiciones.
- `static/css/asistente.css` y `static/js/asistente.js`: layout partido, tarjetas del catálogo y flujo de chat de `/asistente`.
- `static/css/chatbot-widget.css` y `static/js/chatbot-widget.js`: cajón flotante inyectado en la portada y en `/inicio`.
- `static/js/auth-login.js`: CSRF, login/registro, validación del wizard, disponibilidad, estados de carga y controles de mostrar contraseña.
- El panel y la marca aparecen con movimientos cortos; los pasos del wizard entran suavemente, y campos, botones y barra de progreso tienen transiciones.
- `prefers-reduced-motion: reduce` reduce a prácticamente cero las animaciones/transiciones y desactiva el desplazamiento suave.
- El botón de visibilidad de contraseña es compacto, con icono de ojo.

## Configuración y secretos

| Archivo/variable | Función |
| --- | --- |
| `application.properties` | PostgreSQL, Hibernate, sesión y listas permitidas/desechables de dominios. |
| `application-demo.properties` | H2 en memoria. |
| `application-prod.properties` | Activa `Secure` para la cookie de sesión. |
| `DB_PASSWORD` | Contraseña de PostgreSQL; no hay fallback de contraseña en el repositorio. |
| `APP_AUTH_EMAIL_DOMINIOS_PERMITIDOS` | Dominios separados por coma; predeterminados: Gmail, Hotmail, Outlook y Yahoo. |
| `APP_AUTH_EMAIL_DOMINIOS_DESECHABLES` | Dominios desechables conocidos, separados por coma. |
| `APP_SEED_ADMIN_PASSWORD`, `APP_SEED_RECEPCION_PASSWORD`, `APP_SEED_ROOMSERVICE_PASSWORD` | Contraseñas semilla; el inicializador solo actúa con perfiles `demo`/`dev`. |
| `GEMINI_API_KEY` | Clave de la API de Gemini. Vive en `.env` (raíz, ignorado por git) o en una variable de entorno; sin ella el asistente responde con respuestas predefinidas. |
| `app.chat.*` en `application.properties` | `base-url`, `api-key`, `modelo`, `timeout-ms`, `max-historial`, `max-entrada`, `limite-publico-min`, `limite-cliente-min`, `max-mensajes-anonimos`. |

No añadir secretos a archivos versionados. `application.properties` importa el `.env` de la raíz con `spring.config.import=optional:file:./.env[.properties]`, y `.env` está en `.gitignore`; una variable de entorno con el mismo nombre pisa al archivo. El secreto no entra al jar porque la importación usa `file:` y no `classpath:`. El perfil `prod` debe utilizar HTTPS para que la cookie `Secure` pueda viajar.

## Módulos aún no conectados

Las carpetas `templates/roomservice`, `templates/operaciones` y `templates/reportes` contienen diseños de páginas, pero no se encontraron rutas MVC activas para esos procesos. Al inspeccionar el workspace, los siguientes archivos estaban vacíos (0 bytes):

- Controladores: `CheckoutController`, `OperacionesController`, `RackController`, `ReporteExcelController` y `RoomServiceController`.
- Implementaciones de servicio: `FacturacionServiceImpl`, `LimpiezaServiceImpl` y `RoomServiceServiceImpl`.

Las interfaces/repositories relacionados que sí tienen contenido no reemplazan esos flujos faltantes. Antes de anunciar reservas, pagos, recepción, limpieza o pedidos como disponibles, se requiere implementar y conectar sus rutas, lógica y persistencia.

## Validación técnica del estado documentado

- Suite Maven más reciente presente en los reportes del workspace: 126 pruebas, 0 fallos, 0 errores.
- Las pruebas de integración usan H2 en memoria.
- El objetivo configurado en `pom.xml` es Java 21; no se ha cambiado aquí a Java 25.
- Comando de pruebas en Windows: `.\mvnw.cmd test`.

