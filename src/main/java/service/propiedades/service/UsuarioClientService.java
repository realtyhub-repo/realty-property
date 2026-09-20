package service.propiedades.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import service.propiedades.dto.response.UsuarioInternalResponse;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioClientService {

    private final RestClient restClient;

    public UsuarioInternalResponse buscarAgente(UUID agenteId){
        return restClient.get()
                .uri("/usuario/internal/users/{id}", agenteId)
                .retrieve()
                .body(UsuarioInternalResponse.class);

    }

}
