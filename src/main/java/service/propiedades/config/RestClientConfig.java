package service.propiedades.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(@Value("${servicios.usuarios-url}") String usuariosUrl){
        return RestClient.builder()
                .baseUrl(usuariosUrl)
                .build();
    }


}
