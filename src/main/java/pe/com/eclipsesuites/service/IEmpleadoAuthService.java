package pe.com.eclipsesuites.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import pe.com.eclipsesuites.entity.Empleado;

public interface IEmpleadoAuthService extends UserDetailsService {

    /**
     * Autentica al empleado contra la BD (valida existencia, estado activo y password BCrypt).
     */
    Empleado autenticar(String username, String password);

    /**
     * Devuelve los datos completos del empleado autenticado.
     */
    Empleado buscarPorUsername(String username);
}
