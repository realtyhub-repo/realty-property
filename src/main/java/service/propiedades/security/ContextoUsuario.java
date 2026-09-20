package service.propiedades.security;

import service.propiedades.dto.internal.RolUsuario;
import java.util.UUID;

public record ContextoUsuario(UUID userId, RolUsuario rol) {}