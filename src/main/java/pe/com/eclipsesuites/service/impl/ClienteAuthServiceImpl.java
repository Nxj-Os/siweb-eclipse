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
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ClienteAuthServiceImpl implements IClienteAuthService {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final CorreoDomainPolicy correoDomainPolicy;
    private final PasswordPolicy passwordPolicy;

    @Override
    @Transactional
    public Cliente registrar(ClienteRegistroRequestDTO request) {
        if (!request.isAceptaPoliticaPrivacidad()) {
            throw new IllegalArgumentException("Debes aceptar el tratamiento de datos personales");
        }
        String correo = normalizarCorreo(request.getCorreoElectronico());
        String tipoDocumento = StringUtils.upperCase(StringUtils.trim(request.getTipoDocumento()));
        String documento = StringUtils.upperCase(StringUtils.trim(request.getNumDocumento()));
        String telefono = normalizarTelefono(request.getTelefono());

        passwordPolicy.validarRegistro(request);
        correoDomainPolicy.validar(correo, true);
        validarDocumento(tipoDocumento, documento);
        if (clienteRepository.existsByCorreoElectronicoIgnoreCase(correo)) {
            throw new DatosRegistroNoDisponiblesException();
        }
        if (clienteRepository.existsByNumDocumento(documento)) {
            throw new DatosRegistroNoDisponiblesException();
        }
        if (clienteRepository.existsByTelefono(telefono)) {
            throw new DatosRegistroNoDisponiblesException();
        }

        Cliente cliente = Cliente.builder()
                .tipoDocumento(tipoDocumento)
                .numDocumento(documento)
                .paisEmisionDoc(StringUtils.upperCase(StringUtils.trim(request.getPaisEmisionDoc())))
                .correoElectronico(correo)
                .nombre(StringUtils.trim(request.getNombre()))
                .apellido(StringUtils.trim(request.getApellido()))
                .telefono(telefono)
                .edad(request.getEdad())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .aceptoPoliticaPrivacidad(true)
                .fechaAceptacionPrivacidad(Instant.now())
                .build();

        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaDisponible(String campo, String valor, String tipoDocumento) {
        if (StringUtils.isBlank(campo) || StringUtils.isBlank(valor)) {
            throw new IllegalArgumentException("Completa el dato que deseas verificar");
        }
        return switch (campo) {
            case "correo" -> {
                String correo = normalizarCorreo(valor);
                correoDomainPolicy.validar(correo, false);
                yield !clienteRepository.existsByCorreoElectronicoIgnoreCase(correo);
            }
            case "documento" -> {
                String documento = StringUtils.upperCase(StringUtils.trim(valor));
                String tipo = StringUtils.upperCase(StringUtils.trim(tipoDocumento));
                validarDocumento(tipo, documento);
                yield !clienteRepository.existsByNumDocumento(documento);
            }
            case "telefono" -> !clienteRepository.existsByTelefono(normalizarTelefono(valor));
            default -> throw new IllegalArgumentException("Campo de disponibilidad no válido");
        };
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

    private String normalizarTelefono(String telefono) {
        String normalizado = StringUtils.trim(telefono).replaceAll("[\\s()-]", "");
        if (!normalizado.matches("^\\+[1-9]\\d{7,14}$")) {
            throw new IllegalArgumentException("Ingresa un teléfono válido en formato internacional");
        }
        return normalizado;
    }

    private void validarDocumento(String tipo, String documento) {
        boolean valido = switch (tipo) {
            case "DNI" -> documento.matches("^\\d{8}$");
            case "CE", "PASAPORTE" -> documento.matches("^[A-Z0-9]{6,20}$");
            default -> false;
        };
        if (!valido) {
            throw new IllegalArgumentException("El número de documento no es válido para el tipo elegido");
        }
    }
}