package com.flip7.flip7.service;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.flip7.flip7.dto.UserAdminDTO;
import com.flip7.flip7.entity.User;
import com.flip7.flip7.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void initSuperAdmin() {
        User bbsb = userRepository.findByPseudo("bbsb");
        if (bbsb != null && !"SUPERADMIN".equals(bbsb.getRole())) {
            bbsb.setRole("SUPERADMIN");
            userRepository.save(bbsb);
        }
    }

    // ─── Role helpers ─────────────────────────────────────────────────────────

    public boolean isAdmin(String userId) {
        try {
            User u = getUserById(userId);
            return "ADMIN".equals(u.getRole()) || "SUPERADMIN".equals(u.getRole());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isSuperAdmin(String userId) {
        try {
            User u = getUserById(userId);
            return "SUPERADMIN".equals(u.getRole());
        } catch (Exception e) {
            return false;
        }
    }

    // ─── Admin operations ─────────────────────────────────────────────────────

    public List<UserAdminDTO> getAllUsersForAdmin() {
        return userRepository.findAll().stream()
            .map(u -> new UserAdminDTO(u.getId(), u.getPseudo(), u.getEmail(), u.getRole()))
            .collect(Collectors.toList());
    }

    public void updateRole(String targetId, String newRole, String requesterId) {
        if (!isSuperAdmin(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé au super-admin");
        }
        if (!"USER".equals(newRole) && !"ADMIN".equals(newRole) && !"SUPERADMIN".equals(newRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle invalide");
        }
        User target = getUserById(targetId);
        target.setRole(newRole);
        userRepository.save(target);
    }

    public void adminResetPassword(String targetId, String newPassword, String requesterId) {
        if (!isAdmin(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe invalide");
        }
        User target = getUserById(targetId);
        target.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(target);
    }

    public void adminDeleteUser(String targetId, String requesterId) {
        if (!isSuperAdmin(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé au super-admin");
        }
        if (targetId.equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de se supprimer soi-même");
        }
        userRepository.deleteById(targetId);
    }

    public List<User> getAll() {
        List<User> users = this.userRepository.findAll();
        return users;
    }

    public User getUserById(String id) {
        return this.userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public void save(User user) {
        // Hash the password before persisting (BCrypt includes its own salt)
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        this.userRepository.save(user);
    }

    /**
     * Vérifie les identifiants (utilisé par le login).
     * Retourne l'utilisateur si les identifiants sont valides, null sinon.
     */
    public User checkCredentials(String pseudo, String rawPassword) {
        User user = this.userRepository.findByPseudo(pseudo);
        if (user == null) return null;
        // Supports both BCrypt-hashed and legacy plaintext passwords
        boolean matches;
        if (user.getPassword() != null && (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$"))) {
            matches = passwordEncoder.matches(rawPassword, user.getPassword());
        } else {
            // Legacy plaintext — accept the login and migrate the hash on the fly
            matches = rawPassword.equals(user.getPassword());
            if (matches) {
                user.setPassword(passwordEncoder.encode(rawPassword));
                this.userRepository.save(user);
            }
        }
        return matches ? user : null;
    }

    public String delete(String userUUID, String password) {
        User user = this.getUserById(userUUID);
        boolean valid;
        if (user.getPassword() != null && (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$"))) {
            valid = passwordEncoder.matches(password, user.getPassword());
        } else {
            valid = user.getPassword().equals(password);
        }
        if (valid) {
            this.userRepository.delete(user);
            return "User deleted";
        } else {
            return "Wrong password";
        }
    }

    public User findById(String id) {
        return this.userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public User findByPseudo(String pseudo) {
        return this.userRepository.findByPseudo(pseudo);
    }

    public User updateUserPersonnalData(String userUUID, String firstname, String lastname, String email, Date dateOfBirth) {
        User userToUpdate = this.getUserById(userUUID);
        
        if (firstname != null) {
            userToUpdate.setFirstname(firstname);
        }
        if (lastname != null) {
            userToUpdate.setLastname(lastname);
        }
        if (email != null) {
            userToUpdate.setEmail(email);
        }
        if (dateOfBirth != null) {
            userToUpdate.setDateOfBirth(dateOfBirth);
        }
        
        this.userRepository.save(userToUpdate);
        return userToUpdate;
    }

    public void changePassword(String userUUID, String currentPassword, String newPassword) {
        User user = this.getUserById(userUUID);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mot de passe actuel incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        this.userRepository.save(user);
    }

    public void deleteUser(String userUUID) {
        User user = this.getUserById(userUUID);
        this.userRepository.delete(user);
    }

}
