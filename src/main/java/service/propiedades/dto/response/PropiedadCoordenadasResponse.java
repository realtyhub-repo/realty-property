package service.propiedades.dto.response;

import lombok.Builder;

@Builder
public record PropiedadCoordenadasResponse(
        Double latitud,
        Double longitud
) {
}
