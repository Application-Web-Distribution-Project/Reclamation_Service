package com.restaurant.reclamations.config;

import feign.Logger;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL; // Active les logs détaillés pour mieux déboguer
    }

    @Bean
    public Decoder feignDecoder() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML)); // Accepte les réponses HTML

        return new SpringDecoder(() -> new HttpMessageConverters(converter));
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    @Component
    public class CustomErrorDecoder implements ErrorDecoder {

        @Override
        public Exception decode(String methodKey, Response response) {
            if (response.headers().get("Content-Type") != null && 
                response.headers().get("Content-Type").toString().contains("text/html")) {
                return new RuntimeException("Réponse HTML reçue au lieu de JSON. Vérifiez que le service User est correctement configuré.");
            }
            
            // Meilleure gestion des codes d'erreur
            switch (response.status()) {
                case 400:
                    return new RuntimeException("Requête Invalide");
                case 401:
                    return new RuntimeException("Non autorisé");
                case 403:
                    return new RuntimeException("Accès interdit");
                case 404:
                    return new RuntimeException("Ressource non trouvée");
                case 500:
                    return new RuntimeException("Erreur interne du serveur");
                default:
                    return new RuntimeException("Erreur de communication avec le service: " + 
                                                response.status() + " " + response.reason());
            }
        }
    }
}

