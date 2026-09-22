package service.propiedades.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.propiedades.dto.request.ConfirmarImagenRequest;
import service.propiedades.dto.response.ImagenResponse;
import service.propiedades.dto.response.UploadUrlResponse;
import service.propiedades.security.ContextoUsuario;
import service.propiedades.security.UsuarioActual;
import service.propiedades.service.PropiedadImagenService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PropiedadImagenController {

    private final PropiedadImagenService imagenService;

    @PostMapping("/propiedades/{id}/imagenes/upload-urls")
    public ResponseEntity<List<UploadUrlResponse>> generarUls(@PathVariable UUID id,
                                                              @UsuarioActual ContextoUsuario usuario,
                                                              @RequestBody List<String> nombresArchivos
    ) {

        List<UploadUrlResponse> uploadUrlResponses = imagenService.generarUrlSubida(id, usuario.rol(), usuario.userId(), nombresArchivos);

        return ResponseEntity.ok(uploadUrlResponses);
    }

    @PostMapping("/propiedades/{id}/imagenes/confirmar")
    public ResponseEntity<List<ImagenResponse>> confirmarImagenes(@PathVariable UUID id,
                                                                  @UsuarioActual ContextoUsuario usuario,
                                                                  @RequestBody List<ConfirmarImagenRequest> imagenRequests) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                imagenService.confirmarImagenes(
                        id,
                        usuario.rol(),
                        usuario.userId(),
                        imagenRequests));
    }

    @PutMapping("/imagenes/{imagenId}/portada")
    public ResponseEntity<Void> marcarPortada(@PathVariable UUID imagenId,
                                              @UsuarioActual ContextoUsuario usuario
                                              ){

        imagenService.marcarPortada(imagenId,usuario.rol(),usuario.userId());

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/imagenes/{imagenId}")
    public ResponseEntity<Void> eliminarImagen(@PathVariable UUID imagenId,
                                               @UsuarioActual ContextoUsuario usuario
    ){

        imagenService.eliminar(imagenId, usuario.rol(), usuario.userId());

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
