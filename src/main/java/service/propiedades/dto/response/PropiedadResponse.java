package service.propiedades.dto.response;

import lombok.Builder;
import service.propiedades.entity.EstadoComercial;
import service.propiedades.entity.Modalidad;
import service.propiedades.entity.Propiedad;
import service.propiedades.entity.TipoPropiedad;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
public record PropiedadResponse(

        UUID id,
        String titulo,
        BigDecimal precio,
        String ciudad,
        TipoPropiedad tipoPropiedad,
        Modalidad modalidad,
        EstadoComercial estadoComercial,

        String urlPortada
) {
    public static PropiedadResponse from(Propiedad propiedad, String urlPortada) {
        return PropiedadResponse.builder()
                .id(propiedad.getId())
                .titulo(propiedad.getTitulo())
                .precio(propiedad.getPrecio())
                .ciudad(propiedad.getCiudad())
                .tipoPropiedad(propiedad.getTipoPropiedad())
                .modalidad(propiedad.getModalidad())
                .estadoComercial(propiedad.getEstadoComercial())
                .urlPortada(urlPortada)
                .build();
    }
}