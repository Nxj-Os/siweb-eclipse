package pe.com.eclipsesuites.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.env.Environment;
import pe.com.eclipsesuites.dao.EmpleadoRepository;
import pe.com.eclipsesuites.dao.RolEmpleadoRepository;
import pe.com.eclipsesuites.entity.Empleado;
import pe.com.eclipsesuites.entity.RolEmpleado;

@Slf4j
@Configuration
@Profile({"demo", "dev"})
@RequiredArgsConstructor
public class DataInitializer {

    private final RolEmpleadoRepository rolEmpleadoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Bean
    public CommandLineRunner inicializarDatos() {
        return args -> {
            RolEmpleado admin = obtenerOCrearRol("ADMIN", "REPORTES,EMPLEADOS,CONFIGURACION,RACK");
            RolEmpleado recepcion = obtenerOCrearRol("RECEPCION", "RACK,CHECKOUT,RESERVAS,LIQUIDACION");
            RolEmpleado roomservice = obtenerOCrearRol("ROOMSERVICE", "PEDIDOS,COMANDA,EXTENSION");

            String passwordAdmin = environment.getProperty("APP_SEED_ADMIN_PASSWORD");
            String passwordRecepcion = environment.getProperty("APP_SEED_RECEPCION_PASSWORD");
            String passwordRoomservice = environment.getProperty("APP_SEED_ROOMSERVICE_PASSWORD");
            if (org.apache.commons.lang3.StringUtils.isAnyBlank(
                    passwordAdmin, passwordRecepcion, passwordRoomservice)) {
                log.warn("Se omitió la creación de cuentas semilla; configura las contraseñas por entorno.");
                return;
            }
            crearEmpleadoSiNoExiste("admin", passwordAdmin, "Nilton", "Quispe",
                    30, "DIURNO", admin);
            crearEmpleadoSiNoExiste("recepcion1", passwordRecepcion, "Rosa", "Flores",
                    27, "MANANA", recepcion);
            crearEmpleadoSiNoExiste("roomservice1", passwordRoomservice, "Luis", "Torres",
                    24, "TARDE", roomservice);
        };
    }

    private RolEmpleado obtenerOCrearRol(String nombreRol, String permisos) {
        return rolEmpleadoRepository.findByNombreRol(nombreRol)
                .orElseGet(() -> {
                    RolEmpleado rol = rolEmpleadoRepository.save(RolEmpleado.builder()
                            .nombreRol(nombreRol)
                            .permisosSistema(permisos)
                            .build());
                    log.info("Rol creado: {}", nombreRol);
                    return rol;
                });
    }

    private void crearEmpleadoSiNoExiste(String username, String passwordPlano, String nombre,
                                         String apellido, Integer edad, String turno,
                                         RolEmpleado rol) {
        if (empleadoRepository.existsByUsernameLogin(username)) {
            return;
        }
        Empleado empleado = Empleado.builder()
                .rol(rol)
                .nombre(nombre)
                .apellido(apellido)
                .edad(edad)
                .usernameLogin(username)
                .passwordHashBcrypt(passwordEncoder.encode(passwordPlano))
                .turnoLaboral(turno)
                .estadoActivo(true)
                .build();
        empleadoRepository.save(empleado);
        log.info("Se creó una cuenta semilla para el rol {}", rol.getNombreRol());
    }
}
