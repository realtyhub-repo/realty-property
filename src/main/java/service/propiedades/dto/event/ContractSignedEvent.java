package service.propiedades.dto.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContractSignedEvent(
        UUID contratoId,
        UUID leadId,
        UUID propiedadId,
        UUID clienteId,
        UUID agenteId,
        TipoContrato tipo,
        BigDecimal precioAcordado,
        LocalDateTime firmadoEn
) {
}
