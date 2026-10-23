package pe.com.eclipsesuites.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.com.eclipsesuites.dao.EmpleadoRepository;
import pe.com.eclipsesuites.dao.RolEmpleadoRepository;
import pe.com.eclipsesuites.entity.Empleado;
import pe.com.eclipsesuites.entity.RolEmpleado;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RolEmpleadoRepository rolEmpleadoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner inicializarDatos() {
        return args -> {
            RolEmpleado admin = obtenerOCrearRol("ADMIN", "REPORTES,EMPLEADOS,CONFIGURACION,RACK");
            RolEmpleado recepcion = obtenerOCrearRol("RECEPCION", "RACK,CHECKOUT,RESERVAS,LIQUIDACION");
            RolEmpleado roomservice = obtenerOCrearRol("ROOMSERVICE", "PEDIDOS,COMANDA,EXTENSION");

            crearEmpleadoSiNoExiste("admin", "Admin@123", "Nilton", "Quispe",
                    30, "DIURNO", admin);
            crearEmpleadoSiNoExiste("recepcion1", "Recep@123", "Rosa", "Flores",
                    27, "MANANA", recepcion);
            crearEmpleadoSiNoExiste("roomservice1", "Room@123", "Luis", "Torres",
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
        log.info("Empleado semilla creado: {} (rol {})", username, rol.getNombreRol());
    }
}
