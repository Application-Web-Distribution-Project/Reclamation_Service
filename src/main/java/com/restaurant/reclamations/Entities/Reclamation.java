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

    Long userId; // Stocke uniquement l'ID de l'utilisateur (microservice User)
    String commandeId; // Modifié de Long à String pour correspondre à MongoDB

    String description;

    @Enumerated(EnumType.STRING)
    StatusReclamation status = StatusReclamation.EN_ATTENTE;

    LocalDateTime dateCreation = LocalDateTime.now();
    LocalDateTime dateResolution;

    @Transient
    CommandeDTO commande; // Récupéré via FeignClient

    @Transient
    @JsonIgnore
    UserDTO user; // Récupéré via FeignClient

    @OneToMany(mappedBy = "reclamation", cascade = CascadeType.ALL)
    private List<StatusHistory> statusHistory = new ArrayList<>();
}
