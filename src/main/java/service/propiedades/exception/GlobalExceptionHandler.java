package service.propiedades.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import service.propiedades.dto.internal.ErrorResponse;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

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

    @ExceptionHandler(ContextoUsuarioInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleContextoUsuarioInvalidoException(ContextoUsuarioInvalidoException ex){
        return construirRespuesta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ErrorAlmacenamientoException.class)
    public ResponseEntity<ErrorResponse> handleErrorAlmacenamientoException(ErrorAlmacenamientoException ex){
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(PortadaDuplicadaException.class)
    public  ResponseEntity<ErrorResponse> handlePortadaDuplicadaException(PortadaDuplicadaException ex){
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ImagenNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleImagenNoEncontradaException(ImagenNoEncontradaException ex){
        return construirRespuesta(HttpStatus.NOT_FOUND,ex.getMessage());
    }

    @ExceptionHandler(ImagenDuplicadaException.class)
    public ResponseEntity<ErrorResponse> handleImagenDuplicadaException(ImagenDuplicadaException ex){
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado");
    }

    @ExceptionHandler(FormatoNoValidoException.class)
    public ResponseEntity<ErrorResponse> handleFormatoNoValidoException(FormatoNoValidoException ex){
        return construirRespuesta(HttpStatus.BAD_REQUEST,ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErros(MethodArgumentNotValidException ex){
        String mensaje = ex.getBindingResult().getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return construirRespuesta(HttpStatus.BAD_REQUEST,mensaje);
    }

    private ResponseEntity<ErrorResponse> construirRespuesta(HttpStatus status, String mensaje){
        ErrorResponse error = new ErrorResponse(mensaje, status.value(), LocalDateTime.now());
        return ResponseEntity.status(status).body(error);

    }

}
