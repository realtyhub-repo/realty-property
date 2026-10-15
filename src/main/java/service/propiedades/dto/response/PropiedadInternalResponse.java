package service.propiedades.dto.response;

import lombok.Builder;
import service.propiedades.entity.EstadoComercial;
import service.propiedades.entity.Modalidad;
import service.propiedades.entity.Propiedad;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record PropiedadInternalResponse(
        UUID id,
        String titulo,
        String direccion,
        String ciudad,
        BigDecimal precio,
        Modalidad modalidad,
        EstadoComercial estadoComercial
) {

    public static PropiedadInternalResponse from(Propiedad propiedad) {
        return PropiedadInternalResponse.builder()
                .id(propiedad.getId())
                .titulo(propiedad.getTitulo())
                .direccion(propiedad.getDireccion())
                .ciudad(propiedad.getCiudad())
                .precio(propiedad.getPrecio())
                .modalidad(propiedad.getModalidad())
                .estadoComercial(propiedad.getEstadoComercial())
                .build();
    }
}
