package service.propiedades.dto.internal;

import service.propiedades.entity.EstadoComercial;
import service.propiedades.entity.Modalidad;
import service.propiedades.entity.TipoPropiedad;

import java.math.BigDecimal;

public record PropiedadFiltros(
        TipoPropiedad tipoPropiedad,
        Modalidad modalidad,
        EstadoComercial estadoComercial,
        String ciudad,
        BigDecimal precioMin,
        BigDecimal precioMax
) {}
