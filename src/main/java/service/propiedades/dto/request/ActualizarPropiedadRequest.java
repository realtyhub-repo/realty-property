package service.propiedades.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

        Map<String, Object> caracteristicas

) {
}