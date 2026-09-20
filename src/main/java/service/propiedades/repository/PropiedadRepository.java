package service.propiedades.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import service.propiedades.entity.Propiedad;

import java.util.UUID;

public interface PropiedadRepository extends JpaRepository<Propiedad, UUID>, JpaSpecificationExecutor<Propiedad> {
}
