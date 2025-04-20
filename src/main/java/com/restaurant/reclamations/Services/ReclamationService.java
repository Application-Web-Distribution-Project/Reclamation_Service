package com.restaurant.reclamations.Services;

import com.restaurant.reclamations.DTO.ReclamationDTO;
import com.restaurant.reclamations.Entities.StatusReclamation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ReclamationService {
    ReclamationDTO createReclamation(ReclamationDTO reclamationDTO);
    List<ReclamationDTO> getAllReclamations();
    ReclamationDTO getReclamationById(Long id);
    void deleteReclamation(Long id);
    Page<ReclamationDTO> searchReclamations(String keyword, StatusReclamation status, 
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    ReclamationDTO updateStatus(Long id, StatusReclamation newStatus, String comment);
    Map<StatusReclamation, Long> getReclamationStats();
    void notifyUserStatusChange(Long reclamationId);
}
