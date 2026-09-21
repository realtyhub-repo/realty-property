package service.propiedades.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UploadUrlResponse(

        @NotBlank
        String key,

        @NotBlank
        String uploadUrl

) {
}
