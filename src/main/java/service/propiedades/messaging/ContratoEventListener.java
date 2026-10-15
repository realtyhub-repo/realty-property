package service.propiedades.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import service.propiedades.config.RabbitConfig;
import service.propiedades.dto.event.ContractCancelledEvent;
import service.propiedades.dto.event.ContractSignedEvent;
import service.propiedades.exception.PropiedadNoEncontradaException;
import service.propiedades.service.PropiedadService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContratoEventListener {

    private final PropiedadService propiedadService;

    @RabbitListener(queues = RabbitConfig.QUEUE_CONTRACT_SIGNED)
    public void alFirmarseContrato(ContractSignedEvent evento) {
        try {
            propiedadService.aplicarContratoFirmado(evento.propiedadId(), evento.tipo());
        } catch (PropiedadNoEncontradaException e) {
            log.error("contract.signed descartado, la propiedad {} no existe", evento.propiedadId());
        }
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_CONTRACT_CANCELLED)
    public void alCancelarseContrato(ContractCancelledEvent evento) {
        try {
            propiedadService.revertirPorContratoCancelado(evento.propiedadId());
        } catch (PropiedadNoEncontradaException e) {
            log.error("contract.cancelled descartado, la propiedad {} no existe", evento.propiedadId());
        }
    }
}
