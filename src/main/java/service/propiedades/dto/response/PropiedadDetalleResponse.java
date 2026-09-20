package service.propiedades.dto.response;

import lombok.Builder;
import service.propiedades.entity.EstadoComercial;
import service.propiedades.entity.Modalidad;
import service.propiedades.entity.Propiedad;
import service.propiedades.entity.TipoPropiedad;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record PropiedadDetalleResponse(

        UUID id,
        String titulo,
        String descripcion,
        BigDecimal precio,
        String direccion,
        String ciudad,
        TipoPropiedad tipoPropiedad,
        Modalidad modalidad,
        EstadoComercial estadoComercial,
        Map<String, Object> caracteristicas,

        UUID agenteId,
        String nombreAgente,

        List<ImagenResponse> imagenes,

        LocalDateTime createdAt

) {
    public static PropiedadDetalleResponse from(Propiedad propiedad, String nombreAgente, List<ImagenResponse> imagenes) {
        return PropiedadDetalleResponse.builder()
                .id(propiedad.getId())
                .titulo(propiedad.getTitulo())
                .descripcion(propiedad.getDescripcion())
                .precio(propiedad.getPrecio())
                .direccion(propiedad.getDireccion())
                .ciudad(propiedad.getCiudad())
                .tipoPropiedad(propiedad.getTipoPropiedad())
                .modalidad(propiedad.getModalidad())
                .estadoComercial(propiedad.getEstadoComercial())
                .caracteristicas(propiedad.getCaracteristicas())
                .agenteId(propiedad.getAgenteId())
                .nombreAgente(nombreAgente)
                .imagenes(imagenes)
                .createdAt(propiedad.getCreatedAt())
                .build();


    }
}