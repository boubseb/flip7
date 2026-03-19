package com.flip7.flip7.dto;

import java.sql.Date;

public class UserDTO {
    private String id;
    private String pseudo;
    private String lastname;
    private String firstname;
    private Date dateOfBirth;
    private String email;

    // Constructors
    public UserDTO() {
    }

    public UserDTO(String id, String pseudo, String lastname, String firstname, Date dateOfBirth, String email) {
        this.id = id;
        this.pseudo = pseudo;
        this.lastname = lastname;
        this.firstname = firstname;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
