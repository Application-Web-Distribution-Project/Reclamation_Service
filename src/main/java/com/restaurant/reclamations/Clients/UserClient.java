package com.restaurant.reclamations.Clients;

import com.restaurant.reclamations.DTO.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "users-service")
public interface UserClient {
    @GetMapping(value = "/users/{id}", produces = "application/json")
    UserDTO getUserById(@PathVariable("id") String id);
    
    @GetMapping("/users/validate-token")
    Map<String, Object> validateToken(@RequestHeader("Authorization") String authHeader);
    
    @GetMapping("/users/token-info")
    Map<String, Object> getTokenInfo(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String info);
    
    @GetMapping("/users/me")
    UserDTO getCurrentUser(@RequestHeader("Authorization") String authHeader);
}
