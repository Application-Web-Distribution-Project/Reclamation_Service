package com.restaurant.reclamations.DTO;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommandeDTO {
    private String id;  // Changé de Long à String pour correspondre à MongoDB
    private LocalDateTime dateCommande;
    private String status;
    private String userId;
    private List<String> menuIds;
}
