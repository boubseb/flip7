package com.flip7.flip7.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flip7.flip7.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByPseudo(String pseudo);
}

