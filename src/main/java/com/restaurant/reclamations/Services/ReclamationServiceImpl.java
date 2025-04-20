package com.restaurant.reclamations.Services;

import com.restaurant.reclamations.DTO.ReclamationDTO;
import com.restaurant.reclamations.DTO.UserDTO;
import com.restaurant.reclamations.Entities.Reclamation;
import com.restaurant.reclamations.Repositories.ReclamationRepository;
import com.restaurant.reclamations.Clients.UserClient;
import com.restaurant.reclamations.Clients.CommandeClient;
import feign.FeignException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

import com.restaurant.reclamations.Entities.StatusHistory;
import com.restaurant.reclamations.Entities.StatusReclamation;
import com.restaurant.reclamations.Repositories.StatusHistoryRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReclamationServiceImpl implements ReclamationService {

    private final ReclamationRepository reclamationRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final UserClient userClient;
    private final CommandeClient commandeClient;
    private final NotificationService notificationService;

    @Override
    public ReclamationDTO createReclamation(ReclamationDTO reclamationDTO) {
        System.out.println("📩 Nouvelle réclamation reçue : " + reclamationDTO);

        // Récupérer l'ID utilisateur depuis le contexte de sécurité
        String userId = getCurrentUserId();
        log.info("Creating reclamation for authenticated user with ID: {}", userId);

        // Si aucun utilisateur n'est authentifié, vérifier si l'ID est fourni dans le DTO
        if (userId == null) {
            userId = reclamationDTO.getUserId();
            if (userId == null || userId.isEmpty()) {
                throw new RuntimeException("UserId invalide ! Aucun utilisateur authentifié ou spécifié dans la requête.");
            }
        } else {
            // Toujours définir l'ID utilisateur du contexte de sécurité dans le DTO
            reclamationDTO.setUserId(userId);
        }

        // Vérifier que la commande est spécifiée
        if (reclamationDTO.getCommandeId() == null || reclamationDTO.getCommandeId().isEmpty()) {
            throw new RuntimeException("CommandeId invalide !");
        }

        // Sauvegarde en base de données
        Reclamation reclamation = new Reclamation();
        reclamation.setUserId(userId);
        reclamation.setCommandeId(reclamationDTO.getCommandeId());
        reclamation.setDescription(reclamationDTO.getDescription());
        reclamation.setStatus(StatusReclamation.EN_ATTENTE); // Utiliser EN_ATTENTE comme statut initial
        reclamation.setDateCreation(LocalDateTime.now());

        reclamation = reclamationRepository.save(reclamation);

        // Retourner le DTO avec les détails de l'utilisateur
        ReclamationDTO responseDTO = new ReclamationDTO();
        responseDTO.setId(reclamation.getId());
        responseDTO.setUserId(reclamation.getUserId());
        responseDTO.setCommandeId(reclamation.getCommandeId());
        responseDTO.setDescription(reclamation.getDescription());
        responseDTO.setStatus(reclamation.getStatus());
        responseDTO.setDateCreation(reclamation.getDateCreation());

        try {
            // Récupérer les détails de l'utilisateur
            UserDTO user = userClient.getUserById(userId);
            responseDTO.setUser(user);
            log.info("User details retrieved: {}", user);
        } catch (Exception e) {
            log.error("Error retrieving user details: {}", e.getMessage());
        }

        return responseDTO;
    }

    // Méthode utilitaire pour récupérer l'ID utilisateur courant depuis le contexte de sécurité
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof String) {
                    log.debug("Retrieved user ID from security context: {}", principal);
                    return (String) principal;
                }
                log.warn("Principal is not a String but: {}", principal != null ? principal.getClass().getName() : "null");
            } else {
                log.warn("No authenticated user found in security context");
            }
        } catch (Exception e) {
            log.error("Error retrieving current user: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public List<ReclamationDTO> getAllReclamations() {
        return reclamationRepository.findAll().stream()
                .map(ReclamationDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public ReclamationDTO getReclamationById(Long id) {
        Reclamation reclamation = reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée"));

        ReclamationDTO dto = new ReclamationDTO(reclamation);

        try {
            System.out.println("🔍 [Feign] Récupération des infos user...");
            UserDTO user = userClient.getUserById(reclamation.getUserId());
            System.out.println("✅ [Feign] Utilisateur récupéré : " + user);
            dto.setUser(user);
            
            // Récupération des détails de la commande avec l'identifiant du service
            System.out.println("🔍 [Feign] Récupération des infos commande...");
            
            // Extract current request authorization token if available
            String authToken = extractAuthorizationToken();
            if (authToken != null) {
                log.debug("Authorization token found, forwarding to commandes-service");
                dto.setCommande(commandeClient.getCommandeByIdWithAuth(
                    reclamation.getCommandeId(), 
                    "reclamations-service",
                    "Bearer " + authToken
                ));
            } else {
                log.debug("No authorization token available, using only client ID");
                dto.setCommande(commandeClient.getCommandeByIdBasic(
                    reclamation.getCommandeId(), 
                    "reclamations-service"
                ));
            }
            
            System.out.println("✅ [Feign] Commande récupérée avec ID: " + reclamation.getCommandeId());
        } catch (FeignException e) {
            System.err.println("❌ [Feign] Erreur lors de l'appel aux services: " + e.getMessage());
            // Définit des objets vides pour éviter des erreurs
            if (dto.getUser() == null) dto.setUser(new UserDTO());
        }

        return dto;
    }
    
    /**
     * Extracts the JWT token from the current request's Authorization header
     * @return the JWT token without the "Bearer " prefix, or null if not found
     */
    private String extractAuthorizationToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    return authHeader.substring(7); // Remove "Bearer " prefix
                }
            }
        } catch (Exception e) {
            log.error("Error extracting authorization token: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public void deleteReclamation(Long id) {
        reclamationRepository.deleteById(id);
    }

    @Override
    public Page<ReclamationDTO> searchReclamations(String keyword, StatusReclamation status, 
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        // Log pour debug
        System.out.println("Recherche avec status: " + status);
        
        Page<Reclamation> reclamations = reclamationRepository.searchReclamations(
            keyword, status, startDate, endDate, pageable);
        
        // Log pour vérifier les résultats
        System.out.println("Nombre de résultats: " + reclamations.getTotalElements());
        reclamations.getContent().forEach(r -> 
            System.out.println("ID: " + r.getId() + ", Status: " + r.getStatus()));
            
        return reclamations.map(ReclamationDTO::new);
    }

    @Override
    public ReclamationDTO updateStatus(Long id, StatusReclamation newStatus, String comment) {
        log.info("Attempting to update status for reclamation {} to {} with comment: {}", id, newStatus, comment);
        
        Reclamation reclamation = reclamationRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Reclamation not found with ID: {}", id);
                return new RuntimeException("Réclamation non trouvée avec l'ID: " + id);
            });

        log.info("Found reclamation: {}", reclamation);

        // Create history entry
        StatusHistory history = new StatusHistory();
        history.setReclamation(reclamation);
        history.setOldStatus(StatusReclamation.valueOf(reclamation.getStatus().name()));
        history.setNewStatus(newStatus);
        history.setComment(comment);
        history.setChangeDate(LocalDateTime.now());

        // Update reclamation status
        reclamation.setStatus(newStatus);
        if (newStatus == StatusReclamation.RESOLU) {
            reclamation.setDateResolution(LocalDateTime.now());
        }

        try {
            // Save both entities
            statusHistoryRepository.save(history);
            reclamation = reclamationRepository.save(reclamation);
            
            // Attempt to notify user
            try {
                notifyUserStatusChange(id);
            } catch (Exception e) {
                log.warn("Failed to send notification, but status was updated: {}", e.getMessage());
            }

            log.info("Successfully updated status for reclamation {}", id);
            return new ReclamationDTO(reclamation);
        } catch (Exception e) {
            log.error("Failed to update status: {}", e.getMessage());
            throw new RuntimeException("Failed to update status: " + e.getMessage());
        }
    }

    @Override
    public Map<StatusReclamation, Long> getReclamationStats() {
        List<Object[]> stats = reclamationRepository.getReclamationStats();
        Map<StatusReclamation, Long> statsMap = new HashMap<>();
        
        for (Object[] stat : stats) {
            StatusReclamation status = (StatusReclamation) stat[0];
            Long count = (Long) stat[1];
            // Skip null keys to avoid serialization error
            if (status != null) {
                statsMap.put(status, count);
            } else {
                log.warn("Encountered null status in reclamation stats with count: {}", count);
            }
        }
        
        // Make sure all status values are represented (even with zero counts)
        for (StatusReclamation status : StatusReclamation.values()) {
            if (!statsMap.containsKey(status)) {
                statsMap.put(status, 0L);
            }
        }
        
        return statsMap;
    }

    @Override
    public void notifyUserStatusChange(Long reclamationId) {
        try {
            System.out.println("🔄 Début de la notification pour la réclamation #" + reclamationId);
            
            Reclamation reclamation = reclamationRepository.findById(reclamationId)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée"));
            
            // Obtenir les informations d'utilisateur pour récupérer son email
            try {
                // L'ID est déjà une chaîne, pas besoin de conversion
                UserDTO user = userClient.getUserById(reclamation.getUserId());
                if (user != null && user.getEmail() != null) {
                    // Utiliser l'email de l'utilisateur au lieu d'un email en dur
                    notificationService.sendStatusUpdateEmail(
                        user.getEmail(),
                        reclamationId,
                        reclamation.getStatus().toString(),
                        user.getNom() // Utiliser le nom de l'utilisateur comme nom du client
                    );
                    System.out.println("✅ Notification envoyée à " + user.getEmail() + " pour la réclamation " + reclamationId);
                } else {
                    System.err.println("❌ Email utilisateur non disponible pour userId: " + reclamation.getUserId());
                    // Fallback à l'email par défaut si l'email de l'utilisateur n'est pas disponible
                    notificationService.sendStatusUpdateEmail(
                        "aymenbog9@gmail.com", 
                        reclamationId,
                        reclamation.getStatus().toString(),
                        "Client"
                    );
                }
            } catch (Exception e) {
                System.err.println("❌ Impossible de récupérer l'utilisateur: " + e.getMessage());
                // Fallback à l'email par défaut en cas d'erreur
                notificationService.sendStatusUpdateEmail(
                    "aymenbog9@gmail.com",
                    reclamationId,
                    reclamation.getStatus().toString(),
                    "Client" 
                );
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la notification: " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de la notification", e);
        }
    }
}
