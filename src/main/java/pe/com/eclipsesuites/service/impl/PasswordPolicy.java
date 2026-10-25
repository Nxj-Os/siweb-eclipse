package pe.com.eclipsesuites.service.impl;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.com.eclipsesuites.dto.request.ClienteRegistroRequestDTO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PasswordPolicy {

    private final Resource clavesProhibidasResource;
    private Set<String> clavesProhibidas;

    public PasswordPolicy(@Value("classpath:security/claves-prohibidas.txt")
                          Resource clavesProhibidasResource) {
        this.clavesProhibidasResource = clavesProhibidasResource;
    }

    @PostConstruct
    void cargarClavesProhibidas() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                clavesProhibidasResource.getInputStream(), StandardCharsets.UTF_8))) {
            clavesProhibidas = reader.lines()
                    .map(String::trim)
                    .map(PasswordPolicy::normalizar)
                    .filter(clave -> !clave.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo cargar la lista local de claves prohibidas", ex);
        }
    }

    public void validarRegistro(ClienteRegistroRequestDTO request) {
        String password = request.getPassword();
        if (password == null) {
            throw new IllegalArgumentException("La contraseña no cumple la política de seguridad");
        }

        int caracteres = password.codePointCount(0, password.length());
        int bytesUtf8 = password.getBytes(StandardCharsets.UTF_8).length;
        if (caracteres < 8 || caracteres > 20 || bytesUtf8 > 72) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener entre 8 y 20 caracteres");
        }

        String passwordNormalizado = normalizar(password);
        if (clavesProhibidas.contains(passwordNormalizado)) {
            throw new IllegalArgumentException("Elige una contraseña que no sea común o fácil de adivinar");
        }

        if (contieneDatoPersonal(passwordNormalizado, request.getNombre())
                || contieneDatoPersonal(passwordNormalizado, request.getApellido())
                || contieneDatoPersonal(passwordNormalizado, request.getCorreoElectronico())
                || contieneDatoPersonal(passwordNormalizado, request.getNumDocumento())) {
            throw new IllegalArgumentException("La contraseña no debe contener tus datos personales");
        }
    }

    private boolean contieneDatoPersonal(String password, String dato) {
        if (dato == null) {
            return false;
        }
        String normalizado = normalizar(dato);
        if (normalizado.contains("@")) {
            normalizado = normalizado.substring(0, normalizado.indexOf('@'));
        }
        normalizado = normalizado.replaceAll("[^\\p{L}\\p{N}]", "");
        return normalizado.codePointCount(0, normalizado.length()) >= 3
                && password.contains(normalizado);
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }
}
