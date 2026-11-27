package pe.com.eclipsesuites.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.com.eclipsesuites.dao.ClienteRepository;
import pe.com.eclipsesuites.dto.request.ClienteRegistroRequestDTO;
import pe.com.eclipsesuites.dto.request.PerfilClienteUpdateDTO;
import pe.com.eclipsesuites.entity.Cliente;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteAuthServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    private PasswordEncoder passwordEncoder;
    private ClienteAuthServiceImpl servicio;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        servicio = new ClienteAuthServiceImpl(clienteRepository, passwordEncoder);
    }

    @Test
    void registrar_guardaCorreoNormalizadoYPasswordCifrada() {
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.empty());
        when(clienteRepository.findByNumDocumento("12345678")).thenReturn(Optional.empty());
        when(clienteRepository.findByTelefono("999888777")).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        Cliente cliente = servicio.registrar(registroValido());

        assertEquals("ana@example.com", cliente.getCorreoElectronico());
        assertEquals("PER", cliente.getPaisEmisionDoc());
        assertTrue(cliente.estaActivo());
        assertNull(cliente.getFechaBaja());
        assertNotEquals("Cliente@123", cliente.getPasswordHash());
        assertTrue(passwordEncoder.matches("Cliente@123", cliente.getPasswordHash()));
        verify(clienteRepository).save(cliente);
    }

    @Test
    void registrar_correoExistenteRechazaDuplicado() {
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(Cliente.builder()
                        .id(5)
                        .correoElectronico("ana@example.com")
                        .activo(true)
                        .build()));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(registroValido()));

        assertEquals("Ya existe una cuenta con ese correo electrónico", error.getMessage());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void registrar_reactivaCuentaEliminadaConservandoId() {
        Cliente eliminada = Cliente.builder()
                .id(5)
                .correoElectronico("ana@example.com")
                .numDocumento("99999999")
                .telefono("111222333")
                .activo(false)
                .fechaBaja(Instant.now().minusSeconds(3 * 24 * 3600))
                .build();
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(eliminada));
        when(clienteRepository.findByNumDocumento("12345678")).thenReturn(Optional.empty());
        when(clienteRepository.findByTelefono("999888777")).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        Cliente cliente = servicio.registrar(registroValido());

        assertEquals(5, cliente.getId());
        assertTrue(cliente.estaActivo());
        assertNull(cliente.getFechaBaja());
    }

    @Test
    void autenticar_correoIgnoraMayusculasYVerificaPassword() {
        Cliente cliente = clienteRegistrado();
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(cliente));

        Cliente resultado = servicio.autenticar(" ANA@Example.com ", "Cliente@123");

        assertEquals(cliente, resultado);
    }

    @Test
    void autenticar_passwordIncorrectaRechazaAcceso() {
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(clienteRegistrado()));

        assertThrows(BadCredentialsException.class,
                () -> servicio.autenticar("ana@example.com", "incorrecta"));
    }

    @Test
    void autenticar_cuentaEliminadaRechazaAcceso() {
        Cliente eliminada = clienteRegistrado();
        eliminada.setActivo(false);
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(eliminada));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.autenticar("ana@example.com", "Cliente@123"));

        assertTrue(error.getMessage().contains("cuenta fue eliminada"));
    }

    @Test
    void actualizarPerfil_cambiaSoloLosCamposEditables() {
        Cliente cliente = clienteRegistrado();
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        Cliente resultado = servicio.actualizarPerfil("ana@example.com", datosEditables());

        assertEquals("Ana Maria", resultado.getNombre());
        assertEquals("Rojas Quispe", resultado.getApellido());
        assertEquals("+51999123456", resultado.getTelefono());
        assertEquals(31, resultado.getEdad());
        assertEquals("ana@example.com", resultado.getCorreoElectronico());
        verify(clienteRepository).save(cliente);
    }

    @Test
    void actualizarPerfil_telefonoDeOtraCuentaSeRechaza() {
        Cliente cliente = clienteRegistrado();
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(cliente));
        when(clienteRepository.findByTelefono("+51999123456"))
                .thenReturn(Optional.of(Cliente.builder().id(9).activo(true).build()));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarPerfil("ana@example.com", datosEditables()));

        assertEquals("Ya existe una cuenta con ese teléfono", error.getMessage());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void eliminarCuenta_desactivaYRegistraFechaDeBaja() {
        Cliente cliente = clienteRegistrado();
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        servicio.eliminarCuenta("ana@example.com");

        assertFalse(cliente.estaActivo());
        assertNotNull(cliente.getFechaBaja());
        verify(clienteRepository).save(cliente);
    }

    @Test
    void estaDisponible_cuentaEliminadaLiberaElCorreo() {
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(Cliente.builder().id(5).activo(false).build()));

        assertTrue(servicio.estaDisponible("correo", "ana@example.com", null));
    }

    @Test
    void estaDisponible_cuentaActivaSigueBloqueandoElCorreo() {
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(Cliente.builder().id(5).activo(true).build()));

        assertFalse(servicio.estaDisponible("correo", "ana@example.com", null));
    }

    private PerfilClienteUpdateDTO datosEditables() {
        PerfilClienteUpdateDTO datos = new PerfilClienteUpdateDTO();
        datos.setNombre("Ana Maria");
        datos.setApellido("Rojas Quispe");
        datos.setTelefono("+51999123456");
        datos.setEdad(31);
        return datos;
    }

    private ClienteRegistroRequestDTO registroValido() {
        ClienteRegistroRequestDTO request = new ClienteRegistroRequestDTO();
        request.setTipoDocumento(" DNI ");
        request.setNumDocumento(" 12345678 ");
        request.setPaisEmisionDoc(" per ");
        request.setCorreoElectronico(" Ana@Example.com ");
        request.setNombre("Ana");
        request.setApellido("Rojas");
        request.setTelefono("999888777");
        request.setPaisTelefono("PE");
        request.setEdad(28);
        request.setPassword("Cliente@123");
        request.setAceptaPoliticaPrivacidad(true);
        return request;
    }

    private Cliente clienteRegistrado() {
        return Cliente.builder()
                .id(1)
                .correoElectronico("ana@example.com")
                .passwordHash(passwordEncoder.encode("Cliente@123"))
                .nombre("Ana")
                .apellido("Rojas")
                .telefono("999888777")
                .activo(true)
                .build();
    }
}
