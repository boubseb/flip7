package com.flip7.flip7.controller;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.flip7.flip7.dto.RegisterDTO;
import com.flip7.flip7.dto.UserDTO;
import com.flip7.flip7.entity.User;
import com.flip7.flip7.service.UserService;



@RestController
public class UserController {

   

    
    @Autowired
    private UserService userService;

  // Provide a default value so app still starts when the property is missing
  @Value("${myApp.BearerHeader:Bearer }")
  private  String BearerPrefix;

  @PostConstruct
  private void init() {
    // Ne pas trim pour garder l'espace après "Bearer "
    if (this.BearerPrefix == null || this.BearerPrefix.isEmpty()) {
      this.BearerPrefix = "Bearer ";
    }
  }


      @GetMapping("/profil")
  public ResponseEntity<User> getUserProfil(@RequestHeader(value = "Authorization", required = false) String BearerHeader) {
    if (BearerHeader == null || BearerHeader.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Authorization header");
    }

    if (!BearerHeader.startsWith(BearerPrefix)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Authorization header format. Expected: '" + BearerPrefix + "', Got: '" + BearerHeader + "'");
    }

    String userUUID = BearerHeader.substring(BearerPrefix.length()).trim();
    if (userUUID.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty token in Authorization header");
    }

    System.out.println("User UUID extracted: '" + userUUID + "' (length: " + userUUID.length() + ")");
    
    try {
      User user = userService.getUserById(userUUID);
      System.out.println("User found: " + user.getPseudo());
      return ResponseEntity.ok(user);
    } catch (Exception e) {
      System.err.println("Error getting user: " + e.getMessage());
      e.printStackTrace();
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving user: " + e.getMessage());
    }
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.OK)
    public void postUser(@RequestBody RegisterDTO registerDTO) {
        System.out.println("=== POST /register called ===");
        System.out.println("RegisterDTO received: " + registerDTO.getPseudo());
        
        // Convert RegisterDTO to User entity
        User user = new User();
        user.setPseudo(registerDTO.getPseudo());
        user.setLastname(registerDTO.getLastname());
        user.setFirstname(registerDTO.getFirstname());
        user.setPassword(registerDTO.getPassword());
        user.setDateOfBirth(registerDTO.getDateOfBirth());
        user.setEmail(registerDTO.getEmail());
        
        System.out.println("Saving user with password: " + (registerDTO.getPassword() != null ? "***" : "NULL"));
        userService.save(user);
        System.out.println("User registered successfully");
}

//       @GetMapping("user/{id}")
//     public ResponseEntity<User> getUserById(@PathVariable String id) {
//         User user = userService.getUserById(id);
//         return ResponseEntity.ok(user);
//     }

    @PostMapping("/login")
    public Map<String, String> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        User userFromDB = userService.checkCredentials(username, password);
        if (userFromDB != null) {
            Map<String, String> response = new HashMap<>();
            response.put("access_token", userFromDB.getId());
            response.put("role", userFromDB.getRole());
            return response;
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

//     @DeleteMapping("delete")
//     public String deleteUser(@RequestHeader("Authorization") String BearerHeader, @RequestParam("password") String password) {
   
//         String userUUID = BearerHeader.substring(BearerPrefix.length());
//         return userService.delete(userUUID, password);
        

    @PostMapping("/updateProfile")
    public ResponseEntity<UserDTO> updateUserProfile(
            @RequestHeader("Authorization") String BearerHeader,
            @RequestBody Map<String, Object> updates) {
        
        if (BearerHeader == null || !BearerHeader.startsWith(BearerPrefix)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Authorization header");
        }
        
        String userUUID = BearerHeader.substring(BearerPrefix.length());
        
        String firstname = (String) updates.get("firstname");
        String lastname = (String) updates.get("lastname");
        String email = (String) updates.get("email");
        String dateOfBirthStr = (String) updates.get("dateOfBirth");
        Date dateOfBirth = dateOfBirthStr != null ? Date.valueOf(dateOfBirthStr) : null;
        
        User updatedUser = userService.updateUserPersonnalData(userUUID, firstname, lastname, email, dateOfBirth);
        
        // Convert to DTO
        UserDTO userDTO = new UserDTO(
            updatedUser.getId(),
            updatedUser.getPseudo(),
            updatedUser.getLastname(),
            updatedUser.getFirstname(),
            updatedUser.getDateOfBirth(),
            updatedUser.getEmail()
        );
        
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/changePassword")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestHeader("Authorization") String BearerHeader,
            @RequestBody Map<String, String> passwordData) {
        
        if (BearerHeader == null || !BearerHeader.startsWith(BearerPrefix)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Authorization header");
        }
        
        String userUUID = BearerHeader.substring(BearerPrefix.length());
        String currentPassword = passwordData.get("currentPassword");
        String newPassword = passwordData.get("newPassword");

        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe actuel est requis");
        }

        try {
            userService.changePassword(userUUID, currentPassword, newPassword);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password changed successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/deleteAccount")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @RequestHeader("Authorization") String BearerHeader) {
        
        if (BearerHeader == null || !BearerHeader.startsWith(BearerPrefix)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Authorization header");
        }
        
        String userUUID = BearerHeader.substring(BearerPrefix.length());
        userService.deleteUser(userUUID);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Account deleted successfully");
        return ResponseEntity.ok(response);
    }
    
}
