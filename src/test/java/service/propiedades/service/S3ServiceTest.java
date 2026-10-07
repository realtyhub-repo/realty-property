package service.propiedades.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import service.propiedades.exception.ErrorAlmacenamientoException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    private static final String BUCKET = "realtyhub-imagenes";

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Client s3Client;

    @Mock
    private PresignedPutObjectRequest presignedRequest;

    @InjectMocks
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        // El servicio lee R2_BUCKET_NAME con @Value; en un test unitario lo inyectamos a mano
        ReflectionTestUtils.setField(s3Service, "bucketName", BUCKET);
    }

    // ------------------------------------------------------------------
    // generarUrlPresigned()
    // ------------------------------------------------------------------

    @Test
    void generarUrlPresigned_devuelveLaUrlQueGeneraElPresigner() throws Exception {
        String urlFirmada = "https://r2.test/realtyhub-imagenes/propiedades/a.jpg?firma=abc123";
        when(presignedRequest.url()).thenReturn(URI.create(urlFirmada).toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        String resultado = s3Service.generarUrlPresigned("propiedades/a.jpg", Duration.ofMinutes(15), "image/jpeg");

        assertEquals(urlFirmada, resultado);
    }

    @Test
    void generarUrlPresigned_armaLaPeticionConBucketKeyYContentType() throws Exception {
        when(presignedRequest.url()).thenReturn(URI.create("https://r2.test/firmada").toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        s3Service.generarUrlPresigned("propiedades/123/foto.png", Duration.ofMinutes(15), "image/png");

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());

        PutObjectPresignRequest peticion = captor.getValue();
        assertEquals(BUCKET, peticion.putObjectRequest().bucket());
        assertEquals("propiedades/123/foto.png", peticion.putObjectRequest().key());
        assertEquals("image/png", peticion.putObjectRequest().contentType());
    }

    @ParameterizedTest
    @ValueSource(longs = {5, 15, 60})
    void generarUrlPresigned_usaLaDuracionDeExpiracionRecibida(long minutos) throws Exception {
        when(presignedRequest.url()).thenReturn(URI.create("https://r2.test/firmada").toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        s3Service.generarUrlPresigned("propiedades/a.jpg", Duration.ofMinutes(minutos), "image/jpeg");

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());

        assertEquals(Duration.ofMinutes(minutos), captor.getValue().signatureDuration());
    }

    // ------------------------------------------------------------------
    // eliminarImagen()
    // ------------------------------------------------------------------

    @Test
    void eliminarImagen_enviaAR2ElBucketYLaKeyCorrectos() {
        s3Service.eliminarImagen("propiedades/123/foto.jpg");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());

        assertEquals(BUCKET, captor.getValue().bucket());
        assertEquals("propiedades/123/foto.jpg", captor.getValue().key());
    }

    @Test
    void eliminarImagen_cuandoR2Falla_lanzaErrorAlmacenamientoConMensaje() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(SdkClientException.create("fallo de red"));

        ErrorAlmacenamientoException ex = assertThrows(ErrorAlmacenamientoException.class,
                () -> s3Service.eliminarImagen("propiedades/123/foto.jpg"));

        assertEquals("No se pudo eliminar la imagen de R2", ex.getMessage());
    }
}
