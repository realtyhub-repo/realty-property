package service.propiedades.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ErrorAlmacenamientoException extends RuntimeException {
    public ErrorAlmacenamientoException(String message) {
        super(message);
    }
}
