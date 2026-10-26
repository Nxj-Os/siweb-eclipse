package pe.com.eclipsesuites;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pe.com.eclipsesuites.dao.ClienteRepository;
import pe.com.eclipsesuites.dto.request.ClienteRegistroRequestDTO;
import pe.com.eclipsesuites.entity.Cliente;
import pe.com.eclipsesuites.service.IClienteAuthService;
import pe.com.eclipsesuites.service.MxRecordVerifier;

import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:siweb_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"app.auth.email.dominios-permitidos=example.com",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class SiwebEclipseApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private IClienteAuthService clienteAuthService;

	@Autowired
	private ClienteRepository clienteRepository;

	@MockitoBean
	private MxRecordVerifier mxRecordVerifier;

	@BeforeEach
	void configurarVerificacionMx() {
		when(mxRecordVerifier.tieneRegistroMx("example.com")).thenReturn(true);
	}

	@Test
	void contextLoads() {
	}

	@Test
	void rechazaRegistroSinConsentimientoDePrivacidad() {
		ClienteRegistroRequestDTO request = solicitud(
				"sin-consentimiento@example.com", "87654326", "+51999111227");
		request.setAceptaPoliticaPrivacidad(false);

		assertThrows(IllegalArgumentException.class, () -> clienteAuthService.registrar(request));
	}

	@Test
	void recorridoCompletoApiCreaCuentaSesionYPrivacidad() throws Exception {
		String correo = "recorrido@example.com";
		String cuerpoRegistro = """
				{"tipoDocumento":"DNI","numDocumento":"87654327","paisEmisionDoc":"PER",
				"correoElectronico":"recorrido@example.com","nombre":"Cliente",
				"apellido":"Recorrido","telefono":"+51999111228","edad":30,
				"password":"P4ss-Montana-Feliz","aceptaPoliticaPrivacidad":true}
				""";

		mockMvc.perform(post("/api/auth/client/register").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoRegistro))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rol").value("CLIENTE"))
				.andExpect(jsonPath("$.mensaje").value("Cuenta creada e inicio de sesión exitoso"))
				.andExpect(request().sessionAttribute(
						"SPRING_SECURITY_CONTEXT", org.hamcrest.Matchers.notNullValue()));

		Cliente creada = buscar(correo);
		assertTrue(creada.getAceptoPoliticaPrivacidad());
		assertNotNull(creada.getFechaAceptacionPrivacidad());
		assertEquals(creada, clienteAuthService.autenticar(correo, "P4ss-Montana-Feliz"));
	}

	private ClienteRegistroRequestDTO solicitud(String correo, String documento, String telefono) {
		ClienteRegistroRequestDTO request = new ClienteRegistroRequestDTO();
		request.setTipoDocumento("DNI");
		request.setNumDocumento(documento);
		request.setPaisEmisionDoc("PER");
		request.setCorreoElectronico(correo);
		request.setNombre("Cliente");
		request.setApellido("Simulado");
		request.setTelefono(telefono);
		request.setEdad(30);
		request.setPassword("CuentaSegura123");
		request.setAceptaPoliticaPrivacidad(true);
		return request;
	}

	private Cliente buscar(String correo) {
		return clienteRepository.findByCorreoElectronicoIgnoreCase(correo).orElseThrow();
	}

}
