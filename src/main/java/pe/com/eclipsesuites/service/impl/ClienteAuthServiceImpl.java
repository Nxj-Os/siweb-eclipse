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
import pe.com.eclipsesuites.dto.request.PerfilClienteUpdateDTO;
import pe.com.eclipsesuites.entity.Cliente;
import pe.com.eclipsesuites.service.IClienteAuthService;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

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
        String telefono = StringUtils.trim(request.getTelefono());

        if (!request.isAceptaPoliticaPrivacidad()) {
            throw new IllegalArgumentException("Debes aceptar los Términos y la Política de Datos Personales");
        }

        // Una cuenta eliminada se reutiliza: conserva su id y sus reservas.
        Cliente candidato = clienteRepository.findByCorreoElectronicoIgnoreCase(correo).orElse(null);
        if (candidato != null && candidato.estaActivo()) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo electrónico");
        }
        exigirLibre(clienteRepository.findByNumDocumento(documento), candidato,
                "Ya existe una cuenta con ese documento");
        exigirLibre(clienteRepository.findByTelefono(telefono), candidato,
                "Ya existe una cuenta con ese teléfono");

        Cliente cliente = candidato == null ? new Cliente() : candidato;
        cliente.setTipoDocumento(StringUtils.trim(request.getTipoDocumento()));
        cliente.setNumDocumento(documento);
        cliente.setPaisEmisionDoc(StringUtils.upperCase(StringUtils.trim(request.getPaisEmisionDoc())));
        cliente.setCorreoElectronico(correo);
        cliente.setNombre(StringUtils.trim(request.getNombre()));
        cliente.setApellido(StringUtils.trim(request.getApellido()));
        cliente.setTelefono(telefono);
        cliente.setEdad(request.getEdad());
        cliente.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        cliente.setAceptoPoliticaPrivacidad(true);
        cliente.setFechaAceptacionPrivacidad(Instant.now());
        cliente.setActivo(true);
        cliente.setFechaBaja(null);

        return clienteRepository.save(cliente);
    }

    private static void exigirLibre(Optional<Cliente> encontrado, Cliente candidato, String mensaje) {
        encontrado.filter(otro -> candidato == null || !candidato.getId().equals(otro.getId()))
                .ifPresent(otro -> {
                    throw new IllegalArgumentException(mensaje);
                });
    }

    @Override
    @Transactional
    public Cliente actualizarPerfil(String correoElectronico, PerfilClienteUpdateDTO request) {
        Cliente cliente = buscarPorCorreo(correoElectronico);
        if (!cliente.estaActivo()) {
            throw new IllegalArgumentException("Tu cuenta ya no está activa");
        }

        String telefono = StringUtils.trim(request.getTelefono());
        if (!telefono.equals(cliente.getTelefono())) {
            exigirLibre(clienteRepository.findByTelefono(telefono), cliente,
                    "Ya existe una cuenta con ese teléfono");
            cliente.setTelefono(telefono);
        }

        cliente.setNombre(StringUtils.trim(request.getNombre()));
        cliente.setApellido(StringUtils.trim(request.getApellido()));
        cliente.setEdad(request.getEdad());
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public void eliminarCuenta(String correoElectronico) {
        Cliente cliente = buscarPorCorreo(correoElectronico);
        cliente.setActivo(false);
        cliente.setFechaBaja(Instant.now());
        clienteRepository.save(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente autenticar(String correoElectronico, String password) {
        if (StringUtils.isBlank(correoElectronico) || StringUtils.isBlank(password)) {
            throw new BadCredentialsException("Credenciales inv├ílidas");
        }

        Cliente cliente = clienteRepository.findByCorreoElectronicoIgnoreCase(
                        normalizarCorreo(correoElectronico))
                .orElseThrow(() -> new BadCredentialsException("Credenciales inv├ílidas"));

        if (StringUtils.isBlank(cliente.getPasswordHash())
                || !passwordEncoder.matches(password, cliente.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inv├ílidas");
        }
        if (!cliente.estaActivo()) {
            throw new IllegalArgumentException(
                    "Tu cuenta fue eliminada. Puedes crear una nueva con el mismo correo.");
        }
        return cliente;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaDisponible(String campo, String valor, String tipoDocumento) {
        if (StringUtils.isBlank(campo) || StringUtils.isBlank(valor)) {
            throw new IllegalArgumentException("Completa el dato que deseas verificar");
        }

        return switch (campo) {
            // Una cuenta eliminada no bloquea el dato: queda libre para reutilizarlo.
            case "correo" -> clienteRepository.findByCorreoElectronicoIgnoreCase(
                    normalizarCorreo(valor)).filter(Cliente::estaActivo).isEmpty();
            case "documento" -> {
                if (StringUtils.isBlank(tipoDocumento)) {
                    throw new IllegalArgumentException("El tipo de documento es obligatorio");
                }
                yield clienteRepository.findByNumDocumento(StringUtils.trim(valor))
                        .filter(Cliente::estaActivo).isEmpty();
            }
            case "telefono" -> clienteRepository.findByTelefono(StringUtils.trim(valor))
                    .filter(Cliente::estaActivo).isEmpty();
            default -> throw new IllegalArgumentException("Campo de disponibilidad no válido");
        };
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
