# Siweb Eclipse

Sistema de gestión para reservas hoteleras, recepción, room service y reportes.

## Tecnologías

- Java 21
- Spring Boot 4
- Spring Data JPA
- Spring Security
- Thymeleaf
- PostgreSQL
- Maven

## Requisitos

- JDK 21+
- Maven o Maven Wrapper
- PostgreSQL
- Git

## Configuración inicial

1. Clona el proyecto:
   ```bash
   git clone <url-del-repositorio>
   cd siweb-eclipse
   ```

2. Configura la base de datos en `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/siweb_eclipse
   spring.datasource.username=postgres
   spring.datasource.password=tu_password
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   ```

3. Asegúrate de tener creada la base de datos `siweb_eclipse`.

## Ejecutar la aplicación

En Windows:
```bash
mvnw.cmd spring-boot:run
```

En Linux/macOS:
```bash
./mvnw spring-boot:run
```

La app estará disponible normalmente en:
```text
http://localhost:8080
```

## Probar

```bash
./mvnw test
```

O en Windows:
```bash
mvnw.cmd test
```

## Estructura principal

```text
src/
  main/
    java/
      pe/com/eclipsesuites/
        config/
        controller/
        dao/
        dto/
        entity/
        service/
        util/
    resources/
      static/
      templates/
  test/
    java/
```

## Git y ramas

Crear una rama:
```bash
git checkout -b cambios-Kenyer0w0
```

Guardar cambios:
```bash
git add .
git commit -m "Add payment entities and repositories"
```

Subir rama:
```bash
git push --set-upstream origin cambios-Kenyer0w0
```

> Si el push falla por permisos, revisa que el repositorio remoto sea el correcto y que hayas aceptado la invitación de colaboración.

## Licencia

Proyecto académico / interno del equipo. Ajustar según corresponda.
