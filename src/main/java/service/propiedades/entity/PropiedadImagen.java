package service.propiedades.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Entity
@Table(name = "propiedad_imagen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PropiedadImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "propiedad_id")
    private UUID propiedadId;

    @Column(nullable = false, name = "key_r2")
    private String keyR2;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false, name = "es_portada")
    @Builder.Default
    private Boolean esPortada = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}