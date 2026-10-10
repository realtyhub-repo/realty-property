package service.propiedades.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GenerarUrlsSubidaRequest(
        @NotEmpty
        List<@NotBlank String> nombresArchivo
) {
}
