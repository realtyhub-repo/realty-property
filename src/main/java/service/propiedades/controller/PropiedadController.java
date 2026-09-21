package service.propiedades.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.propiedades.dto.internal.PropiedadFiltros;
import service.propiedades.dto.request.ActualizarPropiedadRequest;
import service.propiedades.dto.request.CambiarEstadoRequest;
import service.propiedades.dto.request.CrearPropiedadRequest;
import service.propiedades.dto.response.PropiedadDetalleResponse;
import service.propiedades.dto.response.PropiedadResponse;
import service.propiedades.security.ContextoUsuario;
import service.propiedades.security.UsuarioActual;
import service.propiedades.service.PropiedadService;

import java.util.UUID;

@RestController
@RequestMapping("/property")
@RequiredArgsConstructor
public class PropiedadController {

    private final PropiedadService propiedadService;

    @PostMapping
    public ResponseEntity<PropiedadResponse> crear(@Valid  @RequestBody CrearPropiedadRequest request,
                                                   @UsuarioActual @NonNull ContextoUsuario usuario){

        PropiedadResponse response = propiedadService.crear(usuario.rol(),usuario.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<PropiedadResponse>> listarPropiedades(
            @RequestParam(required = false)PropiedadFiltros filtros,
            Pageable pageable
            ){

        Page<PropiedadResponse> responsePage = propiedadService.listar(pageable,filtros);

        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropiedadDetalleResponse> obtenerDetalles(@PathVariable UUID id,
                                                                    HttpServletRequest servletRequest,
                                                                    @UsuarioActual ContextoUsuario contextoUsuario
                                                                    ){

        return  ResponseEntity.ok(propiedadService.obtenerDetalle(id,contextoUsuario, servletRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PropiedadResponse> actualizar(@PathVariable UUID propiedadId,
                                           @UsuarioActual @NonNull ContextoUsuario usuario,
                                           @Valid @RequestBody ActualizarPropiedadRequest request
                                           ){

        PropiedadResponse response = propiedadService.actualizar(propiedadId,usuario.rol(),usuario.userId(),request);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Void> cambiarEstado(@PathVariable UUID propiedadId,
                                              @UsuarioActual ContextoUsuario contextoUsuario,
                                              @Valid  @RequestBody CambiarEstadoRequest estadoRequest
                                              ){

        propiedadService.cambiarEstadoManual(propiedadId,estadoRequest.nuevoEstado(),contextoUsuario.rol(),contextoUsuario.userId());
        return ResponseEntity.noContent().build();
    }

}
