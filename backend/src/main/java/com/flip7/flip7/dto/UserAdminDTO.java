package com.flip7.flip7.dto;

public class UserAdminDTO {
    private String id;
    private String pseudo;
    private String email;
    private String role;

    public UserAdminDTO(String id, String pseudo, String email, String role) {
        this.id = id;
        this.pseudo = pseudo;
        this.email = email;
        this.role = role;
    }

    public String getId() { return id; }
    public String getPseudo() { return pseudo; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
