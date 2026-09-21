package service.propiedades.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PortadaDuplicadaException extends RuntimeException {
    public PortadaDuplicadaException(String message) {
        super(message);
    }
}
