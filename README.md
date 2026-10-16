# Realty Property

Realty Property es el microservicio encargado de administrar la información de los inmuebles registrados en RealtyHub. Su función es centralizar los datos de las propiedades para facilitar su consulta y gestión dentro de la plataforma.

## Funcionalidades

- Registro, consulta y actualización de propiedades (casas y apartamentos, en venta o alquiler).
- Gestión de imágenes de cada propiedad, almacenadas en Cloudflare R2.
- Control del estado comercial del inmueble (disponible, reservado, vendido o alquilado).
- Publica eventos cuando cambia el estado de una propiedad.

## Tecnologías

Java 21, Spring Boot, PostgreSQL, Flyway, Redis, RabbitMQ y Cloudflare R2.

## Puerto

El servicio se ejecuta en el puerto `8083`.
