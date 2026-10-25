package pe.com.eclipsesuites.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.eclipsesuites.dao.ClienteRepository;
import pe.com.eclipsesuites.dto.request.ClienteRegistroRequestDTO;
import pe.com.eclipsesuites.entity.Cliente;
import pe.com.eclipsesuites.service.IClienteAuthService;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ClienteAuthServiceImpl implements IClienteAuthService {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Cliente registrar(ClienteRegistroRequestDTO request) {
        String correo = normalizarCorreo(request.getCorreoElectronico());
        String documento = StringUtils.trim(request.getNumDocumento());

        if (clienteRepository.existsByCorreoElectronicoIgnoreCase(correo)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo electrónico");
        }
        if (clienteRepository.existsByNumDocumento(documento)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese documento");
        }

        Cliente cliente = Cliente.builder()
                .tipoDocumento(StringUtils.trim(request.getTipoDocumento()))
                .numDocumento(documento)
                .paisEmisionDoc(StringUtils.upperCase(StringUtils.trim(request.getPaisEmisionDoc())))
                .correoElectronico(correo)
                .nombre(StringUtils.trim(request.getNombre()))
                .apellido(StringUtils.trim(request.getApellido()))
                .telefono(StringUtils.trim(request.getTelefono()))
                .edad(request.getEdad())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente autenticar(String correoElectronico, String password) {
        if (StringUtils.isBlank(correoElectronico) || StringUtils.isBlank(password)) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        Cliente cliente = clienteRepository.findByCorreoElectronicoIgnoreCase(
                        normalizarCorreo(correoElectronico))
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (StringUtils.isBlank(cliente.getPasswordHash())
                || !passwordEncoder.matches(password, cliente.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        return cliente;
    }

    @Override
    public UserDetails crearUserDetails(Cliente cliente) {
        return User.withUsername(cliente.getCorreoElectronico())
                .password(cliente.getPasswordHash())
                .authorities("ROLE_CLIENTE")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente buscarPorCorreo(String correoElectronico) {
        return clienteRepository.findByCorreoElectronicoIgnoreCase(
                        normalizarCorreo(correoElectronico))
                .orElseThrow(() -> new UsernameNotFoundException("No existe el cliente"));
    }

    private String normalizarCorreo(String correoElectronico) {
        return StringUtils.trim(correoElectronico).toLowerCase(Locale.ROOT);
    }
}