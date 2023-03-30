package com.example.demo.Validations;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class Utf8Validator implements ConstraintValidator<ValidUtf8, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value.getBytes(StandardCharsets.UTF_8).length == value.length();
    }
}
