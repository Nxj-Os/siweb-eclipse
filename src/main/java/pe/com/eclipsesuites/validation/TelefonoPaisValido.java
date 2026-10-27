package pe.com.eclipsesuites.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TelefonoPaisValidoValidator.class)
public @interface TelefonoPaisValido {

    String message() default "Ingresa un teléfono válido para el país seleccionado";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
