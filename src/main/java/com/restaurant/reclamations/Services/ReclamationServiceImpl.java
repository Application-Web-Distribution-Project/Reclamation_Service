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

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

// Add these imports
import com.restaurant.reclamations.Entities.StatusHistory;
import com.restaurant.reclamations.Entities.StatusReclamation;
import com.restaurant.reclamations.Repositories.StatusHistoryRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReclamationServiceImpl implements ReclamationService {

    private final ReclamationRepository reclamationRepository;
    private final StatusHistoryRepository statusHistoryRepository; // Add this
    private final UserClient userClient;
    private final CommandeClient commandeClient;
    private final NotificationService notificationService;

    @Override
    public ReclamationDTO createReclamation(ReclamationDTO reclamationDTO) {
        System.out.println("📩 Nouvelle réclamation reçue : " + reclamationDTO);

        if (reclamationDTO.getUserId() == null || reclamationDTO.getUserId() == 0) {
            throw new RuntimeException("UserId invalide !");
        }
        if (reclamationDTO.getCommandeId() == null || reclamationDTO.getCommandeId() == 0) {
            throw new RuntimeException("CommandeId invalide !");
        }

        // Sauvegarde en base de données
        Reclamation reclamation = new Reclamation();
        reclamation.setUserId(reclamationDTO.getUserId());
        reclamation.setCommandeId(reclamationDTO.getCommandeId());
        reclamation.setDescription(reclamationDTO.getDescription());
        reclamation.setStatus(reclamationDTO.getStatus());

        reclamation = reclamationRepository.save(reclamation);

        // 🔍 Récupération de l'utilisateur depuis le mock server
        try {
            UserDTO user = userClient.getUserById(reclamationDTO.getUserId());
            // Use the user object
            System.out.println("✅ User found: " + user);
            // Use commandeClient
            commandeClient.getCommandeById(reclamationDTO.getCommandeId());
        } catch (FeignException e) {
            System.err.println("❌ [Feign] Erreur lors de l'appel à UserClient : " + e.getMessage());
        }

        return new ReclamationDTO(reclamation);
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
        } catch (FeignException e) {
            System.err.println("❌ [Feign] Erreur lors de l'appel à UserClient : " + e.getMessage());
            dto.setUser(new UserDTO()); // Définit un objet vide pour éviter une erreur
        }

        return dto;
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
            statsMap.put(status, count);
        }
        
        return statsMap;
    }

    @Override
    public void notifyUserStatusChange(Long reclamationId) {
        try {
            System.out.println("🔄 Début de la notification pour la réclamation #" + reclamationId);
            
            Reclamation reclamation = reclamationRepository.findById(reclamationId)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée"));
            
            notificationService.sendStatusUpdateEmail(
                "aymenbog9@gmail.com",
                reclamationId,
                reclamation.getStatus().toString(),
                "Mise à jour de votre réclamation" // Ajout du commentaire manquant
            );
            
            System.out.println("✅ Notification envoyée pour la réclamation " + reclamationId);
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la notification: " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de la notification", e);
        }
    }
}
