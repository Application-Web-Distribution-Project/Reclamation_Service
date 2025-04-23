package com.restaurant.reclamations.DTO;

import com.restaurant.reclamations.Entities.Reclamation;
import com.restaurant.reclamations.Entities.StatusReclamation;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ReclamationDTO {
    private Long id;
    private String userId; // Modifié de Long à String pour correspondre au User Service
    private String commandeId;
    private String description;
    private String commentaire;
    private StatusReclamation status;
    private LocalDateTime dateCreation;
    private LocalDateTime dateResolution;
    private LocalDateTime dateMiseAJour;

    private UserDTO user; // Récupéré via FeignClient
    private CommandeDTO commande; // Récupéré via FeignClient

    // Constructeur pour convertir une entité Reclamation en DTO
    public ReclamationDTO(Reclamation reclamation) {
        if (reclamation != null) {
            this.id = reclamation.getId();
            this.userId = reclamation.getUserId();
            this.commandeId = reclamation.getCommandeId();
            this.description = reclamation.getDescription();
            this.commentaire = reclamation.getCommentaire();
            this.status = reclamation.getStatus();
            this.dateCreation = reclamation.getDateCreation();
            this.dateResolution = reclamation.getDateResolution();
            this.dateMiseAJour = reclamation.getDateMiseAJour();

            // Les objets user et commande seront chargés séparément via les clients Feign
        }
    }

    
}
