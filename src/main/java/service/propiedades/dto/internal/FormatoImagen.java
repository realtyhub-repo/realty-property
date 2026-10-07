package service.propiedades.dto.internal;

import lombok.Getter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum FormatoImagen {

    JPEG("image/jpeg", "jpg", "jpeg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    @Getter
    private final String contentType;
    private final Set<String> extensiones;

    FormatoImagen(String contentType, String... extensiones) {
        this.contentType = contentType;
        this.extensiones = Set.of(extensiones);
    }

    public static Optional<FormatoImagen> desdeNombreArchivo(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return Optional.empty();
        }
        String nombre = nombreArchivo.strip();
        int ultimoPunto = nombre.lastIndexOf('.');

        if (ultimoPunto <= 0 || ultimoPunto == nombre.length() - 1) {
            return Optional.empty();
        }
        String extension = nombre.substring(ultimoPunto + 1).toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(formato -> formato.extensiones.contains(extension))
                .findFirst();
    }

    public static String extensionesPermitidas() {
        return Arrays.stream(values())
                .flatMap(formato -> formato.extensiones.stream())
                .sorted()
                .collect(Collectors.joining(", "));
    }
}