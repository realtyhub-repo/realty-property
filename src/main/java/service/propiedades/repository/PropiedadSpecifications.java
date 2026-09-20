package service.propiedades.repository;

import org.springframework.data.jpa.domain.Specification;
import service.propiedades.entity.EstadoComercial;
import service.propiedades.entity.Modalidad;
import service.propiedades.entity.Propiedad;
import service.propiedades.entity.TipoPropiedad;

import java.math.BigDecimal;

public class PropiedadSpecifications {

    public static Specification<Propiedad> tienePropiedad(TipoPropiedad tipoPropiedad){
        return (root, query,cb)-> tipoPropiedad==null?null:cb.equal(root.get("tipoPropiedad"), tipoPropiedad);
    }

    public static Specification<Propiedad> tieneModalidad(Modalidad modalidad){
        return (root, query, cb) -> modalidad==null?null:cb.equal(root.get("modalidad"), modalidad);
    }

    public static Specification<Propiedad> tieneEstadoComercial(EstadoComercial estadoComercial){
        return (root, query,cb)-> estadoComercial==null?null:cb.equal(root.get("estadoComercial"),estadoComercial);
    }

    public static Specification<Propiedad> tieneCiudad(String ciudad){
        return (root,query,cb)->{
            if(ciudad==null||ciudad.isBlank()){
                return cb.conjunction();
            }

            String ciudadBuscar = "%"+ciudad.trim().toLowerCase()+"%";
            return cb.like(cb.lower(root.get("ciudad")),ciudadBuscar);
        };
    }

    public static Specification<Propiedad> tienePrecio(BigDecimal min, BigDecimal max){
        return (root, query, cb)->{

            if(min==null && max==null) return cb.conjunction();
            if(min==null) return cb.lessThanOrEqualTo(root.get("precio"),max);
            if(max==null) return cb.greaterThanOrEqualTo(root.get("precio"),min);
            return cb.between(root.get("precio"),min,max);

        };
    }
}
