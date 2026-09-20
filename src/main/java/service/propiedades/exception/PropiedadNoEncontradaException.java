package service.propiedades.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PropiedadNoEncontradaException extends RuntimeException {
    public PropiedadNoEncontradaException(String message) {
        super(message);
    }
}
