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
     * Récupère les statistiques d'un joueur
     */
    @GetMapping("/{userId}")
    public ResponseEntity<PlayerStatistics> getPlayerStatistics(@PathVariable String userId) {
        try {
            PlayerStatistics stats = statisticsService.getPlayerStatistics(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération des statistiques: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
