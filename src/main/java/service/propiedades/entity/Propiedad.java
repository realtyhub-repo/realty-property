package service.propiedades.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
@Entity
@Table(name = "propiedad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Propiedad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String descripcion;

    @Column(precision = 15, scale = 0, nullable = false)
    private BigDecimal precio;

    @Column(nullable = false)
    private String direccion;

    @Column(nullable = false)
    private String ciudad;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, name = "tipo_propiedad")
    private TipoPropiedad tipoPropiedad;


    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private Modalidad modalidad;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, name = "estado_comercial")
    @Builder.Default
    private EstadoComercial estadoComercial=EstadoComercial.DISPONIBLE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> caracteristicas;

    @Column(nullable = false, name = "agente_id")
    private UUID agenteId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt= LocalDateTime.now();
    }



}
