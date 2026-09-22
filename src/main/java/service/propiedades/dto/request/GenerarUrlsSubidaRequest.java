package service.propiedades.dto.request;

import java.util.List;

public record GenerarUrlsSubidaRequest(
        List<String> nombresArchivo
) {
}
