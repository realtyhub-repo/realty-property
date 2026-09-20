package service.propiedades.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import service.propiedades.dto.internal.ErrorResponse;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccesoNoAutorizadoException.class)
    public ResponseEntity<ErrorResponse> handleAccesoNoAutorizadoException(AccesoNoAutorizadoException ex){
        return construirRespuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }


    @ExceptionHandler(PropiedadNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handlePropiedadNoEncontradaException(PropiedadNoEncontradaException ex){
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> construirRespuesta(HttpStatus status, String mensaje){
        ErrorResponse error = new ErrorResponse(mensaje, status.value(), LocalDateTime.now());
        return ResponseEntity.status(status).body(error);

    }

}
