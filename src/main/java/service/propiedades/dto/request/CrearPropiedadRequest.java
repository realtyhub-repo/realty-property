package service.propiedades.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;
import service.propiedades.entity.Modalidad;
import service.propiedades.entity.TipoPropiedad;

import java.math.BigDecimal;
import java.util.Map;

@Builder
public record CrearPropiedadRequest(

        @NotBlank
        @Size(max = 150)
        String titulo,

        @NotBlank
        @Size(max = 2000)
        String descripcion,

        @NotNull
        @Digits(integer = 15, fraction = 0)
        @Positive
        BigDecimal precio,

        @NotBlank
        @Size(max = 255)
        String direccion,

        @NotBlank
        @Size(max = 100)
        String ciudad,

        @NotNull
        TipoPropiedad tipoPropiedad,

        @NotNull
        Modalidad modalidad,

        Map<String, Object> caracteristicas

) {
    public CrearPropiedadRequest {
        if (caracteristicas == null) {
            caracteristicas = Map.of();
        }
    }
}
