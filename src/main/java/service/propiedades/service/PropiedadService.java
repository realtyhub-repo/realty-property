package service.propiedades.service;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import service.propiedades.dto.internal.PropiedadFiltros;
import service.propiedades.dto.internal.RolUsuario;
import service.propiedades.dto.request.ActualizarPropiedadRequest;
import service.propiedades.dto.request.CrearPropiedadRequest;
import service.propiedades.dto.response.ImagenResponse;
import service.propiedades.dto.response.PropiedadDetalleResponse;
import service.propiedades.dto.response.PropiedadResponse;
import service.propiedades.dto.response.UsuarioInternalResponse;
import service.propiedades.entity.EstadoComercial;
import service.propiedades.entity.Propiedad;
import service.propiedades.entity.PropiedadImagen;
import service.propiedades.exception.AccesoNoAutorizadoException;
import service.propiedades.exception.PropiedadNoEncontradaException;
import service.propiedades.repository.PropiedadImagenRepository;
import service.propiedades.repository.PropiedadRepository;
import service.propiedades.repository.PropiedadSpecifications;
import service.propiedades.security.ContextoUsuario;

import javax.sound.sampled.Port;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropiedadService {

    private final PropiedadRepository propiedadRepository;
    private final UsuarioClientService usuarioClientService;
    private final StringRedisTemplate redisTemplate;
    private final PropiedadImagenService propiedadImagenService;
    private final PropiedadImagenRepository propiedadImagenRepository;

    @Value("${R2_URL}")
    private String urlBase;

    public PropiedadResponse crear(RolUsuario rolSolicitante, UUID idSolicitante, CrearPropiedadRequest request) {

        if (rolSolicitante == RolUsuario.CLIENTE)
            throw new AccesoNoAutorizadoException("acceso no autorizado");

        Propiedad propiedadBuild = Propiedad.builder()
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .precio(request.precio())
                .direccion(request.direccion())
                .ciudad(request.ciudad())
                .tipoPropiedad(request.tipoPropiedad())
                .modalidad(request.modalidad())
                .estadoComercial(EstadoComercial.DISPONIBLE)
                .caracteristicas(request.caracteristicas())
                .agenteId(idSolicitante)
                .build();

        Propiedad propiedadGuardada = propiedadRepository.save(propiedadBuild);

        UsuarioInternalResponse usuarioInternalResponse = usuarioClientService.buscarAgente(idSolicitante);

        return PropiedadResponse.from(propiedadGuardada, null);

    }

    public PropiedadDetalleResponse obtenerDetalle(UUID propiedadId, ContextoUsuario usuario, HttpServletRequest servletRequest) {
        Propiedad propiedadPorId = buscarPorId(propiedadId);

        publicarVistaSiAplica(propiedadId, propiedadPorId.getAgenteId(), usuario, servletRequest);

        List<ImagenResponse> propiedadImagenList = propiedadImagenService.listarPorPropiedad(propiedadId);

        UsuarioInternalResponse usuarioInternalResponse = usuarioClientService.buscarAgente(propiedadPorId.getAgenteId());

        return PropiedadDetalleResponse.from(propiedadPorId, usuarioInternalResponse.nombre(), propiedadImagenList);

    }

    public Page<PropiedadResponse> listar(Pageable pageable, @NonNull PropiedadFiltros propiedadFiltros) {
        Specification<Propiedad> spec = Specification
                .where(PropiedadSpecifications.tienePropiedad(propiedadFiltros.tipoPropiedad()))
                .and(PropiedadSpecifications.tieneModalidad(propiedadFiltros.modalidad()))
                .and(PropiedadSpecifications.tieneEstadoComercial(propiedadFiltros.estadoComercial()))
                .and(PropiedadSpecifications.tieneCiudad(propiedadFiltros.ciudad()))
                .and(PropiedadSpecifications.tienePrecio(propiedadFiltros.precioMin(), propiedadFiltros.precioMax()));


        Page<Propiedad> propiedadPage = propiedadRepository.findAll(spec, pageable);

        List<UUID> propiedadesIds = propiedadPage.stream()
                .map(Propiedad::getId)
                .toList();

        List<PropiedadImagen> propiedadImagenList = propiedadImagenRepository.findByPropiedadIdInAndEsPortadaTrue(propiedadesIds);

        Map<UUID, PropiedadImagen> imagenesMap = propiedadImagenList.stream()
                .collect(Collectors.toMap(
                        PropiedadImagen::getPropiedadId,
                        imagen -> imagen
                ));

        return new PageImpl<>(

                propiedadPage.stream()
                        .map(propiedad -> {
                            PropiedadImagen portada = imagenesMap.get(propiedad.getId());
                            String urlPortada = portada != null ? urlBase + portada.getKeyR2() : null;


                            return new PropiedadResponse(
                                    propiedad.getId(),
                                    propiedad.getTitulo(),
                                    propiedad.getPrecio(),
                                    propiedad.getCiudad(),
                                    propiedad.getTipoPropiedad(),
                                    propiedad.getModalidad(),
                                    propiedad.getEstadoComercial(),
                                    urlPortada
                                    );

                        }).toList(),
                pageable,
                propiedadPage.getTotalElements());

    }

    public PropiedadResponse actualizar(UUID propiedadId, RolUsuario rol, UUID solicitanteId, ActualizarPropiedadRequest request){

        Propiedad propiedad = buscarPorId(propiedadId);

        boolean esDueno = propiedad.getAgenteId().equals(solicitanteId);
        boolean esAdmin = rol == RolUsuario.ADMINISTRADOR_CENTRAL;

        if (!esDueno && !esAdmin)
            throw new AccesoNoAutorizadoException("No tienes permiso sobre esta propiedad");

        if (request.titulo() != null)
            propiedad.setTitulo(request.titulo());

        if (request.descripcion() != null)
            propiedad.setDescripcion(request.descripcion());

        if (request.precio() != null)
            propiedad.setPrecio(request.precio());

        if (request.direccion() != null)
            propiedad.setDireccion(request.direccion());

        if (request.ciudad() != null)
            propiedad.setCiudad(request.ciudad());

        if (request.caracteristicas() != null)
            propiedad.setCaracteristicas(request.caracteristicas());

        propiedadRepository.save(propiedad);


        return PropiedadResponse.from(propiedad,null);
    }


    public void cambiarEstadoManual(UUID propiedadId, EstadoComercial nuevo, RolUsuario rol, UUID solicitanteId){


        if (rol == RolUsuario.CLIENTE)
            throw new AccesoNoAutorizadoException("acceso no autorizado");

        Propiedad propiedadPorId = buscarPorId(propiedadId);

        boolean esDueno = propiedadPorId.getAgenteId().equals(solicitanteId);
        boolean esAdmin = rol == RolUsuario.ADMINISTRADOR_CENTRAL;

        if (!esDueno && !esAdmin)
            throw new AccesoNoAutorizadoException("No tienes permiso sobre esta propiedad");

        cambiarEstado(propiedadId,nuevo);

    }

    public void cambiarEstado(UUID id, EstadoComercial nuevo){
        Propiedad propiedad = buscarPorId(id);
        propiedad.setEstadoComercial(nuevo);
        propiedadRepository.save(propiedad);
        log.info("publica evento");
    }


    public Propiedad buscarPorId(UUID id) {
        return propiedadRepository.findById(id).orElseThrow(() ->
                new PropiedadNoEncontradaException("Propiedad no encontrada")
        );
    }


    //interno
    private void publicarVistaSiAplica(UUID propiedadId, UUID agenteId, ContextoUsuario usuario, HttpServletRequest request) {
        boolean esPropietario = usuario != null && agenteId.equals(usuario.userId());
        if (esPropietario) return;

        String identificador = usuario != null ? usuario.userId().toString() : request.getRemoteAddr();
        String key = "vista:" + propiedadId + ":" + identificador;

        Boolean propiedadVista = redisTemplate.opsForValue()
                .setIfAbsent(key, Instant.now().toString(), Duration.ofMinutes(30));

        if (Boolean.TRUE.equals(propiedadVista)) {
            log.info("evento publicado");
        }
    }

}
