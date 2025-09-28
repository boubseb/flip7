package com.flip7.server.repository;

import com.flip7.server.model.GameRoom;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RoomRepository extends MongoRepository<GameRoom, String> {
    Optional<GameRoom> findByName(String name);
}
