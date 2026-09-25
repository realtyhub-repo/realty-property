package service.propiedades.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.propiedades.dto.response.PropiedadCoordenadasResponse;
import service.propiedades.service.PropiedadService;

import java.util.UUID;

@RestController
@RequestMapping("/internal/propiedades")
@RequiredArgsConstructor
public class PropiedadInternalController {

    private final PropiedadService propiedadService;


    @GetMapping("/{id}")
    public ResponseEntity<PropiedadCoordenadasResponse> obtenerCoordenadas(@PathVariable UUID id){

        return ResponseEntity.ok(propiedadService.encontrarCoordenadas(id));

    }
}
