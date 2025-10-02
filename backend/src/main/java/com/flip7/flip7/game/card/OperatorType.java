package com.flip7.flip7.game.card;

/**
 * Types d'opérateurs
 */
public enum OperatorType {
    PLUS_2(2, false),      // +2
    PLUS_4(4, false),      // +4
    PLUS_6(6, false),      // +6
    PLUS_8(8, false),      // +8
    PLUS_10(10, false),    // +10
    MULTIPLY_2(2, true);   // ×2

    private final int value;
    private final boolean isMultiply;

    OperatorType(int value, boolean isMultiply) {
        this.value = value;
        this.isMultiply = isMultiply;
    }

    public int getValue() {
        return value;
    }

    public boolean isMultiply() {
        return isMultiply;
    }

    public int apply(int currentScore) {
        if (isMultiply) {
            return currentScore * value;
        } else {
            return currentScore + value;
        }
    }
}
