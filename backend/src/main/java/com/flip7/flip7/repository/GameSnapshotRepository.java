package com.flip7.flip7.repository;

import com.flip7.flip7.entity.GameSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameSnapshotRepository extends JpaRepository<GameSnapshot, String> {
    
    /**
     * Trouve tous les snapshots de parties en cours (non terminées)
     */
    List<GameSnapshot> findByGameStatusIn(List<String> statuses);
}
