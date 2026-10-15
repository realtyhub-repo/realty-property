package service.propiedades.dto.event;

import service.propiedades.entity.EstadoComercial;

import java.time.LocalDateTime;
import java.util.UUID;

public record PropiedadEstadoCambiadoEvent(
        UUID propiedadId,
        EstadoComercial estadoAnterior,
        EstadoComercial estadoNuevo,
        LocalDateTime ocurridoEn
) {
}
