CREATE TABLE propiedad_imagen (
                                  id uuid NOT NULL,
                                  created_at timestamp(6) without time zone NOT NULL,
                                  es_portada boolean NOT NULL,
                                  key_r2 character varying(255) NOT NULL,
                                  orden integer NOT NULL,
                                  propiedad_id uuid NOT NULL,
                                  CONSTRAINT propiedad_imagen_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_una_portada_por_propiedad
    ON propiedad_imagen (propiedad_id)
    WHERE es_portada = true;