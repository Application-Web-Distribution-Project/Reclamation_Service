package com.restaurant.reclamations.DTO;

import com.restaurant.reclamations.Entities.Reclamation;
import com.restaurant.reclamations.Entities.StatusReclamation;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReclamationDTO {
    private Long id;
    private Long userId;
    private String commandeId; // Modifié de Long à String pour correspondre à MongoDB
    private String description;
    private StatusReclamation status;
    private LocalDateTime dateCreation;
    private LocalDateTime dateResolution;

    private UserDTO user; // Transient - Récupéré via FeignClient
    private CommandeDTO commande; // Transient - Récupéré via FeignClient

    // Constructeur pour convertir une entité Reclamation en DTO
    public ReclamationDTO(Reclamation reclamation) {
        this.id = reclamation.getId();
        this.userId = reclamation.getUserId();
        this.commandeId = reclamation.getCommandeId();
        this.description = reclamation.getDescription();
        this.status = reclamation.getStatus();
        this.dateCreation = reclamation.getDateCreation();
        this.dateResolution = reclamation.getDateResolution();
    }
}
