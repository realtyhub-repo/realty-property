package service.propiedades.dto.response;

import lombok.Builder;
import service.propiedades.entity.EstadoComercial;

@Builder
public record PropiedadCoordenadasResponse(
        EstadoComercial estadoComercial,
        Double latitud,
        Double longitud
) {
}
