package com.flip7.flip7.service;

import java.sql.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.flip7.flip7.entity.User;
import com.flip7.flip7.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;




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

    public void changePassword(String userUUID, String newPassword) {
        User user = this.getUserById(userUUID);
        user.setPassword(passwordEncoder.encode(newPassword));
        this.userRepository.save(user);
    }

    public void deleteUser(String userUUID) {
        User user = this.getUserById(userUUID);
        this.userRepository.delete(user);
    }

}
