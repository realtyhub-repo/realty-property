package service.propiedades.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import service.propiedades.entity.PropiedadImagen;

import java.util.List;
import java.util.UUID;

public interface PropiedadImagenRepository extends JpaRepository<service.propiedades.entity.PropiedadImagen, UUID> {

    List<PropiedadImagen> findByPropiedadIdOrderByOrden(UUID id);
    List<PropiedadImagen> findByPropiedadIdInAndEsPortadaTrue(List<UUID> id);

    @Modifying
    @Query("UPDATE PropiedadImagen p SET p.esPortada = false WHERE p.propiedadId = :propiedadId AND p.esPortada = true")
    void desmarcarPortadaActual(@Param("propiedadId") UUID propiedadId);

    @Query("SELECT COALESCE(MAX(p.orden), -1) FROM PropiedadImagen p WHERE p.propiedadId = :propiedadId")
    int obtenerOrdenMaximo(@Param("propiedadId") UUID propiedadId);

}
