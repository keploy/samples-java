package com.example.demo.Validations;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = Utf8Validator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUtf8 {
    String message() default "contains invalid UTF-8 characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

