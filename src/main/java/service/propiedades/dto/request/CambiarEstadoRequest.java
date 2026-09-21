package service.propiedades.dto.request;

import jakarta.validation.constraints.NotNull;
import service.propiedades.entity.EstadoComercial;

public record CambiarEstadoRequest(
        @NotNull
        EstadoComercial nuevoEstado
) {}