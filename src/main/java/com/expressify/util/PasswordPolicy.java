package com.expressify.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Password rules: longer than 8 characters (minimum 9), and at least one uppercase
 * letter, lowercase letter, digit, and special (non-alphanumeric) character.
 */
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static final int MIN_LENGTH_EXCLUSIVE = 8;

    public static void validate(String password) {
        if (password == null || password.length() <= MIN_LENGTH_EXCLUSIVE) {
            throw new IllegalArgumentException(
                    "Password must be longer than 8 characters (at least 9 characters).");
        }
        List<String> missing = new ArrayList<>();
        if (!password.matches(".*[A-Z].*")) {
            missing.add("an uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            missing.add("a lowercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            missing.add("a number");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            missing.add("a special character");
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Password must include " + formatMissing(missing) + ".");
        }
    }

    private static String formatMissing(List<String> missing) {
        if (missing.size() == 1) {
            return missing.get(0);
        }
        if (missing.size() == 2) {
            return missing.get(0) + " and " + missing.get(1);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < missing.size(); i++) {
            if (i > 0) {
                sb.append(i == missing.size() - 1 ? ", and " : ", ");
            }
            sb.append(missing.get(i));
        }
        return sb.toString();
    }
}
