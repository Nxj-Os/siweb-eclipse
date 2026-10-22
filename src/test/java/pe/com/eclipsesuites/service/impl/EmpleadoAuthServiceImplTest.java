package pe.com.eclipsesuites.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.com.eclipsesuites.dao.EmpleadoRepository;
import pe.com.eclipsesuites.entity.Empleado;
import pe.com.eclipsesuites.entity.RolEmpleado;

import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpleadoAuthServiceImplTest {

    @Mock
    private EmpleadoRepository empleadoRepository;

    private PasswordEncoder passwordEncoder;
    private EmpleadoAuthServiceImpl servicio;

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "Admin@123";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(10);
        servicio = new EmpleadoAuthServiceImpl(empleadoRepository, passwordEncoder);
    }

    private RolEmpleado rol(String nombre, String permisos) {
        return RolEmpleado.builder().id(1).nombreRol(nombre).permisosSistema(permisos).build();
    }

    private Empleado empleado(RolEmpleado rol, boolean activo, String hashPassword) {
        return Empleado.builder()
                .id(1)
                .rol(rol)
                .nombre("Nilton")
                .apellido("Quispe")
                .usernameLogin(USERNAME)
                .passwordHashBcrypt(hashPassword)
                .estadoActivo(activo)
                .build();
    }

    @Test
    void loadUserByUsername_usuarioValido_retornaUserConAuthoritiesDeRolYPermisos() {
        String hash = passwordEncoder.encode(PASSWORD);
        Empleado empleado = empleado(rol("ADMIN", "REPORTES,RACK"), true, hash);
        when(empleadoRepository.findByUsernameLogin(USERNAME)).thenReturn(Optional.of(empleado));

        UserDetails user = servicio.loadUserByUsername(USERNAME);

        assertEquals(USERNAME, user.getUsername());
        assertEquals(hash, user.getPassword());
        assertTrue(user.isEnabled());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isCredentialsNonExpired());

        var authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("PERMISO_REPORTES"));
        assertTrue(authorities.contains("PERMISO_RACK"));
        assertEquals(3, authorities.size());
    }

    @Test
    void loadUserByUsername_usuarioInactivo_retornaUserDeshabilitado() {
        String hash = passwordEncoder.encode(PASSWORD);
        Empleado empleado = empleado(rol("RECEPCION", null), false, hash);
        when(empleadoRepository.findByUsernameLogin(USERNAME)).thenReturn(Optional.of(empleado));

        UserDetails user = servicio.loadUserByUsername(USERNAME);

        assertFalse(user.isEnabled());
    }

    @Test
    void loadUserByUsername_sinPermisos_soloTieneAuthorityDeRol() {
        String hash = passwordEncoder.encode(PASSWORD);
        Empleado empleado = empleado(rol("ROOMSERVICE", ""), true, hash);
        when(empleadoRepository.findByUsernameLogin(USERNAME)).thenReturn(Optional.of(empleado));

        UserDetails user = servicio.loadUserByUsername(USERNAME);

        var authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains("ROLE_ROOMSERVICE"));
    }

    @Test
    void loadUserByUsername_usernameConEspacios_seRecortaAntesDeBuscar() {
        String hash = passwordEncoder.encode(PASSWORD);
        Empleado empleado = empleado(rol("ADMIN", "REPORTES"), true, hash);
        when(empleadoRepository.findByUsernameLogin(USERNAME)).thenReturn(Optional.of(empleado));

        UserDetails user = servicio.loadUserByUsername("  admin  ");

        assertEquals(USERNAME, user.getUsername());
    }

    @Test
    void loadUserByUsername_usuarioInexistente_lanzaUsernameNotFoundException() {
        when(empleadoRepository.findByUsernameLogin("fantasma")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> servicio.loadUserByUsername("fantasma"));
    }

    @Test
    void loadUserByUsername_usernameEnBlanco_lanzaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> servicio.loadUserByUsername("   "));
        assertThrows(IllegalArgumentException.class, () -> servicio.loadUserByUsername(null));
    }

    @Test
    void autenticar_credencialesValidas_retornaEmpleado() {
        String hash = passwordEncoder.encode(PASSWORD);
        Empleado empleado = empleado(rol("ADMIN", "REPORTES"), true, hash);
        when(empleadoRepository.findByUsernameLogin(USERNAME)).thenReturn(Optional.of(empleado));

        Empleado resultado = servicio.autenticar(USERNAME, PASSWORD);

        assertEquals(USERNAME, resultado.getUsernameLogin());
        assertEquals("Nilton", resultado.getNombre());
    }

    @Test
    void autenticar_passwordIncorrecta_lanzaBadCredentialsException() {
        String hash = passwordEncoder.encode(PASSWORD);
        Empleado empleado = empleado(rol("ADMIN", "REPORTES"), true, hash);
        when(empleadoRepository.findByUsernameLogin(USERNAME)).thenReturn(Optional.of(empleado));

        assertThrows(BadCredentialsException.class, () -> servicio.autenticar(USERNAME, "OtraClave123"));
    }

    @Test
    void autenticar_usuarioInactivo_lanzaDisabledException() {
        String hash = passwordEncoder.encode(PASSWORD);
        Empleado empleado = empleado(rol("ADMIN", "REPORTES"), false, hash);
        when(empleadoRepository.findByUsernameLogin(USERNAME)).thenReturn(Optional.of(empleado));

        assertThrows(DisabledException.class, () -> servicio.autenticar(USERNAME, PASSWORD));
    }

    @Test
    void autenticar_usuarioInexistente_lanzaUsernameNotFoundException() {
        when(empleadoRepository.findByUsernameLogin("fantasma")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> servicio.autenticar("fantasma", PASSWORD));
    }

    @Test
    void autenticar_camposEnBlanco_lanzaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> servicio.autenticar("", PASSWORD));
        assertThrows(IllegalArgumentException.class, () -> servicio.autenticar(USERNAME, "  "));
        assertThrows(IllegalArgumentException.class, () -> servicio.autenticar(null, null));
    }
}
