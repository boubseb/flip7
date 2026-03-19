package com.flip7.flip7.dto;

import java.sql.Date;

public class RegisterDTO {
    private String pseudo;
    private String lastname;
    private String firstname;
    private String password;
    private Date dateOfBirth;
    private String email;

    // Constructors
    public RegisterDTO() {
    }

    public RegisterDTO(String pseudo, String lastname, String firstname, String password, Date dateOfBirth, String email) {
        this.pseudo = pseudo;
        this.lastname = lastname;
        this.firstname = firstname;
        this.password = password;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
    }

    // Getters and Setters
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
