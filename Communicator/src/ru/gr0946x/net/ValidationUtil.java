package ru.gr0946x.net;

public final class ValidationUtil {
    private ValidationUtil() {}

    /** Начинается с буквы, 3–20 символов, буквы/цифры/подчёркивание */
    public static boolean isValidUsername(String name) {
        if (name == null || name.isBlank()) return false;
        return name.matches("[a-zA-Zа-яА-ЯёЁ][a-zA-Zа-яА-ЯёЁ0-9_]{2,19}");
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 4;
    }
}