package pe.com.eclipsesuites.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
import pe.com.eclipsesuites.dto.request.ClienteRegistroRequestDTO;

public class TelefonoPaisValidoValidator
        implements ConstraintValidator<TelefonoPaisValido, ClienteRegistroRequestDTO> {

    @Override
    public boolean isValid(ClienteRegistroRequestDTO request, ConstraintValidatorContext context) {
        if (request == null || StringUtils.isAnyBlank(request.getTelefono(), request.getPaisTelefono())) {
            return true;
        }

        boolean valid = TelefonoValidation.esValidoParaPais(
                request.getTelefono(), request.getPaisTelefono());
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("telefono")
                    .addConstraintViolation();
        }
        return valid;
    }
}
