package pe.com.eclipsesuites.service;

import org.springframework.security.core.userdetails.UserDetails;
import pe.com.eclipsesuites.dto.request.ClienteRegistroRequestDTO;
import pe.com.eclipsesuites.entity.Cliente;

public interface IClienteAuthService {

    Cliente registrar(ClienteRegistroRequestDTO request);

    Cliente autenticar(String correoElectronico, String password);

    Cliente buscarPorCorreo(String correoElectronico);

    UserDetails crearUserDetails(Cliente cliente);

    boolean estaDisponible(String campo, String valor, String tipoDocumento);
}