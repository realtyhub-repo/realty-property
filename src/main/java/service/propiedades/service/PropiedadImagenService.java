package service.propiedades.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.propiedades.dto.internal.FormatoImagen;
import service.propiedades.dto.internal.ImagenInvalida;
import service.propiedades.dto.internal.RolUsuario;
import service.propiedades.dto.request.ConfirmarImagenRequest;
import service.propiedades.dto.response.ImagenResponse;
import service.propiedades.dto.response.UploadUrlResponse;
import service.propiedades.entity.Propiedad;
import service.propiedades.entity.PropiedadImagen;
import service.propiedades.exception.*;
import service.propiedades.repository.PropiedadImagenRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PropiedadImagenService {

    @Value("${R2_URL}")
    private String urlBase;

    private final S3Service s3Service;
    private final PropiedadImagenRepository imagenRepository;
    private final PropiedadService propiedadService;

    public List<UploadUrlResponse> generarUrlSubida(UUID propiedadId, RolUsuario rol, UUID solicitanteId, List<String> nombresArchivo) {

        if (rol == RolUsuario.CLIENTE)
            throw new AccesoNoAutorizadoException("acceso no autorizado");

        Propiedad propiedadPorId = propiedadService.buscarPorId(propiedadId);

        boolean esDueno = propiedadPorId.getAgenteId().equals(solicitanteId);
        boolean esAdmin = rol == RolUsuario.ADMINISTRADOR_CENTRAL;

        if (!esDueno && !esAdmin)
            throw new AccesoNoAutorizadoException("No tienes permiso sobre esta propiedad");

        validarFormatos(nombresArchivo);


        return nombresArchivo.stream()
                .map(nombreArchivo -> generarUploadUrlResponse(propiedadId, nombreArchivo))
                .toList();

    }

    @Transactional
    public List<ImagenResponse> confirmarImagenes(UUID propiedadId, RolUsuario rol, UUID solicitanteId, List<ConfirmarImagenRequest> requests) {

        Propiedad propiedad = propiedadService.buscarPorId(propiedadId);

        boolean esDueno = propiedad.getAgenteId().equals(solicitanteId);
        boolean esAdmin = rol == RolUsuario.ADMINISTRADOR_CENTRAL;

        if (!esDueno && !esAdmin)
            throw new AccesoNoAutorizadoException("No tienes permiso sobre esta propiedad");


        String prefijo = "propiedades/"+propiedadId+"/";
        boolean keyAjena = requests.stream().anyMatch(r->!r.key().startsWith(prefijo));

        if (keyAjena)
            throw new AccesoNoAutorizadoException("Una o más imágenes no pertenecen a esta propiedad");

        long cantidadPortadas = requests.stream()
                .filter(ConfirmarImagenRequest::esPortada)
                .count();

        if (cantidadPortadas > 1)
            throw new PortadaMultipleException("Solo una imagen puede ser portada");

        if (cantidadPortadas == 1) {
            imagenRepository.desmarcarPortadaActual(propiedadId);
        }
        int ordenBase = imagenRepository.obtenerOrdenMaximo(propiedadId) + 1;

        List<PropiedadImagen> imagenes = IntStream.range(0, requests.size())
                .mapToObj(i->{
                    ConfirmarImagenRequest request = requests.get(i);
                    return PropiedadImagen.builder()
                            .propiedadId(propiedadId)
                            .keyR2(request.key())
                            .orden(ordenBase+i)
                            .esPortada(request.esPortada())
                            .build();

                })
                .toList();


        List<PropiedadImagen> guardadas;

        try {
            guardadas =  imagenRepository.saveAll(imagenes);
        }catch (DataIntegrityViolationException e){
            throw new ImagenDuplicadaException("Una o más imágenes ya fueron registradas");
        }

        return guardadas.stream()
                .map(imagen -> ImagenResponse.from(imagen, urlBase))
                .toList();
    }

    @Transactional
    public void marcarPortada( UUID imagenId, RolUsuario rol, UUID solicitanteId) {
        PropiedadImagen imagen = imagenRepository.findById(imagenId).orElseThrow(() ->
                new ImagenNoEncontradaException("Imagen no encontrada")
        );

        Propiedad propiedad = propiedadService.buscarPorId(imagen.getPropiedadId());

        boolean esDueno = propiedad.getAgenteId().equals(solicitanteId);
        boolean esAdmin = rol == RolUsuario.ADMINISTRADOR_CENTRAL;

        if (!esDueno && !esAdmin)
            throw new AccesoNoAutorizadoException("No tienes permiso sobre esta propiedad");

        if (imagen.getEsPortada()) return;

        imagenRepository.desmarcarPortadaActual(propiedad.getId());
        imagen.setEsPortada(true);
    }

    public void eliminar(UUID imagenId, RolUsuario rol, UUID solicitanteId) {
        PropiedadImagen imagen = imagenRepository.findById(imagenId).orElseThrow(() ->
                new ImagenNoEncontradaException("Imagen no encontrada")
        );

        Propiedad propiedad = propiedadService.buscarPorId(imagen.getPropiedadId());

        boolean esDueno = propiedad.getAgenteId().equals(solicitanteId);
        boolean esAdmin = rol == RolUsuario.ADMINISTRADOR_CENTRAL;

        if (!esDueno && !esAdmin)
            throw new AccesoNoAutorizadoException("No tienes permiso sobre esta propiedad");

        s3Service.eliminarImagen(imagen.getKeyR2());
        imagenRepository.delete(imagen);

    }

    public List<ImagenResponse> listarPorPropiedad(UUID propiedadId) {
        return imagenRepository.findByPropiedadIdOrderByOrden(propiedadId).stream()
                .map(I -> ImagenResponse.from(I, urlBase))
                .toList();
    }

    private UploadUrlResponse generarUploadUrlResponse(UUID propiedadId, @NonNull String nombreArchivo) {
        String extension = nombreArchivo.substring(nombreArchivo.lastIndexOf('.')+1);
        String key = String.format("propiedades/%s/%s.%s", propiedadId, UUID.randomUUID(), extension);

        String contentType = obtenerContentType(nombreArchivo);

        String urlPresigned = s3Service.generarUrlPresigned(key, Duration.ofMinutes(15), contentType);
        return UploadUrlResponse.builder()
                .key(key)
                .uploadUrl(urlPresigned)
                .build();

    }

    private void  validarFormatos(List<String> nombresArchivos) {
        List<ImagenInvalida> invalidas = new ArrayList<>();


        for (int i = 0; i < nombresArchivos.size(); i++) {
            String nombre = nombresArchivos.get(i);
            Optional<FormatoImagen> formato = FormatoImagen.desdeNombreArchivo(nombre);

            if (formato.isEmpty()) {
                invalidas.add(new ImagenInvalida(i, nombre));
            }
        }

        if (!invalidas.isEmpty()) {
            throw new FormatoImagenNoSoportadoException(invalidas);
        }
    }

    private @NonNull String obtenerContentType(String nombreArchivo){
        return FormatoImagen.desdeNombreArchivo(nombreArchivo)
                .orElseThrow()
                .getContentType();
    }

}
