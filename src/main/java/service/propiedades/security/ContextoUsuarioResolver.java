package service.propiedades.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import service.user.entity.RolUsuario;
import service.user.exception.ContextoUsuarioInvalidoException;
import service.user.security.ContextoUsuario;
import service.user.security.UsuarioActual;

import java.util.UUID;

public class ContextoUsuarioResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(UsuarioActual.class)
                && parameter.getParameterType().equals(ContextoUsuario.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        assert request != null;
        String idHeader = request.getHeader("X-User-Id");
        String rolHeader = request.getHeader("X-User-Role");

        if (idHeader == null || rolHeader == null) {
            throw new ContextoUsuarioInvalidoException("Faltan headers de autenticación");
        }

        try {
            UUID userId = UUID.fromString(idHeader);
            RolUsuario rol = RolUsuario.valueOf(rolHeader);
            return new ContextoUsuario(userId, rol);
        } catch (IllegalArgumentException e) {
            throw new ContextoUsuarioInvalidoException("Headers de autenticación inválidos");
        }
    }
}