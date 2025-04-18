package com.restaurant.reclamations.Clients;

import com.restaurant.reclamations.DTO.CommandeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "commandes-service")
public interface CommandeClient {

    @GetMapping("/commandes/{id}")
    CommandeDTO getCommandeById(@PathVariable("id") String id);
}
