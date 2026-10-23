package pe.com.eclipsesuites;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.com.eclipsesuites.dao.ClienteRepository;
import pe.com.eclipsesuites.dto.request.ClienteRegistroRequestDTO;
import pe.com.eclipsesuites.entity.Cliente;
import pe.com.eclipsesuites.service.IClienteAuthService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:siweb_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class SiwebEclipseApplicationTests {

	@Autowired
	private IClienteAuthService clienteAuthService;

	@Autowired
	private ClienteRepository clienteRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void contextLoads() {
	}

	@Test
	void registroPersisteHashYPermiteIniciarSesion() {
		ClienteRegistroRequestDTO request = new ClienteRegistroRequestDTO();
		request.setTipoDocumento("DNI");
		request.setNumDocumento("87654321");
		request.setPaisEmisionDoc("PER");
		request.setCorreoElectronico("simulacion@example.com");
		request.setNombre("Cliente");
		request.setApellido("Simulado");
		request.setTelefono("999111222");
		request.setEdad(30);
		request.setPassword("CuentaSegura123");

		Cliente creado = clienteAuthService.registrar(request);
		Cliente persistido = clienteRepository.findByCorreoElectronicoIgnoreCase(
				"simulacion@example.com").orElseThrow();

		assertNotNull(creado.getId());
		assertEquals(creado.getId(), persistido.getId());
		assertNotEquals("CuentaSegura123", persistido.getPasswordHash());
		assertTrue(passwordEncoder.matches("CuentaSegura123", persistido.getPasswordHash()));
		assertEquals(creado, clienteAuthService.autenticar(
				"SIMULACION@example.com", "CuentaSegura123"));
	}

}
