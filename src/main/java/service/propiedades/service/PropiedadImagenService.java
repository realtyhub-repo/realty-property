package service.propiedades.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.propiedades.dto.internal.RolUsuario;
import service.propiedades.dto.request.ConfirmarImagenRequest;
import service.propiedades.dto.response.ImagenResponse;
import service.propiedades.dto.response.UploadUrlResponse;
import service.propiedades.entity.Propiedad;
import service.propiedades.entity.PropiedadImagen;
import service.propiedades.exception.AccesoNoAutorizadoException;
import service.propiedades.exception.ErrorAlmacenamientoException;
import service.propiedades.exception.ImagenNoEncontradaException;
import service.propiedades.repository.PropiedadImagenRepository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropiedadImagenService {

    @Value("")
    private String urlBase;

    private final S3Service s3Service;
    private final PropiedadImagenRepository imagenRepository;
    private final PropiedadService propiedadService;

    public List<UploadUrlResponse> generarUrlSubida(UUID propiedadId, RolUsuario rol, UUID solicitanteId, List<String> nombresArchivo){

        if (rol == RolUsuario.CLIENTE)
            throw new AccesoNoAutorizadoException("acceso no autorizado");

        Propiedad propiedadPorId =propiedadService.buscarPorId(propiedadId);

        boolean esDueno = propiedadPorId.getAgenteId().equals(solicitanteId);
        boolean esAdmin = rol == RolUsuario.ADMINISTRADOR_CENTRAL;

        if (!esDueno && !esAdmin)
            throw new AccesoNoAutorizadoException("No tienes permiso sobre esta propiedad");


       return nombresArchivo.stream()
               .map(nombreArchivo->generarUploadUrlResponse(propiedadId,nombreArchivo))
               .toList();

    }


    public List<ImagenResponse> confirmarImagenes(UUID propiedadId, RolUsuario rol, UUID solicitanteId, List<ConfirmarImagenRequest> requests){

        Propiedad propiedad = propiedadService.buscarPorId(propiedadId);

        boolean esDueno = propiedad.getAgenteId().equals(solicitanteId);
        boolean esAdmin = rol == RolUsuario.ADMINISTRADOR_CENTRAL;

        if (!esDueno && !esAdmin)
            throw new AccesoNoAutorizadoException("No tienes permiso sobre esta propiedad");

        long cantidadPortadas = requests.stream()
                .filter(ConfirmarImagenRequest::esPortada)
                .count();

        if (cantidadPortadas > 1)
            throw new ErrorAlmacenamientoException("Solo una imagen puede ser portada");

        if (cantidadPortadas == 1) {
            imagenRepository.desmarcarPortadaActual(propiedadId);
        }

        List<PropiedadImagen> imagenes = requests.stream()
                .map(request -> PropiedadImagen.builder()
                        .propiedadId(propiedadId)
                        .keyR2(request.key())
                        .orden(request.orden())
                        .esPortada(request.esPortada())
                        .build())
                .toList();

        List<PropiedadImagen> guardadas = imagenRepository.saveAll(imagenes);

        return guardadas.stream()
                .map(imagen -> ImagenResponse.from(imagen, urlBase))
                .toList();
    }

    @Transactional
    public void marcarPortada(UUID propiedadId, UUID imagenId, RolUsuario rol, UUID solicitanteId){
        Propiedad propiedad = propiedadService.buscarPorId(propiedadId);

        boolean esDueno = propiedad.getAgenteId().equals(solicitanteId);
        boolean esAdmin = rol == RolUsuario.ADMINISTRADOR_CENTRAL;

        if (!esDueno && !esAdmin)
            throw new AccesoNoAutorizadoException("No tienes permiso sobre esta propiedad");

        imagenRepository.desmarcarPortadaActual(propiedadId);

        PropiedadImagen imagen = imagenRepository.findById(imagenId).orElseThrow(()->
                new ImagenNoEncontradaException("Imagen no encontrada")
                );

        imagen.setEsPortada(true);
        imagenRepository.save(imagen);

    }

    public void eliminar (UUID imagenId, RolUsuario rol, UUID solicitanteId){
        PropiedadImagen imagen = imagenRepository.findById(imagenId).orElseThrow(()->
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

    public List<ImagenResponse> listarPorPropiedad(UUID propiedadId){
        return imagenRepository.findByPropiedadIdOrderByOrden(propiedadId).stream()
                .map(I -> ImagenResponse.from(I, urlBase))
                .toList();
    }

    private UploadUrlResponse generarUploadUrlResponse(UUID propiedadId, String nombreArchivo){
        String extension = nombreArchivo.substring(nombreArchivo.lastIndexOf('.'));
        String key = String.format("propiedades/%s/%s.%s",propiedadId, UUID.randomUUID(),extension);

        String urlPresigned = s3Service.generarUrlPresigned(key, Duration.ofMinutes(15));
        return UploadUrlResponse.builder()
                .key(key)
                .uploadUrl(urlPresigned)
                .build();

    }


    }
