package service.propiedades.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;

@Builder
public record ActualizarPropiedadRequest(

        @Size(max = 150)
        String titulo,

        @Size(max = 2000)
        String descripcion,

        @Digits(integer = 15, fraction = 0)
        @Positive
        BigDecimal precio,

        @Size(max = 255)
        String direccion,

        @Size(max = 100)
        String ciudad,

        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        @NotNull
        Double latitud,

        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        @NotNull
        Double longitud,

        Map<String, Object> caracteristicas

) {
}