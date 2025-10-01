package com.flip7.flip7.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flip7.flip7.entity.Room;
import com.flip7.flip7.entity.Room.RoomStatus;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {
    List<Room> findByStatus(RoomStatus status);
    List<Room> findByAdminId(String adminId);
    Optional<Room> findByIdAndPassword(String id, String password);
}
