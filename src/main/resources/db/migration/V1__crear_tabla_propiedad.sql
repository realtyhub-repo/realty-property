CREATE TABLE propiedad (
                           id uuid NOT NULL,
                           agente_id uuid NOT NULL,
                           caracteristicas jsonb NOT NULL,
                           ciudad character varying(255) NOT NULL,
                           created_at timestamp(6) without time zone NOT NULL,
                           descripcion character varying(255) NOT NULL,
                           direccion character varying(255) NOT NULL,
                           estado_comercial character varying(255) NOT NULL,
                           modalidad character varying(255) NOT NULL,
                           precio numeric(15,0) NOT NULL,
                           tipo_propiedad character varying(255) NOT NULL,
                           titulo character varying(255) NOT NULL,
                           updated_at timestamp(6) without time zone,
                           CONSTRAINT propiedad_pkey PRIMARY KEY (id),
                           CONSTRAINT propiedad_estado_comercial_check CHECK (estado_comercial IN ('DISPONIBLE', 'RESERVADO', 'VENDIDO', 'ALQUILADO')),
                           CONSTRAINT propiedad_modalidad_check CHECK (modalidad IN ('VENTA', 'ALQUILER')),
                           CONSTRAINT propiedad_tipo_propiedad_check CHECK (tipo_propiedad IN ('CASA', 'APARTAMENTO'))
);