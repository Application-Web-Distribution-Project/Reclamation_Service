package com.restaurant.reclamations.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserDTO {
    private String id; // Changé de Long à String pour correspondre au type dans User Service
    private String nom;
    private String prenom;
    private String email;
    private String role;
    
    // Pas de champ password pour des raisons de sécurité dans les DTOs
}
