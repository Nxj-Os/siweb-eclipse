package pe.com.eclipsesuites.validation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.regex.Pattern;

public final class TelefonoValidation {

    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    /**
     * Reglas estrictas por país, aplicadas además de libphonenumber.
     * PE: solo móvil de 9 dígitos que empieza con 9 (se rechazan fijos de 7-8 dígitos).
     */
    private static final Map<String, Pattern> REGLAS_NACIONALES = Map.of(
            "PE", Pattern.compile("^9\\d{8}$")
    );

    private TelefonoValidation() {
    }

    public static boolean esValidoParaPais(String telefono, String region) {
        if (StringUtils.isAnyBlank(telefono, region)
                || !PHONE_NUMBER_UTIL.getSupportedRegions().contains(region)) {
            return false;
        }

        try {
            PhoneNumber numero = PHONE_NUMBER_UTIL.parse(telefono, region);
            if (!PHONE_NUMBER_UTIL.isValidNumberForRegion(numero, region)
                    || !PHONE_NUMBER_UTIL.format(numero, PhoneNumberUtil.PhoneNumberFormat.E164)
                    .equals(telefono)) {
                return false;
            }
            Pattern regla = REGLAS_NACIONALES.get(region);
            return regla == null
                    || regla.matcher(String.valueOf(numero.getNationalNumber())).matches();
        } catch (NumberParseException ex) {
            return false;
        }
    }
}
