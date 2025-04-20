package com.restaurant.reclamations.Clients;

import com.restaurant.reclamations.DTO.CommandeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "commandes-service")
public interface CommandeClient {
    @GetMapping(value = "/commandes/{id}", produces = "application/json")
    CommandeDTO getCommandeByIdBasic(
        @PathVariable("id") String id, 
        @RequestHeader(value = "X-Client-Id", required = true) String clientId
    );

    @GetMapping(value = "/commandes/{id}", produces = "application/json")
    CommandeDTO getCommandeByIdWithAuth(
        @PathVariable("id") String id, 
        @RequestHeader(value = "X-Client-Id", required = true) String clientId,
        @RequestHeader(value = "Authorization", required = false) String authToken
    );

    /**
     * Helper method to call the appropriate API endpoint based on whether an auth token is available
     */
    default CommandeDTO getCommandeById(String id, String clientId, String authToken) {
        if (authToken != null) {
            return getCommandeByIdWithAuth(id, clientId, authToken);
        } else {
            return getCommandeByIdBasic(id, clientId);
        }
    }
    
    /**
     * Backwards compatibility method
     */
    default CommandeDTO getCommandeById(String id, String clientId) {
        return getCommandeByIdBasic(id, clientId);
    }
}
