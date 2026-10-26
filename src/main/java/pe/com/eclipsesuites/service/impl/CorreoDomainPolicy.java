package pe.com.eclipsesuites.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.com.eclipsesuites.service.MxRecordVerifier;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class CorreoDomainPolicy {

    private final Set<String> dominiosPermitidos;
    private final Set<String> dominiosDesechables;
    private final MxRecordVerifier mxRecordVerifier;

    public CorreoDomainPolicy(
            @Value("${app.auth.email.dominios-permitidos:gmail.com,hotmail.com,outlook.com,yahoo.com}")
            String dominiosPermitidos,
            @Value("${app.auth.email.dominios-desechables:10minutemail.com,dispostable.com,guerrillamail.com,mailinator.com,tempmail.com,yopmail.com}")
            String dominiosDesechables,
            MxRecordVerifier mxRecordVerifier) {
        this.dominiosPermitidos = parsearDominios(dominiosPermitidos);
        this.dominiosDesechables = parsearDominios(dominiosDesechables);
        this.mxRecordVerifier = mxRecordVerifier;
    }

    public String validar(String correo, boolean verificarMx) {
        if (correo == null || !correo.matches("(?i)^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Ingresa un correo electrónico válido");
        }
        String dominio = correo.substring(correo.lastIndexOf('@') + 1).toLowerCase(Locale.ROOT);
        if (dominiosDesechables.contains(dominio)) {
            throw new IllegalArgumentException("No se aceptan correos temporales o desechables");
        }
        if (!dominiosPermitidos.contains(dominio)) {
            throw new IllegalArgumentException(
                    "Usa un correo de Gmail, Hotmail, Outlook o Yahoo");
        }
        if (verificarMx && !mxRecordVerifier.tieneRegistroMx(dominio)) {
            throw new IllegalArgumentException("El dominio del correo no recibe mensajes");
        }
        return correo;
    }

    private Set<String> parsearDominios(String dominios) {
        Set<String> resultado = new HashSet<>();
        for (String dominio : dominios.split(",")) {
            String normalizado = dominio.trim().toLowerCase(Locale.ROOT);
            if (!normalizado.isEmpty()) {
                resultado.add(normalizado);
            }
        }
        return Set.copyOf(resultado);
    }
}
