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
import pe.com.eclipsesuites.entity.Cliente;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
        when(clienteRepository.existsByCorreoElectronicoIgnoreCase("ana@example.com"))
                .thenReturn(false);
        when(clienteRepository.existsByNumDocumento("12345678")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        Cliente cliente = servicio.registrar(registroValido());

        assertEquals("ana@example.com", cliente.getCorreoElectronico());
        assertEquals("PER", cliente.getPaisEmisionDoc());
        assertNotEquals("Cliente@123", cliente.getPasswordHash());
        assertTrue(passwordEncoder.matches("Cliente@123", cliente.getPasswordHash()));
        verify(clienteRepository).save(cliente);
    }

    @Test
    void registrar_correoExistenteRechazaDuplicado() {
        when(clienteRepository.existsByCorreoElectronicoIgnoreCase("ana@example.com"))
                .thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(registroValido()));

        assertEquals("Ya existe una cuenta con ese correo electrónico", error.getMessage());
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

    private ClienteRegistroRequestDTO registroValido() {
        ClienteRegistroRequestDTO request = new ClienteRegistroRequestDTO();
        request.setTipoDocumento(" DNI ");
        request.setNumDocumento(" 12345678 ");
        request.setPaisEmisionDoc(" per ");
        request.setCorreoElectronico(" Ana@Example.com ");
        request.setNombre("Ana");
        request.setApellido("Rojas");
        request.setTelefono("999888777");
        request.setEdad(28);
        request.setPassword("Cliente@123");
        return request;
    }

    private Cliente clienteRegistrado() {
        return Cliente.builder()
                .id(1)
                .correoElectronico("ana@example.com")
                .passwordHash(passwordEncoder.encode("Cliente@123"))
                .nombre("Ana")
                .apellido("Rojas")
                .build();
    }
}