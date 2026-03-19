package com.flip7.flip7.service;

import java.sql.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.flip7.flip7.entity.User;
import com.flip7.flip7.repository.UserRepository;

@Service
public class UserService {

        @Autowired
    private UserRepository userRepository;




        public List<User> getAll() {
        List<User> users = this.userRepository.findAll();
        return users;
    }

    public User getUserById(String id) {
        return this.userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public void save(User user) {
        this.userRepository.save(user);
    }

    public String delete(String userUUID,String password) {
        User user = this.getUserById(userUUID);
        if (user.getPassword().equals(password)) {
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
        user.setPassword(newPassword);
        this.userRepository.save(user);
    }

    public void deleteUser(String userUUID) {
        User user = this.getUserById(userUUID);
        this.userRepository.delete(user);
    }

}
