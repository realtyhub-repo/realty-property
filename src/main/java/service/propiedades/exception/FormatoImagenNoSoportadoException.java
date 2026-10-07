package service.propiedades.exception;

import lombok.Getter;
import service.propiedades.dto.internal.FormatoImagen;
import service.propiedades.dto.internal.ImagenInvalida;

import java.util.List;

@Getter
public class FormatoImagenNoSoportadoException extends RuntimeException {
    private final List<ImagenInvalida> imagenesInvalidas;

    public FormatoImagenNoSoportadoException(List<ImagenInvalida> imagenesInvalidas) {
        super("Formato no soportado en " + imagenesInvalidas.size()
                + " imagen(es). Formatos permitidos: " + FormatoImagen.extensionesPermitidas());
        this.imagenesInvalidas = List.copyOf(imagenesInvalidas);
    }

}
