package pe.com.eclipsesuites.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import pe.com.eclipsesuites.dto.request.ClienteRegistroRequestDTO;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    private PasswordPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new PasswordPolicy(new ClassPathResource("security/claves-prohibidas.txt"));
        policy.cargarClavesProhibidas();
    }

    @Test
    void validarRegistro_aceptaContrasenaDentroDelRangoSinReglasDeComposicion() {
        ClienteRegistroRequestDTO request = registro("Una frase larga!!");

        assertDoesNotThrow(() -> policy.validarRegistro(request));
    }

    @Test
    void validarRegistro_aceptaVeinteCaracteres() {
        ClienteRegistroRequestDTO request = registro("Abcdefghijklmnopqr!?");

        assertDoesNotThrow(() -> policy.validarRegistro(request));
    }

    @Test
    void validarRegistro_aceptaOchoCaracteres() {
        ClienteRegistroRequestDTO request = registro("Abcdefg!");

        assertDoesNotThrow(() -> policy.validarRegistro(request));
    }

    @Test
    void validarRegistro_rechazaMenosDeOchoCaracteres() {
        ClienteRegistroRequestDTO request = registro("Abc123!");

        assertThrows(IllegalArgumentException.class, () -> policy.validarRegistro(request));
    }

    @Test
    void validarRegistro_rechazaMasDeVeinteCaracteres() {
        ClienteRegistroRequestDTO request = registro("Abcdefghijklmnopqrstu");

        assertThrows(IllegalArgumentException.class, () -> policy.validarRegistro(request));
    }

    @Test
    void validarRegistro_rechazaDatosPersonales() {
        ClienteRegistroRequestDTO request = registro("AnaLargaSegura123");

        assertThrows(IllegalArgumentException.class, () -> policy.validarRegistro(request));
    }

    @Test
    void validarRegistro_rechazaMasDeSetentaYDosBytesUtf8() {
        ClienteRegistroRequestDTO request = registro("😀".repeat(19));

        assertThrows(IllegalArgumentException.class, () -> policy.validarRegistro(request));
    }

    private ClienteRegistroRequestDTO registro(String password) {
        ClienteRegistroRequestDTO request = new ClienteRegistroRequestDTO();
        request.setPassword(password);
        request.setNombre("Ana");
        request.setApellido("Rojas");
        request.setCorreoElectronico("cliente@gmail.com");
        request.setNumDocumento("12345678");
        return request;
    }
}
