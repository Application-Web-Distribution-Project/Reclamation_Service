package com.restaurant.reclamations.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import com.restaurant.reclamations.DTO.CommandeDTO;
import com.restaurant.reclamations.DTO.UserDTO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Reclamation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String userId; // Modifié de Long à String pour correspondre au type d'ID dans User Service
    String commandeId; // String pour correspondre à MongoDB

    String description;
    String commentaire; // Ajout du champ commentaire

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "VARCHAR(12)")
    StatusReclamation status = StatusReclamation.EN_ATTENTE; // Changé EN_ATTENTE à OUVERT

    LocalDateTime dateCreation = LocalDateTime.now();
    LocalDateTime dateResolution;
    LocalDateTime dateMiseAJour; // Ajout du champ date de mise à jour

    @Transient
    CommandeDTO commande; // Récupéré via FeignClient

    @Transient
    @JsonIgnore
    UserDTO user; // Récupéré via FeignClient

    @OneToMany(mappedBy = "reclamation", cascade = CascadeType.ALL)
    private List<StatusHistory> statusHistory = new ArrayList<>();

   
}
