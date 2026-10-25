package pe.com.eclipsesuites.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pe.com.eclipsesuites.dao.EmpleadoRepository;
import pe.com.eclipsesuites.entity.Empleado;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpleadoAuthServiceImpl implements IEmpleadoAuthService {

    private static final String PREFIJO_ROL = "ROLE_";
    private static final String PREFIJO_PERMISO = "PERMISO_";
    private static final char SEPARADOR_PERMISOS = ',';

    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Empleado empleado = buscarPorUsername(username);

        return User.builder()
                .username(empleado.getUsernameLogin())
                .password(empleado.getPasswordHashBcrypt())
                .authorities(construirAuthorities(empleado))
                .disabled(!Boolean.TRUE.equals(empleado.getEstadoActivo()))
                .build();
    }

    @Override
    public Empleado autenticar(String username, String password) {
        Preconditions.checkArgument(StringUtils.isNotBlank(username),
                "El username es obligatorio");
        Preconditions.checkArgument(StringUtils.isNotBlank(password),
                "El password es obligatorio");

        String usernameLimpio = StringUtils.trim(username);
        UserDetails userDetails = loadUserByUsername(usernameLimpio);

        if (!userDetails.isEnabled()) {
            throw new DisabledException("Credenciales inválidas");
        }

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        return buscarPorUsername(usernameLimpio);
    }

    @Override
    public Empleado buscarPorUsername(String username) {
        Preconditions.checkArgument(StringUtils.isNotBlank(username),
                "El username no puede estar en blanco");
        String usernameLimpio = StringUtils.trim(username);
        return empleadoRepository.findByUsernameLogin(usernameLimpio)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Credenciales inválidas"));
    }

    private List<GrantedAuthority> construirAuthorities(Empleado empleado) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(
                PREFIJO_ROL + empleado.getRol().getNombreRol()));

        String permisos = empleado.getRol().getPermisosSistema();
        if (StringUtils.isNotBlank(permisos)) {
            Splitter splitter = Splitter.on(SEPARADOR_PERMISOS)
                    .trimResults()
                    .omitEmptyStrings();
            for (String permiso : splitter.split(permisos)) {
                authorities.add(new SimpleGrantedAuthority(PREFIJO_PERMISO + permiso));
            }
        }
        return ImmutableList.copyOf(authorities);
    }
}
