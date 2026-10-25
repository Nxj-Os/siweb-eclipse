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
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ClienteAuthServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CorreoDomainPolicy correoDomainPolicy;

    @Mock
    private PasswordPolicy passwordPolicy;

    private PasswordEncoder passwordEncoder;
    private ClienteAuthServiceImpl servicio;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        servicio = new ClienteAuthServiceImpl(
                clienteRepository, passwordEncoder, correoDomainPolicy, passwordPolicy);
        lenient().when(correoDomainPolicy.validar("ana@gmail.com", true)).thenReturn("ana@gmail.com");
    }

    @Test
    void registrar_guardaCorreoNormalizadoYPasswordCifrada() {
        when(clienteRepository.existsByCorreoElectronicoIgnoreCase("ana@gmail.com"))
                .thenReturn(false);
        when(clienteRepository.existsByNumDocumento("12345678")).thenReturn(false);
        when(clienteRepository.existsByTelefono("+51999888777")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        Cliente cliente = servicio.registrar(registroValido());

        assertEquals("ana@gmail.com", cliente.getCorreoElectronico());
        assertEquals("PER", cliente.getPaisEmisionDoc());
        assertEquals("+51999888777", cliente.getTelefono());
        assertNotEquals("Larga!Clave2026OK", cliente.getPasswordHash());
        assertTrue(passwordEncoder.matches("Larga!Clave2026OK", cliente.getPasswordHash()));
        verify(clienteRepository).save(cliente);
    }

    @Test
    void registrar_correoExistenteRechazaDuplicado() {
        when(clienteRepository.existsByCorreoElectronicoIgnoreCase("ana@gmail.com"))
                .thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(registroValido()));

        assertEquals("No se pudo completar el registro; verifica la información ingresada",
                error.getMessage());
    }

    @Test
    void registrar_dniNoNumericoRechazaRegistro() {
        ClienteRegistroRequestDTO request = registroValido();
        request.setNumDocumento("AB123456");

        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(request));
    }

    @Test
    void registrar_telefonoNoE164RechazaRegistro() {
        ClienteRegistroRequestDTO request = registroValido();
        request.setTelefono("999888777");

        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(request));
    }

    @Test
    void estaDisponible_documentoDuplicadoDevuelveNoDisponible() {
        when(clienteRepository.existsByNumDocumento("12345678")).thenReturn(true);

        assertEquals(false, servicio.estaDisponible("documento", "12345678", "DNI"));
    }

    @Test
    void estaDisponible_correoRechazaDominioNoPermitido() {
        when(correoDomainPolicy.validar("ana@dominioinventado.net", false))
                .thenThrow(new IllegalArgumentException("Dominio no permitido"));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.estaDisponible("correo", "ana@dominioinventado.net", null));
    }

    @Test
    void autenticar_correoIgnoraMayusculasYVerificaPassword() {
        Cliente cliente = clienteRegistrado();
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("ana@gmail.com"))
                .thenReturn(Optional.of(cliente));

        Cliente resultado = servicio.autenticar(" ANA@Gmail.com ", "Larga!Clave2026OK");

        assertEquals(cliente, resultado);
    }

    @Test
    void autenticar_passwordIncorrectaRechazaAcceso() {
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("ana@gmail.com"))
                .thenReturn(Optional.of(clienteRegistrado()));

        assertThrows(BadCredentialsException.class,
                () -> servicio.autenticar("ana@gmail.com", "incorrecta"));
    }

    private ClienteRegistroRequestDTO registroValido() {
        ClienteRegistroRequestDTO request = new ClienteRegistroRequestDTO();
        request.setTipoDocumento(" DNI ");
        request.setNumDocumento(" 12345678 ");
        request.setPaisEmisionDoc(" per ");
        request.setCorreoElectronico(" Ana@Gmail.com ");
        request.setNombre("Ana");
        request.setApellido("Rojas");
        request.setTelefono("+51999888777");
        request.setEdad(28);
        request.setPassword("Larga!Clave2026OK");
        request.setAceptaPoliticaPrivacidad(true);
        return request;
    }

    private Cliente clienteRegistrado() {
        return Cliente.builder()
                .id(1)
                .correoElectronico("ana@gmail.com")
                .passwordHash(passwordEncoder.encode("Larga!Clave2026OK"))
                .nombre("Ana")
                .apellido("Rojas")
                .build();
    }
}