package com.restaurant.reclamations.Repositories;

import com.restaurant.reclamations.Entities.Reclamation;
import com.restaurant.reclamations.Entities.StatusReclamation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {
    List<Reclamation> findByStatus(StatusReclamation status);
    Page<Reclamation> findByStatus(StatusReclamation status, Pageable pageable);
    
    // Recherche par texte dans la description
    Page<Reclamation> findByDescriptionContainingIgnoreCase(String keyword, Pageable pageable);
    
    // Recherche par date
    Page<Reclamation> findByDateCreationBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Recherche avancée
    @Query("SELECT r FROM Reclamation r WHERE " +
           "(:keyword IS NULL OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:startDate IS NULL OR r.dateCreation >= :startDate) AND " +
           "(:endDate IS NULL OR r.dateCreation <= :endDate)")
    Page<Reclamation> searchReclamations(
        @Param("keyword") String keyword,
        @Param("status") StatusReclamation status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    // Statistiques
    @Query("SELECT r.status as status, COUNT(r) as count FROM Reclamation r GROUP BY r.status")
    List<Object[]> getReclamationStats();
    
    // Méthode pour obtenir les statistiques sous forme de Map
    @SuppressWarnings("unchecked")
    default Map<StatusReclamation, Long> countByStatus() {
        return Map.of(
            StatusReclamation.EN_ATTENTE, count(StatusReclamation.EN_ATTENTE),
            StatusReclamation.EN_COURS, count(StatusReclamation.EN_COURS),
            StatusReclamation.RESOLU, count(StatusReclamation.RESOLU)
        );
    }
    
    // Méthode auxiliaire pour compter par statut
    default Long count(StatusReclamation status) {
        return countByStatus(status);
    }
    
    Long countByStatus(StatusReclamation status);
}


