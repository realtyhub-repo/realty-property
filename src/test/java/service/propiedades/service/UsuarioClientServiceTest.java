package service.propiedades.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import service.propiedades.dto.response.UsuarioInternalResponse;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UsuarioClientServiceTest {

    private static final String BASE_URL = "http://localhost:8082";

    private MockRestServiceServer servidorSimulado;
    private UsuarioClientService usuarioClientService;
    private UUID agenteId;

    @BeforeEach
    void setUp() {
        // Mismo RestClient que usa la aplicacion, pero conectado a un servidor simulado
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        servidorSimulado = MockRestServiceServer.bindTo(builder).build();
        usuarioClientService = new UsuarioClientService(builder.build());

        agenteId = UUID.randomUUID();
    }

    private String urlEsperada() {
        return BASE_URL + "/internal/usuario/detalle/" + agenteId;
    }

    // ------------------------------------------------------------------
    // buscarAgente() - casos correctos
    // ------------------------------------------------------------------

    @Test
    void buscarAgente_conRespuestaExitosa_devuelveElUsuarioConIdYNombre() {
        String json = "{\"id\":\"" + agenteId + "\",\"nombre\":\"Carlos Perez\"}";
        servidorSimulado.expect(requestTo(urlEsperada()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        UsuarioInternalResponse resultado = usuarioClientService.buscarAgente(agenteId);

        assertNotNull(resultado);
        assertEquals(agenteId, resultado.id());
        assertEquals("Carlos Perez", resultado.nombre());
        // Confirma que se hizo exactamente la llamada GET esperada a /internal/usuario/detalle/{id}
        servidorSimulado.verify();
    }

    @Test
    void buscarAgente_siLaRespuestaTraeCamposAdicionales_losIgnoraYDevuelveElUsuario() {
        // El servicio de usuarios podria devolver mas datos (correo, rol...) de los que Property necesita
        String json = "{\"id\":\"" + agenteId + "\",\"nombre\":\"Ana Gomez\","
                + "\"email\":\"ana@realtyhub.test\",\"rol\":\"AGENTE\"}";
        servidorSimulado.expect(requestTo(urlEsperada()))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        UsuarioInternalResponse resultado = usuarioClientService.buscarAgente(agenteId);

        assertEquals("Ana Gomez", resultado.nombre());
    }

    // ------------------------------------------------------------------
    // buscarAgente() - fallos del servicio de usuarios
    // ------------------------------------------------------------------

    @Test
    void buscarAgente_cuandoElUsuarioNoExiste_propagaElError404() {
        servidorSimulado.expect(requestTo(urlEsperada()))
                .andRespond(withResourceNotFound());

        assertThrows(HttpClientErrorException.NotFound.class,
                () -> usuarioClientService.buscarAgente(agenteId));
    }

    @Test
    void buscarAgente_cuandoElServicioDeUsuariosFalla_propagaElError500() {
        servidorSimulado.expect(requestTo(urlEsperada()))
                .andRespond(withServerError());

        assertThrows(HttpServerErrorException.class,
                () -> usuarioClientService.buscarAgente(agenteId));
    }

    @Test
    void buscarAgente_cuandoElServicioDeUsuariosNoEstaDisponible_lanzaResourceAccessException() {
        servidorSimulado.expect(requestTo(urlEsperada()))
                .andRespond(request -> {
                    throw new IOException("Conexion rechazada");
                });

        assertThrows(ResourceAccessException.class,
                () -> usuarioClientService.buscarAgente(agenteId));
    }

    @Test
    void buscarAgente_cuandoLaRespuestaLlegaSinCuerpo_devuelveNull() {
        servidorSimulado.expect(requestTo(urlEsperada()))
                .andRespond(withSuccess());

        UsuarioInternalResponse resultado = usuarioClientService.buscarAgente(agenteId);

        assertNull(resultado);
    }
}