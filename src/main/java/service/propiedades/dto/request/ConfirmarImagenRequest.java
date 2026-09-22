package service.propiedades.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ConfirmarImagenRequest(

        @NotBlank
        String key,

        boolean esPortada

) {
}