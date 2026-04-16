package com.flip7.flip7.controller;

import com.flip7.flip7.dto.PlayerStatistics;
import com.flip7.flip7.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    /**
     * Récupère les statistiques d'un joueur avec filtres optionnels.
     * @param persistentDeck  null = all, true = only persistent deck, false = only normal
     * @param statsEnabled    null = all, true = only stats-enabled, false = only without
     * @param completedOnly   null = all, true = only completed, false = only non-completed
     */
    @GetMapping("/{userId}")
    public ResponseEntity<PlayerStatistics> getPlayerStatistics(
            @PathVariable String userId,
            @RequestParam(required = false) Boolean persistentDeck,
            @RequestParam(required = false) Boolean statsEnabled,
            @RequestParam(required = false) Boolean completedOnly) {
        try {
            PlayerStatistics stats = statisticsService.getPlayerStatistics(
                userId, persistentDeck, statsEnabled, completedOnly);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération des statistiques: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
