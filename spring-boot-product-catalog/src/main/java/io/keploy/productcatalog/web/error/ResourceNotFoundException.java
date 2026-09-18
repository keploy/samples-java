package io.keploy.productcatalog.web.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// @ResponseStatus is a fallback: for a normal request the @ExceptionHandler in
// GlobalExceptionHandler wins and returns the uniform {status,error,message} body. But if the
// body can't be written (e.g. the client sent Accept: application/xml), that write failure is not
// re-entrant into @ExceptionHandler resolution, so ResponseStatusExceptionResolver uses this
// status to answer body-lessly with 404 instead of letting the exception surface as a 500.
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
