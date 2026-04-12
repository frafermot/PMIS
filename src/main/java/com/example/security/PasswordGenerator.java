package com.example.security;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class PasswordGenerator {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "@#$%&+";
    private static final String ALL = LOWER + UPPER + DIGITS + SPECIAL;
    private static final int MIN_LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    public String generateStrongPassword() {
        List<Character> charList = new ArrayList<>();

        // Ensure at least one of each required type
        charList.add(LOWER.charAt(random.nextInt(LOWER.length())));
        charList.add(UPPER.charAt(random.nextInt(UPPER.length())));
        charList.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
        charList.add(SPECIAL.charAt(random.nextInt(SPECIAL.length())));

        // Fill the rest up to MIN_LENGTH
        for (int i = 4; i < MIN_LENGTH; i++) {
            charList.add(ALL.charAt(random.nextInt(ALL.length())));
        }

        // Shuffle
        Collections.shuffle(charList);

        StringBuilder password = new StringBuilder(MIN_LENGTH);
        for (Character c : charList) {
            password.append(c);
        }

        return password.toString();
    }
}
