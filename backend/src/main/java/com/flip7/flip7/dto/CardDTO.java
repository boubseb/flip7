package com.flip7.flip7.dto;

/**
 * DTO pour les informations d'une carte dans les réponses API
 */
public class CardDTO {
    private String id;
    private String type;           // NUMBER, OPERATOR, SPECIAL
    private String displayName;
    private Integer value;         // Pour les cartes numérotées
    private String operatorType;   // Pour les cartes opérateurs
    private String specialType;    // Pour les cartes spéciales

    public CardDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(String operatorType) {
        this.operatorType = operatorType;
    }

    public String getSpecialType() {
        return specialType;
    }

    public void setSpecialType(String specialType) {
        this.specialType = specialType;
    }
}
