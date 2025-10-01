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
    if (this.BearerPrefix != null) {
      this.BearerPrefix = this.BearerPrefix.trim();
    }
  }


      @GetMapping("/profil")
  public ResponseEntity<User> getUserProfil(@RequestHeader(value = "Authorization", required = false) String BearerHeader) {
    if (BearerHeader == null || BearerHeader.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Authorization header");
    }

    if (!BearerHeader.startsWith(BearerPrefix)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Authorization header format");
    }

    String userUUID = BearerHeader.substring(BearerPrefix.length());
    if (userUUID.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty token in Authorization header");
    }

    System.out.println(userUUID);
    User user = userService.getUserById(userUUID);
    return ResponseEntity.ok(user);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.OK)
    public void postUser(@RequestBody User user) {
       userService.save(user);
}

//       @GetMapping("user/{id}")
//     public ResponseEntity<User> getUserById(@PathVariable String id) {
//         User user = userService.getUserById(id);
//         return ResponseEntity.ok(user);
//     }

    @PostMapping("/login")
    public  Map<String, String> login(@RequestParam("username") String username,@RequestParam("password") String password) {

        User userFromDB  =userService.findByPseudo(username);
        if(userFromDB != null && userFromDB.getPassword().equals(password)){
            Map<String, String> response = new HashMap<>();
            response.put("access_token", userFromDB.getId());
            return response;
        }
        else{
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

//     @DeleteMapping("delete")
//     public String deleteUser(@RequestHeader("Authorization") String BearerHeader, @RequestParam("password") String password) {
   
//         String userUUID = BearerHeader.substring(BearerPrefix.length());
//         return userService.delete(userUUID, password);
        

// }
//     @PostMapping("updatePersonnalData")
//     public User updateUserPersonnalData(@RequestHeader("Authorization") String BearerHeader,@RequestParam("firstname") String firstname,
//     @RequestParam("lastname") String lastname,@RequestParam("email") String email,@RequestParam("dateOfBirth") Date dateOfBirth) {
//         String userUUID=BearerHeader.substring(BearerPrefix.length());
//         return userService.updateUserPersonnalData(userUUID,firstname,lastname,email,dateOfBirth);
//     }

//     @PostMapping("changePassword")
//    public void changePassword(@RequestHeader("Authorization") String BearerHeader,@RequestParam("oldPassword") String oldPassword,@RequestParam("newPassword") String newPassword) {
//        String userUUID=BearerHeader.substring(BearerPrefix.length());
//        userService.changePassword(userUUID,oldPassword,newPassword);
//    }

//    @PostMapping("updateBiography")
//     public User updateBiography(@RequestHeader("Authorization") String BearerHeader,@RequestParam("biography") String biography) {
//          String userUUID=BearerHeader.substring(BearerPrefix.length());
//          return userService.updateBiography(userUUID,biography);
//     }

//     @PostMapping("updateParameters")
//     public User updateParameters(@RequestHeader("Authorization") String BearerHeader,@RequestParam("isPublic") Boolean isPublic,@RequestParam("isNewsletter") Boolean isNewsletter) {
//         String userUUID=BearerHeader.substring(BearerPrefix.length());
//         return userService.updateParameters(userUUID,isPublic,isNewsletter);
//     }

//     @PostMapping("updateDisplayPseudo")
//     public User updateDisplayPseudo(@RequestHeader("Authorization") String BearerHeader,@RequestParam("displayPseudo") String displayPseudo) {
//         String userUUID=BearerHeader.substring(BearerPrefix.length());
//         return userService.updateDisplayPseudo(userUUID,displayPseudo);
//     }

//     @PostMapping("updateProfilPicture")
//     public User updateProfilPicture(@RequestHeader("Authorization") String BearerHeader,@RequestParam("profilPictureUrl") String profilPictureUrl) {
//         String userUUID=BearerHeader.substring(BearerPrefix.length());
//         return userService.updateProfilPicture(userUUID,profilPictureUrl);
//     }   
    
}
