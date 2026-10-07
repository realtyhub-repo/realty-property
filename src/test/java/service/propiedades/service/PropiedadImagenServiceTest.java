package service.propiedades.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import service.propiedades.dto.internal.RolUsuario;
import service.propiedades.dto.request.ConfirmarImagenRequest;
import service.propiedades.dto.response.ImagenResponse;
import service.propiedades.dto.response.UploadUrlResponse;
import service.propiedades.entity.EstadoComercial;
import service.propiedades.entity.Modalidad;
import service.propiedades.entity.Propiedad;
import service.propiedades.entity.PropiedadImagen;
import service.propiedades.entity.TipoPropiedad;
import service.propiedades.exception.AccesoNoAutorizadoException;
import service.propiedades.exception.ErrorAlmacenamientoException;
import service.propiedades.exception.FormatoNoValidoException;
import service.propiedades.exception.ImagenDuplicadaException;
import service.propiedades.exception.ImagenNoEncontradaException;
import service.propiedades.exception.PropiedadNoEncontradaException;
import service.propiedades.repository.PropiedadImagenRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropiedadImagenServiceTest {

    private static final String URL_BASE = "https://cdn.realtyhub.test/";

    @Mock
    private S3Service s3Service;

    @Mock
    private PropiedadImagenRepository imagenRepository;

    @Mock
    private PropiedadService propiedadService;

    @Captor
    private ArgumentCaptor<List<PropiedadImagen>> imagenesCaptor;

    @InjectMocks
    private PropiedadImagenService imagenService;

    private UUID propiedadId;
    private UUID agenteId;
    private UUID otroUsuarioId;
    private Propiedad propiedad;

    @BeforeEach
    void setUp() {
        // El servicio lee R2_URL con @Value; en un test unitario lo inyectamos a mano
        ReflectionTestUtils.setField(imagenService, "urlBase", URL_BASE);

        propiedadId = UUID.randomUUID();
        agenteId = UUID.randomUUID();
        otroUsuarioId = UUID.randomUUID();

        propiedad = Propiedad.builder()
                .id(propiedadId)
                .titulo("Casa en el centro")
                .descripcion("Casa amplia")
                .precio(new BigDecimal("350000000"))
                .direccion("Calle 10 # 5-20")
                .ciudad("Monteria")
                .latitud(8.75)
                .longitud(-75.88)
                .tipoPropiedad(TipoPropiedad.CASA)
                .modalidad(Modalidad.VENTA)
                .estadoComercial(EstadoComercial.DISPONIBLE)
                .caracteristicas(Map.of())
                .agenteId(agenteId)
                .build();
    }

    private PropiedadImagen crearImagen(UUID id, String key, int orden, boolean esPortada) {
        return PropiedadImagen.builder()
                .id(id)
                .propiedadId(propiedadId)
                .keyR2(key)
                .orden(orden)
                .esPortada(esPortada)
                .build();
    }

    // ------------------------------------------------------------------
    // generarUrlSubida()
    // ------------------------------------------------------------------

    @Test
    void generarUrlSubida_conRolCliente_lanzaAccesoNoAutorizadoSinTocarNada() {
        assertThrows(AccesoNoAutorizadoException.class,
                () -> imagenService.generarUrlSubida(propiedadId, RolUsuario.CLIENTE, agenteId, List.of("casa.jpg")));

        verifyNoInteractions(propiedadService, s3Service);
    }

    @Test
    void generarUrlSubida_sinSerDuenoNiAdmin_lanzaAccesoNoAutorizadoYNoGeneraUrls() {
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);

        assertThrows(AccesoNoAutorizadoException.class,
                () -> imagenService.generarUrlSubida(propiedadId, RolUsuario.AGENTE, otroUsuarioId, List.of("casa.jpg")));

        verifyNoInteractions(s3Service);
    }

    @Test
    void generarUrlSubida_cuandoLaPropiedadNoExiste_lanzaPropiedadNoEncontrada() {
        when(propiedadService.buscarPorId(propiedadId))
                .thenThrow(new PropiedadNoEncontradaException("Propiedad no encontrada"));

        assertThrows(PropiedadNoEncontradaException.class,
                () -> imagenService.generarUrlSubida(propiedadId, RolUsuario.AGENTE, agenteId, List.of("casa.jpg")));

        verifyNoInteractions(s3Service);
    }

    @Test
    void generarUrlSubida_siendoElDueno_generaUnaUrlPorCadaArchivoConSuContentType() {
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);
        when(s3Service.generarUrlPresigned(anyString(), eq(Duration.ofMinutes(15)), eq("image/jpeg")))
                .thenReturn("https://upload.test/jpg");
        when(s3Service.generarUrlPresigned(anyString(), eq(Duration.ofMinutes(15)), eq("image/png")))
                .thenReturn("https://upload.test/png");

        List<UploadUrlResponse> resultado = imagenService.generarUrlSubida(
                propiedadId, RolUsuario.AGENTE, agenteId, List.of("casa.jpg", "sala.png"));

        assertEquals(2, resultado.size());
        assertEquals("https://upload.test/jpg", resultado.get(0).uploadUrl());
        assertEquals("https://upload.test/png", resultado.get(1).uploadUrl());
        assertTrue(resultado.get(0).key().startsWith("propiedades/" + propiedadId + "/"));
        assertTrue(resultado.get(1).key().startsWith("propiedades/" + propiedadId + "/"));
        assertNotEquals(resultado.get(0).key(), resultado.get(1).key());
    }

    @Test
    void generarUrlSubida_laKeyTieneFormatoCorrectoSinDobleCaracterPunto() {
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);
        when(s3Service.generarUrlPresigned(anyString(), any(Duration.class), eq("image/jpeg")))
                .thenReturn("https://upload.test/jpg");

        List<UploadUrlResponse> resultado = imagenService.generarUrlSubida(
                propiedadId, RolUsuario.AGENTE, agenteId, List.of("casa.jpg"));

        // Formato esperado: propiedades/<idPropiedad>/<uuid>.jpg
        String key = resultado.get(0).key();
        assertTrue(key.matches("propiedades/" + propiedadId + "/[0-9a-f\\-]{36}\\.jpg"),
                "La key tiene un formato inesperado: " + key);
    }

    @Test
    void generarUrlSubida_siendoAdministradorCentral_puedeGenerarUrlsDePropiedadesAjenas() {
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);
        when(s3Service.generarUrlPresigned(anyString(), any(Duration.class), eq("image/webp")))
                .thenReturn("https://upload.test/webp");

        List<UploadUrlResponse> resultado = imagenService.generarUrlSubida(
                propiedadId, RolUsuario.ADMINISTRADOR_CENTRAL, otroUsuarioId, List.of("fachada.webp"));

        assertEquals(1, resultado.size());
    }

    @ParameterizedTest
    @CsvSource({
            "foto.jpeg, image/jpeg",
            "foto.webp, image/webp",
            "FOTO.PNG, image/png",
            "foto.JPG, image/jpeg"
    })
    void generarUrlSubida_asignaElContentTypeSegunLaExtension(String nombreArchivo, String contentTypeEsperado) {
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);
        when(s3Service.generarUrlPresigned(anyString(), any(Duration.class), eq(contentTypeEsperado)))
                .thenReturn("https://upload.test/ok");

        List<UploadUrlResponse> resultado = imagenService.generarUrlSubida(
                propiedadId, RolUsuario.AGENTE, agenteId, List.of(nombreArchivo));

        assertEquals("https://upload.test/ok", resultado.get(0).uploadUrl());
    }

    @Test
    void generarUrlSubida_conFormatoNoPermitido_lanzaFormatoNoValidoYNoGeneraUrl() {
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);

        assertThrows(FormatoNoValidoException.class,
                () -> imagenService.generarUrlSubida(propiedadId, RolUsuario.AGENTE, agenteId, List.of("documento.pdf")));

        verifyNoInteractions(s3Service);
    }

    @Test
    void generarUrlSubida_conArchivoSinExtension_lanzaFormatoNoValido() {
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);

        assertThrows(FormatoNoValidoException.class,
                () -> imagenService.generarUrlSubida(propiedadId, RolUsuario.AGENTE, agenteId, List.of("foto")));

        verifyNoInteractions(s3Service);
    }

    // ------------------------------------------------------------------
    // confirmarImagenes()
    // ------------------------------------------------------------------

    @Test
    void confirmarImagenes_sinSerDuenoNiAdmin_lanzaAccesoNoAutorizadoYNoGuarda() {
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);
        List<ConfirmarImagenRequest> requests = List.of(
                ConfirmarImagenRequest.builder().key("k1").esPortada(false).build());

        assertThrows(AccesoNoAutorizadoException.class,
                () -> imagenService.confirmarImagenes(propiedadId, RolUsuario.AGENTE, otroUsuarioId, requests));

        verifyNoInteractions(imagenRepository);
    }

    @Test
    void confirmarImagenes_conMasDeUnaPortada_lanzaErrorYNoGuarda() {
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);
        List<ConfirmarImagenRequest> requests = List.of(
                ConfirmarImagenRequest.builder().key("k1").esPortada(true).build(),
                ConfirmarImagenRequest.builder().key("k2").esPortada(true).build());

        assertThrows(ErrorAlmacenamientoException.class,
                () -> imagenService.confirmarImagenes(propiedadId, RolUsuario.AGENTE, agenteId, requests));

        verify(imagenRepository, never()).saveAll(anyList());
        verify(imagenRepository, never()).desmarcarPortadaActual(any());
    }

    @Test
    void confirmarImagenes_conUnaPortada_desmarcaLaAnteriorYAsignaOrdenesConsecutivos() {
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);
        when(imagenRepository.obtenerOrdenMaximo(propiedadId)).thenReturn(2);
        when(imagenRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<ConfirmarImagenRequest> requests = List.of(
                ConfirmarImagenRequest.builder().key("propiedades/a.jpg").esPortada(true).build(),
                ConfirmarImagenRequest.builder().key("propiedades/b.jpg").esPortada(false).build());

        List<ImagenResponse> resultado = imagenService.confirmarImagenes(
                propiedadId, RolUsuario.AGENTE, agenteId, requests);

        verify(imagenRepository).desmarcarPortadaActual(propiedadId);
        verify(imagenRepository).saveAll(imagenesCaptor.capture());

        List<PropiedadImagen> guardadas = imagenesCaptor.getValue();
        assertEquals(2, guardadas.size());
        assertEquals("propiedades/a.jpg", guardadas.get(0).getKeyR2());
        assertEquals(3, guardadas.get(0).getOrden());
        assertTrue(guardadas.get(0).getEsPortada());
        assertEquals("propiedades/b.jpg", guardadas.get(1).getKeyR2());
        assertEquals(4, guardadas.get(1).getOrden());
        assertFalse(guardadas.get(1).getEsPortada());
        assertEquals(propiedadId, guardadas.get(0).getPropiedadId());

        assertEquals(2, resultado.size());
        assertEquals(URL_BASE + "propiedades/a.jpg", resultado.get(0).url());
        assertEquals(3, resultado.get(0).orden());
        assertTrue(resultado.get(0).esPortada());
    }

    @Test
    void confirmarImagenes_sinPortada_noDesmarcaYEmpiezaElOrdenEnCeroSiNoHabiaImagenes() {
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);
        // La consulta devuelve -1 cuando la propiedad todavia no tiene imagenes
        when(imagenRepository.obtenerOrdenMaximo(propiedadId)).thenReturn(-1);
        when(imagenRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<ConfirmarImagenRequest> requests = List.of(
                ConfirmarImagenRequest.builder().key("propiedades/a.jpg").esPortada(false).build(),
                ConfirmarImagenRequest.builder().key("propiedades/b.jpg").esPortada(false).build());

        imagenService.confirmarImagenes(propiedadId, RolUsuario.AGENTE, agenteId, requests);

        verify(imagenRepository, never()).desmarcarPortadaActual(any());
        verify(imagenRepository).saveAll(imagenesCaptor.capture());
        assertEquals(0, imagenesCaptor.getValue().get(0).getOrden());
        assertEquals(1, imagenesCaptor.getValue().get(1).getOrden());
    }

    @Test
    void confirmarImagenes_siendoAdministradorCentral_puedeConfirmarImagenesDePropiedadesAjenas() {
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);
        when(imagenRepository.obtenerOrdenMaximo(propiedadId)).thenReturn(-1);
        when(imagenRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<ConfirmarImagenRequest> requests = List.of(
                ConfirmarImagenRequest.builder().key("propiedades/a.jpg").esPortada(false).build());

        List<ImagenResponse> resultado = imagenService.confirmarImagenes(
                propiedadId, RolUsuario.ADMINISTRADOR_CENTRAL, otroUsuarioId, requests);

        assertEquals(1, resultado.size());
    }

    @Test
    void confirmarImagenes_cuandoUnaImagenYaEstaRegistrada_lanzaImagenDuplicada() {
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);
        when(imagenRepository.obtenerOrdenMaximo(propiedadId)).thenReturn(-1);
        when(imagenRepository.saveAll(anyList())).thenThrow(new DataIntegrityViolationException("key duplicada"));

        List<ConfirmarImagenRequest> requests = List.of(
                ConfirmarImagenRequest.builder().key("propiedades/a.jpg").esPortada(false).build());

        assertThrows(ImagenDuplicadaException.class,
                () -> imagenService.confirmarImagenes(propiedadId, RolUsuario.AGENTE, agenteId, requests));
    }

    // ------------------------------------------------------------------
    // marcarPortada()
    // ------------------------------------------------------------------

    @Test
    void marcarPortada_cuandoLaImagenNoExiste_lanzaImagenNoEncontrada() {
        UUID imagenId = UUID.randomUUID();
        when(imagenRepository.findById(imagenId)).thenReturn(Optional.empty());

        assertThrows(ImagenNoEncontradaException.class,
                () -> imagenService.marcarPortada(imagenId, RolUsuario.AGENTE, agenteId));

        verifyNoInteractions(propiedadService);
    }

    @Test
    void marcarPortada_sinSerDuenoNiAdmin_lanzaAccesoNoAutorizadoYNoCambiaNada() {
        UUID imagenId = UUID.randomUUID();
        PropiedadImagen imagen = crearImagen(imagenId, "propiedades/a.jpg", 0, false);
        when(imagenRepository.findById(imagenId)).thenReturn(Optional.of(imagen));
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);

        assertThrows(AccesoNoAutorizadoException.class,
                () -> imagenService.marcarPortada(imagenId, RolUsuario.AGENTE, otroUsuarioId));

        assertFalse(imagen.getEsPortada());
        verify(imagenRepository, never()).desmarcarPortadaActual(any());
    }

    @Test
    void marcarPortada_cuandoYaEsPortada_noHaceNingunCambio() {
        UUID imagenId = UUID.randomUUID();
        PropiedadImagen imagen = crearImagen(imagenId, "propiedades/a.jpg", 0, true);
        when(imagenRepository.findById(imagenId)).thenReturn(Optional.of(imagen));
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);

        imagenService.marcarPortada(imagenId, RolUsuario.AGENTE, agenteId);

        assertTrue(imagen.getEsPortada());
        verify(imagenRepository, never()).desmarcarPortadaActual(any());
    }

    @Test
    void marcarPortada_cuandoNoEsPortada_desmarcaLaActualYMarcaEstaImagen() {
        UUID imagenId = UUID.randomUUID();
        PropiedadImagen imagen = crearImagen(imagenId, "propiedades/a.jpg", 1, false);
        when(imagenRepository.findById(imagenId)).thenReturn(Optional.of(imagen));
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);

        imagenService.marcarPortada(imagenId, RolUsuario.AGENTE, agenteId);

        verify(imagenRepository).desmarcarPortadaActual(propiedadId);
        assertTrue(imagen.getEsPortada());
    }

    @Test
    void marcarPortada_siendoAdministradorCentral_puedeMarcarPortadaDePropiedadesAjenas() {
        UUID imagenId = UUID.randomUUID();
        PropiedadImagen imagen = crearImagen(imagenId, "propiedades/a.jpg", 1, false);
        when(imagenRepository.findById(imagenId)).thenReturn(Optional.of(imagen));
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);

        imagenService.marcarPortada(imagenId, RolUsuario.ADMINISTRADOR_CENTRAL, otroUsuarioId);

        assertTrue(imagen.getEsPortada());
    }

    // ------------------------------------------------------------------
    // eliminar()
    // ------------------------------------------------------------------

    @Test
    void eliminar_cuandoLaImagenNoExiste_lanzaImagenNoEncontradaSinTocarR2() {
        UUID imagenId = UUID.randomUUID();
        when(imagenRepository.findById(imagenId)).thenReturn(Optional.empty());

        assertThrows(ImagenNoEncontradaException.class,
                () -> imagenService.eliminar(imagenId, RolUsuario.AGENTE, agenteId));

        verifyNoInteractions(s3Service);
    }

    @Test
    void eliminar_sinSerDuenoNiAdmin_lanzaAccesoNoAutorizadoYNoBorraNada() {
        UUID imagenId = UUID.randomUUID();
        PropiedadImagen imagen = crearImagen(imagenId, "propiedades/a.jpg", 0, false);
        when(imagenRepository.findById(imagenId)).thenReturn(Optional.of(imagen));
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);

        assertThrows(AccesoNoAutorizadoException.class,
                () -> imagenService.eliminar(imagenId, RolUsuario.AGENTE, otroUsuarioId));

        verifyNoInteractions(s3Service);
        verify(imagenRepository, never()).delete(any());
    }

    @Test
    void eliminar_siendoElDueno_borraPrimeroDeR2YLuegoDeLaBaseDeDatos() {
        UUID imagenId = UUID.randomUUID();
        PropiedadImagen imagen = crearImagen(imagenId, "propiedades/a.jpg", 0, false);
        when(imagenRepository.findById(imagenId)).thenReturn(Optional.of(imagen));
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);

        imagenService.eliminar(imagenId, RolUsuario.AGENTE, agenteId);

        InOrder orden = inOrder(s3Service, imagenRepository);
        orden.verify(s3Service).eliminarImagen("propiedades/a.jpg");
        orden.verify(imagenRepository).delete(imagen);
    }

    @Test
    void eliminar_siendoAdministradorCentral_puedeBorrarImagenesDePropiedadesAjenas() {
        UUID imagenId = UUID.randomUUID();
        PropiedadImagen imagen = crearImagen(imagenId, "propiedades/a.jpg", 0, false);
        when(imagenRepository.findById(imagenId)).thenReturn(Optional.of(imagen));
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);

        imagenService.eliminar(imagenId, RolUsuario.ADMINISTRADOR_CENTRAL, otroUsuarioId);

        verify(s3Service).eliminarImagen("propiedades/a.jpg");
        verify(imagenRepository).delete(imagen);
    }

    @Test
    void eliminar_cuandoFallaR2_propagaElErrorYNoBorraElRegistroDeLaBaseDeDatos() {
        UUID imagenId = UUID.randomUUID();
        PropiedadImagen imagen = crearImagen(imagenId, "propiedades/a.jpg", 0, false);
        when(imagenRepository.findById(imagenId)).thenReturn(Optional.of(imagen));
        when(propiedadService.buscarPorId(propiedadId)).thenReturn(propiedad);
        doThrow(new ErrorAlmacenamientoException("No se pudo eliminar la imagen de R2"))
                .when(s3Service).eliminarImagen("propiedades/a.jpg");

        assertThrows(ErrorAlmacenamientoException.class,
                () -> imagenService.eliminar(imagenId, RolUsuario.AGENTE, agenteId));

        verify(imagenRepository, never()).delete(any());
    }

    // ------------------------------------------------------------------
    // listarPorPropiedad()
    // ------------------------------------------------------------------

    @Test
    void listarPorPropiedad_devuelveLasImagenesConUrlCompletaYSuOrden() {
        PropiedadImagen primera = crearImagen(UUID.randomUUID(), "propiedades/a.jpg", 0, true);
        PropiedadImagen segunda = crearImagen(UUID.randomUUID(), "propiedades/b.jpg", 1, false);
        when(imagenRepository.findByPropiedadIdOrderByOrden(propiedadId)).thenReturn(List.of(primera, segunda));

        List<ImagenResponse> resultado = imagenService.listarPorPropiedad(propiedadId);

        assertEquals(2, resultado.size());
        assertEquals(URL_BASE + "propiedades/a.jpg", resultado.get(0).url());
        assertEquals(0, resultado.get(0).orden());
        assertTrue(resultado.get(0).esPortada());
        assertEquals(URL_BASE + "propiedades/b.jpg", resultado.get(1).url());
        assertEquals(1, resultado.get(1).orden());
        assertFalse(resultado.get(1).esPortada());
    }

    @Test
    void listarPorPropiedad_sinImagenes_devuelveListaVacia() {
        when(imagenRepository.findByPropiedadIdOrderByOrden(propiedadId)).thenReturn(List.of());

        List<ImagenResponse> resultado = imagenService.listarPorPropiedad(propiedadId);

        assertTrue(resultado.isEmpty());
    }
}