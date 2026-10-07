package service.propiedades.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import service.propiedades.dto.internal.PropiedadFiltros;
import service.propiedades.dto.internal.RolUsuario;
import service.propiedades.dto.request.ActualizarPropiedadRequest;
import service.propiedades.dto.request.CrearPropiedadRequest;
import service.propiedades.dto.response.PropiedadCoordenadasResponse;
import service.propiedades.dto.response.PropiedadDetalleResponse;
import service.propiedades.dto.response.PropiedadResponse;
import service.propiedades.dto.response.UsuarioInternalResponse;
import service.propiedades.entity.EstadoComercial;
import service.propiedades.entity.Modalidad;
import service.propiedades.entity.Propiedad;
import service.propiedades.entity.PropiedadImagen;
import service.propiedades.entity.TipoPropiedad;
import service.propiedades.exception.AccesoNoAutorizadoException;
import service.propiedades.exception.PropiedadNoEncontradaException;
import service.propiedades.repository.PropiedadImagenRepository;
import service.propiedades.repository.PropiedadRepository;
import service.propiedades.security.ContextoUsuario;

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
class PropiedadServiceTest {

    private static final String URL_BASE = "https://cdn.realtyhub.test/";

    @Mock
    private PropiedadRepository propiedadRepository;

    @Mock
    private UsuarioClientService usuarioClientService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private PropiedadImagenRepository propiedadImagenRepository;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletRequest servletRequest;

    @InjectMocks
    private PropiedadService propiedadService;

    private UUID propiedadId;
    private UUID agenteId;
    private UUID otroUsuarioId;

    @BeforeEach
    void setUp() {
        // El servicio lee R2_URL con @Value; en un test unitario no hay Spring, así que lo inyectamos a mano
        ReflectionTestUtils.setField(propiedadService, "urlBase", URL_BASE);

        propiedadId = UUID.randomUUID();
        agenteId = UUID.randomUUID();
        otroUsuarioId = UUID.randomUUID();
    }

    // ------------------------------------------------------------------
    // Datos de apoyo
    // ------------------------------------------------------------------

    private Propiedad crearPropiedad(UUID id, UUID duenoId) {
        return Propiedad.builder()
                .id(id)
                .titulo("Casa en el centro")
                .descripcion("Casa amplia y bien ubicada")
                .precio(new BigDecimal("350000000"))
                .direccion("Calle 10 # 5-20")
                .ciudad("Monteria")
                .latitud(8.75)
                .longitud(-75.88)
                .tipoPropiedad(TipoPropiedad.CASA)
                .modalidad(Modalidad.VENTA)
                .estadoComercial(EstadoComercial.DISPONIBLE)
                .caracteristicas(Map.of("habitaciones", 3))
                .agenteId(duenoId)
                .build();
    }

    private CrearPropiedadRequest crearRequestValido() {
        return CrearPropiedadRequest.builder()
                .titulo("Apartamento nuevo")
                .descripcion("Apartamento con vista")
                .precio(new BigDecimal("220000000"))
                .direccion("Carrera 5 # 12-30")
                .ciudad("Cali")
                .latitud(3.45)
                .longitud(-76.53)
                .tipoPropiedad(TipoPropiedad.APARTAMENTO)
                .modalidad(Modalidad.ALQUILER)
                .caracteristicas(Map.of("banos", 2))
                .build();
    }

    // ------------------------------------------------------------------
    // crear()
    // ------------------------------------------------------------------

    @Test
    void crear_conRolCliente_lanzaAccesoNoAutorizadoYNoGuarda() {
        assertThrows(AccesoNoAutorizadoException.class,
                () -> propiedadService.crear(RolUsuario.CLIENTE, otroUsuarioId, crearRequestValido()));

        verify(propiedadRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = RolUsuario.class, names = {"AGENTE", "GERENTE_OFICINA", "ADMINISTRADOR_CENTRAL"})
    void crear_conRolesPermitidos_guardaPropiedadDisponibleConElAgenteSolicitante(RolUsuario rol) {
        when(propiedadRepository.save(any(Propiedad.class))).thenAnswer(inv -> inv.getArgument(0));

        PropiedadResponse response = propiedadService.crear(rol, agenteId, crearRequestValido());

        ArgumentCaptor<Propiedad> captor = ArgumentCaptor.forClass(Propiedad.class);
        verify(propiedadRepository).save(captor.capture());
        Propiedad guardada = captor.getValue();

        assertEquals("Apartamento nuevo", guardada.getTitulo());
        assertEquals(EstadoComercial.DISPONIBLE, guardada.getEstadoComercial());
        assertEquals(agenteId, guardada.getAgenteId());
        assertEquals(TipoPropiedad.APARTAMENTO, guardada.getTipoPropiedad());

        assertEquals("Apartamento nuevo", response.titulo());
        assertEquals("Cali", response.ciudad());
        assertEquals(EstadoComercial.DISPONIBLE, response.estadoComercial());
        assertNull(response.urlPortada());
    }

    // ------------------------------------------------------------------
    // obtenerDetalle()
    // ------------------------------------------------------------------

    @Test
    void obtenerDetalle_cuandoElUsuarioEsElPropietario_noRegistraVistaYDevuelveDetalle() {
        Propiedad propiedad = crearPropiedad(propiedadId, agenteId);
        PropiedadImagen imagen = PropiedadImagen.builder()
                .id(UUID.randomUUID())
                .propiedadId(propiedadId)
                .keyR2("propiedades/casa-1.jpg")
                .orden(0)
                .esPortada(true)
                .build();

        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedad));
        when(propiedadImagenRepository.findByPropiedadIdOrderByOrden(propiedadId)).thenReturn(List.of(imagen));
        when(usuarioClientService.buscarAgente(agenteId)).thenReturn(new UsuarioInternalResponse(agenteId, "Carlos Perez"));

        ContextoUsuario propietario = new ContextoUsuario(agenteId, RolUsuario.AGENTE);

        PropiedadDetalleResponse detalle = propiedadService.obtenerDetalle(propiedadId, propietario, servletRequest);

        assertEquals("Carlos Perez", detalle.nombreAgente());
        assertEquals(agenteId, detalle.agenteId());
        assertEquals(1, detalle.imagenes().size());
        assertEquals(URL_BASE + "propiedades/casa-1.jpg", detalle.imagenes().get(0).url());
        assertTrue(detalle.imagenes().get(0).esPortada());
        // El propietario no debe sumar vistas a su propia propiedad
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void obtenerDetalle_usuarioAutenticadoQueNoEsPropietario_registraVistaConSuUserId() {
        Propiedad propiedad = crearPropiedad(propiedadId, agenteId);
        ContextoUsuario visitante = new ContextoUsuario(otroUsuarioId, RolUsuario.CLIENTE);
        String claveEsperada = "vista:" + propiedadId + ":" + otroUsuarioId;

        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedad));
        when(usuarioClientService.buscarAgente(agenteId)).thenReturn(new UsuarioInternalResponse(agenteId, "Carlos Perez"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(claveEsperada), anyString(), eq(Duration.ofMinutes(30))))
                .thenReturn(true);

        PropiedadDetalleResponse detalle = propiedadService.obtenerDetalle(propiedadId, visitante, servletRequest);

        assertNotNull(detalle);
        verify(valueOperations).setIfAbsent(eq(claveEsperada), anyString(), eq(Duration.ofMinutes(30)));
    }

    @Test
    void obtenerDetalle_usuarioAnonimo_registraVistaConLaIpDelRequest() {
        Propiedad propiedad = crearPropiedad(propiedadId, agenteId);
        String claveEsperada = "vista:" + propiedadId + ":192.168.1.10";

        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedad));
        when(usuarioClientService.buscarAgente(agenteId)).thenReturn(new UsuarioInternalResponse(agenteId, "Carlos Perez"));
        when(servletRequest.getRemoteAddr()).thenReturn("192.168.1.10");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(claveEsperada), anyString(), eq(Duration.ofMinutes(30))))
                .thenReturn(true);

        PropiedadDetalleResponse detalle = propiedadService.obtenerDetalle(propiedadId, null, servletRequest);

        assertNotNull(detalle);
        verify(valueOperations).setIfAbsent(eq(claveEsperada), anyString(), eq(Duration.ofMinutes(30)));
    }

    @Test
    void obtenerDetalle_cuandoLaVistaYaSeRegistroRecientemente_aunAsiDevuelveElDetalle() {
        Propiedad propiedad = crearPropiedad(propiedadId, agenteId);
        ContextoUsuario visitante = new ContextoUsuario(otroUsuarioId, RolUsuario.CLIENTE);

        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedad));
        when(usuarioClientService.buscarAgente(agenteId)).thenReturn(new UsuarioInternalResponse(agenteId, "Carlos Perez"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // false = la clave ya existía en Redis (vista repetida dentro de los 30 minutos)
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        PropiedadDetalleResponse detalle = propiedadService.obtenerDetalle(propiedadId, visitante, servletRequest);

        assertEquals(propiedadId, detalle.id());
    }

    @Test
    void obtenerDetalle_cuandoLaPropiedadNoExiste_lanzaExcepcionYNoTocaRedisNiUsuarios() {
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.empty());

        assertThrows(PropiedadNoEncontradaException.class,
                () -> propiedadService.obtenerDetalle(propiedadId, null, servletRequest));

        verifyNoInteractions(redisTemplate, usuarioClientService);
    }

    // ------------------------------------------------------------------
    // listar()
    // ------------------------------------------------------------------

    @Test
    void listar_conPortadas_asignaUrlSoloALasPropiedadesQueTienenPortada() {
        Pageable pageable = PageRequest.of(0, 10);
        UUID idConPortada = UUID.randomUUID();
        UUID idSinPortada = UUID.randomUUID();
        Propiedad conPortada = crearPropiedad(idConPortada, agenteId);
        Propiedad sinPortada = crearPropiedad(idSinPortada, agenteId);

        PropiedadImagen portada = PropiedadImagen.builder()
                .propiedadId(idConPortada)
                .keyR2("propiedades/portada.jpg")
                .orden(0)
                .esPortada(true)
                .build();

        when(propiedadRepository.findAll(ArgumentMatchers.<Specification<Propiedad>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(conPortada, sinPortada), pageable, 2));
        when(propiedadImagenRepository.findByPropiedadIdInAndEsPortadaTrue(anyList()))
                .thenReturn(List.of(portada));

        PropiedadFiltros filtros = new PropiedadFiltros(null, null, null, null, null, null);

        Page<PropiedadResponse> resultado = propiedadService.listar(pageable, filtros);

        assertEquals(2, resultado.getTotalElements());
        assertEquals(2, resultado.getContent().size());

        PropiedadResponse primera = resultado.getContent().get(0);
        assertEquals(idConPortada, primera.id());
        assertEquals(URL_BASE + "propiedades/portada.jpg", primera.urlPortada());

        PropiedadResponse segunda = resultado.getContent().get(1);
        assertEquals(idSinPortada, segunda.id());
        assertNull(segunda.urlPortada());
    }

    @Test
    void listar_sinResultados_devuelvePaginaVacia() {
        Pageable pageable = PageRequest.of(0, 10);

        when(propiedadRepository.findAll(ArgumentMatchers.<Specification<Propiedad>>any(), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        PropiedadFiltros filtros = new PropiedadFiltros(TipoPropiedad.CASA, Modalidad.VENTA,
                EstadoComercial.DISPONIBLE, "Cali", new BigDecimal("100"), new BigDecimal("500"));

        Page<PropiedadResponse> resultado = propiedadService.listar(pageable, filtros);

        assertTrue(resultado.isEmpty());
        assertEquals(0, resultado.getTotalElements());
    }

    // ------------------------------------------------------------------
    // actualizar()
    // ------------------------------------------------------------------

    @Test
    void actualizar_siendoElDueno_modificaSoloLosCamposQueNoVienenNulos() {
        Propiedad propiedad = crearPropiedad(propiedadId, agenteId);
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedad));

        ActualizarPropiedadRequest request = ActualizarPropiedadRequest.builder()
                .titulo("Casa remodelada")
                .latitud(9.1)
                .longitud(-75.5)
                .build();

        PropiedadResponse response = propiedadService.actualizar(propiedadId, RolUsuario.AGENTE, agenteId, request);

        // Campos enviados: cambian
        assertEquals("Casa remodelada", propiedad.getTitulo());
        assertEquals(9.1, propiedad.getLatitud());
        assertEquals(-75.5, propiedad.getLongitud());
        // Campos no enviados: se conservan
        assertEquals(new BigDecimal("350000000"), propiedad.getPrecio());
        assertEquals("Monteria", propiedad.getCiudad());
        assertEquals("Calle 10 # 5-20", propiedad.getDireccion());
        assertEquals("Casa amplia y bien ubicada", propiedad.getDescripcion());

        verify(propiedadRepository).save(propiedad);
        assertEquals("Casa remodelada", response.titulo());
    }

    @Test
    void actualizar_conTodosLosCampos_losReemplazaTodos() {
        Propiedad propiedad = crearPropiedad(propiedadId, agenteId);
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedad));

        ActualizarPropiedadRequest request = ActualizarPropiedadRequest.builder()
                .titulo("Titulo nuevo")
                .descripcion("Descripcion nueva")
                .precio(new BigDecimal("400000000"))
                .direccion("Avenida 1 # 2-3")
                .ciudad("Bogota")
                .latitud(4.71)
                .longitud(-74.07)
                .caracteristicas(Map.of("parqueadero", true))
                .build();

        propiedadService.actualizar(propiedadId, RolUsuario.AGENTE, agenteId, request);

        assertEquals("Titulo nuevo", propiedad.getTitulo());
        assertEquals("Descripcion nueva", propiedad.getDescripcion());
        assertEquals(new BigDecimal("400000000"), propiedad.getPrecio());
        assertEquals("Avenida 1 # 2-3", propiedad.getDireccion());
        assertEquals("Bogota", propiedad.getCiudad());
        assertEquals(4.71, propiedad.getLatitud());
        assertEquals(-74.07, propiedad.getLongitud());
        assertEquals(Map.of("parqueadero", true), propiedad.getCaracteristicas());
    }

    @Test
    void actualizar_siendoAdministradorCentral_puedeEditarPropiedadesAjenas() {
        Propiedad propiedad = crearPropiedad(propiedadId, agenteId);
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedad));

        ActualizarPropiedadRequest request = ActualizarPropiedadRequest.builder()
                .titulo("Editada por el admin")
                .build();

        propiedadService.actualizar(propiedadId, RolUsuario.ADMINISTRADOR_CENTRAL, otroUsuarioId, request);

        assertEquals("Editada por el admin", propiedad.getTitulo());
        verify(propiedadRepository).save(propiedad);
    }

    @Test
    void actualizar_sinSerDuenoNiAdmin_lanzaAccesoNoAutorizadoYNoGuarda() {
        Propiedad propiedad = crearPropiedad(propiedadId, agenteId);
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedad));

        ActualizarPropiedadRequest request = ActualizarPropiedadRequest.builder()
                .titulo("Intento de otro agente")
                .build();

        assertThrows(AccesoNoAutorizadoException.class,
                () -> propiedadService.actualizar(propiedadId, RolUsuario.AGENTE, otroUsuarioId, request));

        assertEquals("Casa en el centro", propiedad.getTitulo());
        verify(propiedadRepository, never()).save(any());
    }

    @Test
    void actualizar_cuandoLaPropiedadNoExiste_lanzaPropiedadNoEncontrada() {
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.empty());

        ActualizarPropiedadRequest request = ActualizarPropiedadRequest.builder().titulo("X").build();

        assertThrows(PropiedadNoEncontradaException.class,
                () -> propiedadService.actualizar(propiedadId, RolUsuario.AGENTE, agenteId, request));

        verify(propiedadRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // cambiarEstadoManual()
    // ------------------------------------------------------------------

    @Test
    void cambiarEstadoManual_conRolCliente_lanzaAccesoNoAutorizadoSinConsultarLaBaseDeDatos() {
        assertThrows(AccesoNoAutorizadoException.class,
                () -> propiedadService.cambiarEstadoManual(propiedadId, EstadoComercial.VENDIDO,
                        RolUsuario.CLIENTE, agenteId));

        verifyNoInteractions(propiedadRepository);
    }

    @Test
    void cambiarEstadoManual_siendoElDueno_cambiaElEstadoYGuarda() {
        Propiedad propiedad = crearPropiedad(propiedadId, agenteId);
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedad));

        propiedadService.cambiarEstadoManual(propiedadId, EstadoComercial.RESERVADO, RolUsuario.AGENTE, agenteId);

        assertEquals(EstadoComercial.RESERVADO, propiedad.getEstadoComercial());
        verify(propiedadRepository).save(propiedad);
    }

    @Test
    void cambiarEstadoManual_siendoAdministradorCentral_puedeCambiarElEstadoDePropiedadesAjenas() {
        Propiedad propiedad = crearPropiedad(propiedadId, agenteId);
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedad));

        propiedadService.cambiarEstadoManual(propiedadId, EstadoComercial.VENDIDO,
                RolUsuario.ADMINISTRADOR_CENTRAL, otroUsuarioId);

        assertEquals(EstadoComercial.VENDIDO, propiedad.getEstadoComercial());
        verify(propiedadRepository).save(propiedad);
    }

    @Test
    void cambiarEstadoManual_sinSerDuenoNiAdmin_lanzaAccesoNoAutorizadoYNoGuarda() {
        Propiedad propiedad = crearPropiedad(propiedadId, agenteId);
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedad));

        assertThrows(AccesoNoAutorizadoException.class,
                () -> propiedadService.cambiarEstadoManual(propiedadId, EstadoComercial.VENDIDO,
                        RolUsuario.AGENTE, otroUsuarioId));

        assertEquals(EstadoComercial.DISPONIBLE, propiedad.getEstadoComercial());
        verify(propiedadRepository, never()).save(any());
    }

    @Test
    void cambiarEstadoManual_cuandoLaPropiedadNoExiste_lanzaPropiedadNoEncontrada() {
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.empty());

        assertThrows(PropiedadNoEncontradaException.class,
                () -> propiedadService.cambiarEstadoManual(propiedadId, EstadoComercial.VENDIDO,
                        RolUsuario.AGENTE, agenteId));
    }

    // ------------------------------------------------------------------
    // cambiarEstado()  (lo usan otros servicios, sin validar permisos)
    // ------------------------------------------------------------------

    @Test
    void cambiarEstado_actualizaElEstadoYGuarda() {
        Propiedad propiedad = crearPropiedad(propiedadId, agenteId);
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedad));

        propiedadService.cambiarEstado(propiedadId, EstadoComercial.ALQUILADO);

        assertEquals(EstadoComercial.ALQUILADO, propiedad.getEstadoComercial());
        verify(propiedadRepository).save(propiedad);
    }

    @Test
    void cambiarEstado_cuandoLaPropiedadNoExiste_lanzaPropiedadNoEncontrada() {
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.empty());

        assertThrows(PropiedadNoEncontradaException.class,
                () -> propiedadService.cambiarEstado(propiedadId, EstadoComercial.VENDIDO));

        verify(propiedadRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // buscarPorId()
    // ------------------------------------------------------------------

    @Test
    void buscarPorId_cuandoExiste_devuelveLaPropiedad() {
        Propiedad propiedad = crearPropiedad(propiedadId, agenteId);
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedad));

        Propiedad resultado = propiedadService.buscarPorId(propiedadId);

        assertSame(propiedad, resultado);
    }

    @Test
    void buscarPorId_cuandoNoExiste_lanzaExcepcionConMensaje() {
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.empty());

        PropiedadNoEncontradaException ex = assertThrows(PropiedadNoEncontradaException.class,
                () -> propiedadService.buscarPorId(propiedadId));

        assertEquals("Propiedad no encontrada", ex.getMessage());
    }

    // ------------------------------------------------------------------
    // encontrarCoordenadas()
    // ------------------------------------------------------------------

    @Test
    void encontrarCoordenadas_cuandoExiste_devuelveEstadoLatitudYLongitud() {
        Propiedad propiedad = crearPropiedad(propiedadId, agenteId);
        propiedad.setEstadoComercial(EstadoComercial.RESERVADO);
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedad));

        PropiedadCoordenadasResponse response = propiedadService.encontrarCoordenadas(propiedadId);

        assertEquals(EstadoComercial.RESERVADO, response.estadoComercial());
        assertEquals(8.75, response.latitud());
        assertEquals(-75.88, response.longitud());
    }

    @Test
    void encontrarCoordenadas_cuandoNoExiste_lanzaPropiedadNoEncontrada() {
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.empty());

        assertThrows(PropiedadNoEncontradaException.class,
                () -> propiedadService.encontrarCoordenadas(propiedadId));
    }
}