package service.propiedades.dto.response;

import lombok.Builder;
import service.propiedades.entity.PropiedadImagen;

import java.util.UUID;

@Builder
public record ImagenResponse(
        UUID id,
        String url,
        int orden,
        boolean esPortada
) {

    public static ImagenResponse from(PropiedadImagen imagen, String urlBase){
        return ImagenResponse.builder()
                .id(imagen.getId())
                .url(urlBase+imagen.getKeyR2())
                .orden(imagen.getOrden())
                .esPortada(imagen.getEsPortada())
                .build()
    }

}