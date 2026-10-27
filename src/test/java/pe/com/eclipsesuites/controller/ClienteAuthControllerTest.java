package pe.com.eclipsesuites.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.eclipsesuites.config.SecurityConfig;
import pe.com.eclipsesuites.entity.Cliente;
import pe.com.eclipsesuites.service.IClienteAuthService;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;
import pe.com.eclipsesuites.service.impl.DisponibilidadRateLimiter;
import pe.com.eclipsesuites.service.impl.IntentosLoginService;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class ClienteAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IClienteAuthService clienteAuthService;

    @MockitoBean
    private IEmpleadoAuthService empleadoAuthService;

    @MockitoBean
    private DisponibilidadRateLimiter disponibilidadRateLimiter;

    @MockitoBean
    private IntentosLoginService intentosLoginService;

    private Cliente cliente() {
        return Cliente.builder()
                .id(7)
                .tipoDocumento("DNI")
                .numDocumento("12345678")
                .paisEmisionDoc("PER")
                .correoElectronico("ana@gmail.com")
                .nombre("Ana")
                .apellido("Rojas")
                .telefono("+51999888777")
                .edad(28)
                .passwordHash(new BCryptPasswordEncoder(4).encode("EstaEsUnaFraseLargaDePrueba"))
                .build();
    }

    private User userDetails() {
        return (User) User.builder()
                .username("ana@example.com")
                .password("hash")
                .authorities("ROLE_CLIENTE")
                .build();
    }

    @Test
    void registroValidoIniciaSesionSinPedirCodigos() throws Exception {
        Cliente cliente = cliente();
        when(clienteAuthService.registrar(any())).thenReturn(cliente);
        when(clienteAuthService.crearUserDetails(cliente)).thenReturn(userDetails());

        mockMvc.perform(post("/api/auth/client/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoDocumento":"DNI","numDocumento":"12345678",
                                "paisEmisionDoc":"PER","correoElectronico":"ana@gmail.com",
                                "nombre":"Ana","apellido":"Rojas","telefono":"+51999888777",
                                "paisTelefono":"PE","edad":28,"password":"CuentaSegura123",
                                "aceptaPoliticaPrivacidad":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana@gmail.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.mensaje").value("Cuenta creada e inicio de sesión exitoso"))
                .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", notNullValue()));
    }

    @Test
    void registroAceptaEdadesLimitePermitidas() throws Exception {
        Cliente cliente = cliente();
        when(clienteAuthService.registrar(any())).thenReturn(cliente);
        when(clienteAuthService.crearUserDetails(cliente)).thenReturn(userDetails());

        for (int edad : new int[]{18, 120}) {
            mockMvc.perform(post("/api/auth/client/register").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tipoDocumento":"DNI","numDocumento":"12345678",
                                    "paisEmisionDoc":"PER","correoElectronico":"ana@gmail.com",
                                    "nombre":"Ana","apellido":"Rojas","telefono":"+51999888777",
                                    "paisTelefono":"PE","edad":%d,"password":"CuentaSegura123",
                                    "aceptaPoliticaPrivacidad":true}
                                    """.formatted(edad)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void login_validoCreaSesion() throws Exception {
        Cliente cliente = cliente();
        when(clienteAuthService.autenticar("ana@gmail.com", "EstaEsUnaFraseLargaDePrueba"))
                .thenReturn(cliente);
        when(clienteAuthService.crearUserDetails(cliente)).thenReturn(userDetails());

        mockMvc.perform(post("/api/auth/client/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"correoElectronico\":\"ana@gmail.com\"," +
                                "\"password\":\"EstaEsUnaFraseLargaDePrueba\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana@gmail.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", notNullValue()));
    }

    @Test
    void registro_correoInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/api/auth/client/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoDocumento":"DNI","numDocumento":"12345678",
                                "paisEmisionDoc":"PER","correoElectronico":"invalido",
                                "nombre":"Ana","apellido":"Rojas","telefono":"+51999888777",
                                "paisTelefono":"PE","edad":28,"password":"Cliente@123",
                                "aceptaPoliticaPrivacidad":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.correoElectronico").exists());

        verifyNoInteractions(clienteAuthService);
    }

    @Test
    void registroSinConsentimientoRetorna400() throws Exception {
        mockMvc.perform(post("/api/auth/client/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoDocumento":"DNI","numDocumento":"12345678",
                                "paisEmisionDoc":"PER","correoElectronico":"ana@gmail.com",
                                "nombre":"Ana","apellido":"Rojas","telefono":"+51999888777",
                                "paisTelefono":"PE","edad":28,"password":"CuentaSegura123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.aceptaPoliticaPrivacidad").exists());

        verifyNoInteractions(clienteAuthService);
    }

    @Test
    void registro_rechazaEdadMenorDeDieciocho() throws Exception {
        mockMvc.perform(post("/api/auth/client/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoDocumento":"DNI","numDocumento":"12345678",
                                "paisEmisionDoc":"PER","correoElectronico":"ana@gmail.com",
                                "nombre":"Ana","apellido":"Rojas","telefono":"+51999888777",
                                "paisTelefono":"PE","edad":17,"password":"CuentaSegura123",
                                "aceptaPoliticaPrivacidad":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.edad").exists());

        verifyNoInteractions(clienteAuthService);
    }

    @Test
    void registro_rechazaEdadMayorDeCientoVeinte() throws Exception {
        mockMvc.perform(post("/api/auth/client/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoDocumento":"DNI","numDocumento":"12345678",
                                "paisEmisionDoc":"PER","correoElectronico":"ana@gmail.com",
                                "nombre":"Ana","apellido":"Rojas","telefono":"+51999888777",
                                "paisTelefono":"PE","edad":121,"password":"CuentaSegura123",
                                "aceptaPoliticaPrivacidad":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.edad").exists());

        verifyNoInteractions(clienteAuthService);
    }

    @Test
    void registro_rechazaTelefonoQueNoCorrespondeAlPais() throws Exception {
        mockMvc.perform(post("/api/auth/client/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoDocumento":"DNI","numDocumento":"12345678",
                                "paisEmisionDoc":"PER","correoElectronico":"ana@gmail.com",
                                "nombre":"Ana","apellido":"Rojas","telefono":"+14165550123",
                                "paisTelefono":"PE","edad":28,"password":"CuentaSegura123",
                                "aceptaPoliticaPrivacidad":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.telefono").exists());

        verifyNoInteractions(clienteAuthService);
    }

    @Test
    void registro_rechazaTelefonoPeruanoConMenosDeNueveDigitos() throws Exception {
        mockMvc.perform(post("/api/auth/client/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoDocumento":"DNI","numDocumento":"12345678",
                                "paisEmisionDoc":"PER","correoElectronico":"ana@gmail.com",
                                "nombre":"Ana","apellido":"Rojas","telefono":"+5199988877",
                                "paisTelefono":"PE","edad":28,"password":"CuentaSegura123",
                                "aceptaPoliticaPrivacidad":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.telefono").exists());

        verifyNoInteractions(clienteAuthService);
    }

    @Test
    void disponibilidad_rechazaTelefonoQueNoCumpleElFormatoDelPais() throws Exception {
        when(disponibilidadRateLimiter.permitir(any())).thenReturn(true);

        mockMvc.perform(get("/api/auth/client/disponibilidad")
                        .param("campo", "telefono")
                        .param("valor", "+5199988877")
                        .param("paisTelefono", "PE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.disponible").value(false))
                .andExpect(jsonPath("$.mensaje")
                        .value("Ingresa un teléfono válido para el país seleccionado."));

        verifyNoInteractions(clienteAuthService);
    }

    @Test
    void disponibilidad_aceptaTelefonoPeruanoValido() throws Exception {
        when(disponibilidadRateLimiter.permitir(any())).thenReturn(true);
        when(clienteAuthService.estaDisponible("telefono", "+51999888777", null)).thenReturn(true);

        mockMvc.perform(get("/api/auth/client/disponibilidad")
                        .param("campo", "telefono")
                        .param("valor", "+51999888777")
                        .param("paisTelefono", "PE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.mensaje").value("Disponible"));

        verify(clienteAuthService).estaDisponible("telefono", "+51999888777", null);
    }

    @Test
    void disponibilidad_aceptaNumeroPeruanoDeLaCaptura() throws Exception {
        when(disponibilidadRateLimiter.permitir(any())).thenReturn(true);
        when(clienteAuthService.estaDisponible("telefono", "+51985576482", null)).thenReturn(true);

        mockMvc.perform(get("/api/auth/client/disponibilidad")
                        .param("campo", "telefono")
                        .param("valor", "+51985576482")
                        .param("paisTelefono", "PE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.mensaje").value("Disponible"));

        verify(clienteAuthService).estaDisponible("telefono", "+51985576482", null);
    }

    @Test
    void disponibilidad_rechazaFijoPeruanoDeOchoDigitos() throws Exception {
        when(disponibilidadRateLimiter.permitir(any())).thenReturn(true);

        mockMvc.perform(get("/api/auth/client/disponibilidad")
                        .param("campo", "telefono")
                        .param("valor", "+5111234567")
                        .param("paisTelefono", "PE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.disponible").value(false))
                .andExpect(jsonPath("$.mensaje")
                        .value("Ingresa un teléfono válido para el país seleccionado."));

        verifyNoInteractions(clienteAuthService);
    }
}