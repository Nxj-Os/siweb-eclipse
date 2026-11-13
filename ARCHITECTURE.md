# Arquitectura actual de Siweb Eclipse

Este documento describe el código que existe hoy en el workspace. Es una fotografía del estado actual, no un diseño futuro ni una afirmación de que todas las pantallas del hotel estén conectadas.

## Resumen

Siweb Eclipse es una aplicación web monolítica construida con Spring Boot. El backend Java sirve páginas Thymeleaf y recursos estáticos; el navegador usa `auth-login.js` para autenticar y registrar clientes mediante una API JSON. Spring Security administra autenticación basada en sesión y Hibernate/JPA persiste el modelo disponible en PostgreSQL.

El flujo de acceso, registro de clientes, catálogo y reservas están implementados. En demo, el cliente elige una suite y una habitación física, retiene el horario durante cinco minutos, simula el pago y recibe un QR; recepción registra el check-in y el cliente puede ampliar su estadía. En el perfil normal el pago sigue siendo externo y se valida manualmente. Room service, operaciones y reportes aún tienen artefactos parciales.

## Vista de contexto y componentes

```mermaid
flowchart LR
    Usuario --> Browser[Navegador]
    Browser -->|GET páginas| Thymeleaf[Spring MVC y Thymeleaf]
    Browser -->|fetch JSON + X-XSRF-TOKEN| API[/api/auth]
    Browser -->|CSS, JS e imágenes| Static[Recursos estáticos]
    Thymeleaf --> Security[Spring Security]
    API --> Security
    Security --> Session[Sesión HTTP]
    API --> Controller[AuthController]
    Controller --> AuthService[Servicios de autenticación]
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

## Capas y paquetes

Raíz Java: `src/main/java/pe/com/eclipsesuites`.

| Paquete | Archivos Java encontrados | Responsabilidad/estado |
| --- | ---: | --- |
| raíz | 1 | Punto de entrada `SiwebEclipseApplication`. |
| `config` | 3 | Cadenas de seguridad, codificador BCrypt, configuración de caché y carga de datos de desarrollo. |
| `controller` | 11 | Rutas API/MVC y manejo de errores. Incluye autenticación, páginas, reservas de clientes y validación de reservas por recepción. |
| `dao` | 17 | Interfaces Spring Data JPA para los datos del hotel. |
| `dto/request` | 7 | Contratos de solicitudes, incluidos login y registro. |
| `dto/response` | 5 | Respuestas JSON, errores y datos de disponibilidad. |
| `entity` | 23 | Modelos Java. Hay 12 clases `@Entity`, un tipo `@Embeddable` y estados de reserva/pago. |
| `service` | 8 | Interfaces de autenticación, reserva y política de consulta MX. |
| `service/impl` | 13 | Reglas de autenticación, reservas, correo, contraseña y límites; algunos archivos de otros dominios están vacíos. |
| `util` | 2 | Generador de códigos y constantes SUNAT. |

Las cantidades se obtuvieron contando los archivos `.java` presentes, incluidos los archivos vacíos. No representan funcionalidades terminadas.

### Puntos de entrada actuales

- `SiwebEclipseApplication.main`: inicializa Spring Boot.
- `HomeController`: sirve `/inicio` con los tipos de suite y tarifas actuales; exige una sesión autenticada y, de lo contrario, redirige a `/login`.
- `AuthPageController`: sirve los formularios Thymeleaf de login y registro.
- `AuthController`: API de autenticación bajo `/api/auth`.
- `SecurityConfig`: configura una cadena para `/api/**` y otra para las páginas web.
- `src/main/resources/static/index.html`: portada estática servida por Spring Boot.

## Rutas y seguridad

| Método | Ruta | Acceso/resultado |
| --- | --- | --- |
| `GET` | `/` y `/index.html` | Portada estática pública. |
| `GET` | `/login?tipo=empleado` o `?tipo=cliente` | Formulario público; el parámetro selecciona la identidad a pedir. |
| `GET` | `/registro` | Formulario público; Thymeleaf incluye los dominios permitidos configurados. |
| `GET` | `/inicio` | Requiere autenticación; no diferencia la vista por rol. |
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

En `/api/**`, los errores de autenticación/autorización tienen respuestas JSON. Los patrones web `/cliente/**`, `/admin/**`, `/reportes/**`, `/recepcion/**` y `/roomservice/**` tienen reglas de rol en Spring Security; que exista una regla no implica que haya una ruta o módulo implementado detrás.

`SecurityConfig` usa BCrypt con costo 10 y `HttpSessionSecurityContextRepository`. Las sesiones tienen 30 minutos de inactividad; la cookie es `HttpOnly` y `SameSite=Lax`. `Secure` es falso en desarrollo y se activa con el perfil `prod`. El ID de sesión cambia tras login/registro.

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
| `ReservaEstadia` | `reserva_estadia` | Suite/turno, fechas, cantidad de personas, tarifa, método de pago y estado de la reserva. |
| `ExtensionEstadia` | `extension_estadia` | Hold de cinco minutos y pago de una ampliación de estadía en demo. |
| `ConversacionChatbot` | `conversacion_chatbot` | Conversación de chatbot y sesión web. |
| `MensajeChatbot` | `mensaje_chatbot` | Mensajes de conversación. |

Relaciones JPA explícitas verificadas en el código: `Empleado` → `RolEmpleado`, `Habitacion` → `TipoHabitacion`, `TarifaHabitacion` → `TipoHabitacion` y `ModalidadTurno`, y `TransaccionPago` → `ComprobantePago`. Los datos de cliente imponen unicidad en correo, número de documento y teléfono.

`ReservaEstadia` conserva suite, habitación física, modalidad, intervalo, método de pago, estado, vencimiento de la retención, token QR y fechas de pago/check-in. Los estados incluyen `PENDIENTE_PAGO`, `CONFIRMADA`, `CHECKED_IN`, `CANCELADA` y `EXPIRADA`. La disponibilidad usa intervalos semiabiertos y excluye las reservas activas que se crucen con la habitación elegida; los holds pendientes solo bloquean mientras su vencimiento siga vigente. Un proceso periódico libera holds expirados. En demo los pagos se simulan; en el perfil normal se registra la preferencia para que recepción valide el pago por fuera del sitio. No se almacenan datos de tarjetas ni credenciales de pago.

En la carpeta `entity` hay otros nombres relacionados con pedidos, limpieza y averías, pero algunos archivos siguen vacíos y no son entidades persistidas en el código actual. No usar sus nombres como evidencia de tablas activas.

## Interfaz y movimiento visual

- `templates/inicio.html`: catálogo de tipos de suite y precios mínimos vigentes, cargados desde la base de datos.
- `templates/cliente/reserva-paso1.html`: selección de habitaciones físicas, disponibilidad, fecha/hora, duración y vista previa lateral de la suite.
- `templates/cliente/reserva-pago.html`: selección de método de pago y cuenta regresiva real de cinco minutos; no captura información bancaria.
- `templates/cliente/confirmacion-reserva.html`: estado de la reserva, QR para check-in y solicitud de ampliación tras validar el ingreso.
- `templates/recepcion/reservas-pendientes.html`: validación manual de pagos en modo normal y escaneo del QR de check-in, con entrada manual de respaldo.
- Las imágenes del catálogo y la vista previa se toman de `TipoHabitacion.galeriaFotos`; se muestra un recurso local de reemplazo si falta la imagen. El total final usa la tarifa vigente de la suite, modalidad y fecha elegidas.
- Los precios “Desde” del prototipo se muestran como referencia si no hay una tarifa vigente; no se usan como total de la reserva ni reemplazan la configuración de tarifas del hotel.
- El perfil Spring `demo` carga siete tipos de suite, tres modalidades (4, 6 y 12 horas), 16 habitaciones y tarifas de muestra en H2. La interfaz avisa que son datos simulados; este catálogo no se carga en producción.
- `templates/auth/login.html` y `registro.html`: formularios Thymeleaf.
- `static/css/home.css`: tema oscuro, paneles, responsive layout, fondo `/img/hotel-eclipse-background.png`, estados de validación y transiciones.
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

No añadir secretos a archivos versionados. El perfil `prod` debe utilizar HTTPS para que la cookie `Secure` pueda viajar.

## Módulos aún no conectados

Las carpetas `templates/roomservice`, `templates/operaciones` y `templates/reportes` contienen diseños de páginas, pero no se encontraron rutas MVC activas para esos procesos. Al inspeccionar el workspace, los siguientes archivos estaban vacíos (0 bytes):

- Controladores: `CheckoutController`, `OperacionesController`, `RackController`, `ReporteExcelController` y `RoomServiceController`.
- Implementaciones de servicio: `FacturacionServiceImpl`, `LimpiezaServiceImpl` y `RoomServiceServiceImpl`.

Las interfaces/repositories relacionados que sí tienen contenido no reemplazan esos flujos faltantes. Antes de anunciar reservas, pagos, recepción, limpieza o pedidos como disponibles, se requiere implementar y conectar sus rutas, lógica y persistencia.

## Validación técnica del estado documentado

- Suite Maven ejecutada para este flujo: 108 pruebas, 0 fallos, 0 errores.
- Las pruebas de integración usan H2 en memoria.
- El objetivo configurado en `pom.xml` es Java 21; no se ha cambiado aquí a Java 25.
- Comando de pruebas en Windows: `.\mvnw.cmd test`.

