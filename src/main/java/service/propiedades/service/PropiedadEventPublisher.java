package service.propiedades.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import service.propiedades.config.RabbitConfig;
import service.propiedades.dto.event.PropiedadEstadoCambiadoEvent;
import service.propiedades.entity.EstadoComercial;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PropiedadEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publicarEstadoCambiado(UUID propiedadId, EstadoComercial anterior, EstadoComercial nuevo) {
        PropiedadEstadoCambiadoEvent evento =
                new PropiedadEstadoCambiadoEvent(propiedadId, anterior, nuevo, LocalDateTime.now());

        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_ESTADO_CAMBIADO, evento);
        } catch (AmqpException e) {
            log.error("No se pudo publicar propiedad.estado_cambiado de {}: {}", propiedadId, e.getMessage());
        }
    }
}
