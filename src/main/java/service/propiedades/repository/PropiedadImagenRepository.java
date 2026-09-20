package service.propiedades.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import service.propiedades.entity.PropiedadImagen;

import java.util.List;
import java.util.UUID;

public interface PropiedadImagenRepository extends JpaRepository<service.propiedades.entity.PropiedadImagen, UUID> {

    List<PropiedadImagen> findByPropiedadIdOrderByOrden(UUID id);
    List<PropiedadImagen> findByPropiedadIdInAndEsPortadaTrue(List<UUID> id);
}
