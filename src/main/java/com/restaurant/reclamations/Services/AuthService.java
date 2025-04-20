package com.restaurant.reclamations.Services;

import com.restaurant.reclamations.Clients.UserClient;
import com.restaurant.reclamations.DTO.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserClient userClient;
    
    /**
     * Valide un token JWT et retourne les informations de l'utilisateur s'il est valide
     * @param token Le token JWT (sans le préfixe "Bearer ")
     * @return Map contenant les informations de l'utilisateur ou null si le token est invalide
     */
    public Map<String, Object> validateToken(String token) {
        try {
            String authHeader = "Bearer " + token;
            Map<String, Object> userData = userClient.validateToken(authHeader);
            log.debug("Token validé : {}", userData);
            
            // Vérifier si le token est valide
            if (userData.containsKey("isValid") && Boolean.TRUE.equals(userData.get("isValid"))) {
                return userData;
            }
            return null;
        } catch (Exception e) {
            log.error("Erreur lors de la validation du token : {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Récupère le rôle de l'utilisateur à partir du token JWT
     * @param token Le token JWT (sans le préfixe "Bearer ")
     * @return Le rôle de l'utilisateur ou null si le token est invalide
     */
    public String getUserRole(String token) {
        try {
            String authHeader = "Bearer " + token;
            Map<String, Object> roleData = userClient.getTokenInfo(authHeader, "role");
            return (String) roleData.get("role");
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du rôle : {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Récupère l'ID de l'utilisateur à partir du token JWT
     * @param token Le token JWT (sans le préfixe "Bearer ")
     * @return L'ID de l'utilisateur ou null si le token est invalide
     */
    public String getUserId(String token) {
        try {
            String authHeader = "Bearer " + token;
            Map<String, Object> userData = userClient.getTokenInfo(authHeader, "userId");
            return (String) userData.get("userId");
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'ID utilisateur : {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Récupère l'utilisateur complet à partir du token JWT
     * @param token Le token JWT (sans le préfixe "Bearer ")
     * @return L'utilisateur ou null si le token est invalide
     */
    public UserDTO getCurrentUser(String token) {
        try {
            String authHeader = "Bearer " + token;
            return userClient.getCurrentUser(authHeader);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'utilisateur : {}", e.getMessage());
            return null;
        }
    }
}