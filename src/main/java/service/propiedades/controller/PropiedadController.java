package service.propiedades.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.propiedades.dto.internal.PropiedadFiltros;
import service.propiedades.dto.internal.RolUsuario;
import service.propiedades.dto.request.ActualizarPropiedadRequest;
import service.propiedades.dto.request.CambiarEstadoRequest;
import service.propiedades.dto.request.CrearPropiedadRequest;
import service.propiedades.dto.response.PropiedadDetalleResponse;
import service.propiedades.dto.response.PropiedadResponse;
import service.propiedades.security.ContextoUsuario;
import service.propiedades.security.UsuarioActual;
import service.propiedades.service.PropiedadService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/propiedades")
@RequiredArgsConstructor
public class PropiedadController {

    private final PropiedadService propiedadService;

    @PostMapping
    public ResponseEntity<PropiedadResponse> crear(@Valid  @RequestBody CrearPropiedadRequest request,
                                                   @UsuarioActual  ContextoUsuario usuario){

        PropiedadResponse response = propiedadService.crear(usuario.rol(),usuario.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<PropiedadResponse>> listarPropiedades(
            @ModelAttribute PropiedadFiltros filtros,
            Pageable pageable
            ){

        Page<PropiedadResponse> responsePage = propiedadService.listar(pageable,filtros);

        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropiedadDetalleResponse> obtenerDetalles(@PathVariable UUID id,
                                                                    HttpServletRequest servletRequest,
                                                                    @RequestHeader(value = "X-User-Id", required = false) UUID userId,
                                                                    @RequestHeader(value = "X-User-Role", required = false) RolUsuario userRole ){

        ContextoUsuario contexto = (userId != null && userRole != null)
                ? new ContextoUsuario(userId, userRole)
                : null;

        return  ResponseEntity.ok(propiedadService.obtenerDetalle(id,contexto, servletRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PropiedadResponse> actualizar(@PathVariable UUID id,
                                           @UsuarioActual @NonNull ContextoUsuario usuario,
                                           @Valid @RequestBody ActualizarPropiedadRequest request
                                           ){

        PropiedadResponse response = propiedadService.actualizar(id,usuario.rol(),usuario.userId(),request);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Void> cambiarEstado(@PathVariable UUID id,
                                              @UsuarioActual ContextoUsuario contextoUsuario,
                                              @Valid  @RequestBody CambiarEstadoRequest estadoRequest
                                              ){

        propiedadService.cambiarEstadoManual(id,estadoRequest.nuevoEstado(),contextoUsuario.rol(),contextoUsuario.userId());
        return ResponseEntity.noContent().build();
    }

}
