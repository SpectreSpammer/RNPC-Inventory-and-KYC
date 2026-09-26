package com.rnpc.inventory.util;

import java.util.regex.Pattern;

/**
 * The one place a contact number is validated, formatted and compared. Every flow that reads or
 * writes a client's number - the client forms, the walk-in ticket, appointments and checkout, all
 * through ClientService - goes through here; do not write a second copy.
 *
 * The rule: a Philippine mobile number is stored and shown as {@code 0917 234 5678} (eleven digits,
 * grouped 4-3-4, single spaces), and two numbers are the same phone when their {@link #matchKey}
 * is equal - digits only, with spaces, dashes, brackets and the +63 prefix ignored. Anything that
 * is not an eleven-digit mobile (a landline, say) keeps what the person typed, cleaned of stray
 * characters, and still matches on digits alone.
 */
public final class PhoneNumbers {

    /**
     * The validation pattern shared by every DTO that takes a contact number. Only digits and the
     * usual separators are allowed, and there must be 7 to 15 digits in all, so "+63 917 234 5678"
     * (sixteen characters) passes while "-------" does not - an all-punctuation value would
     * otherwise collapse to an empty match key.
     */
    public static final String PATTERN = "^(?=(?:\\D*\\d){7,15}\\D*$)[0-9+\\-() ]{7,20}$";

    private static final Pattern MOBILE_KEY = Pattern.compile("09\\d{9}");

    private PhoneNumbers() {
    }

    /**
     * The digits-only key two numbers are compared on: 09172345678, 0917 234 5678, 0917-234-5678,
     * +639172345678 and 9172345678 all give "09172345678". Empty when the input has no digits.
     * The +63 prefix becomes a leading 0 (with a redundant "0" after it, as in "+63 0917...",
     * dropped); a bare 63... counts as a prefix only for a 12-digit mobile, so an ordinary number
     * that happens to start 63 is left alone.
     */
    public static String matchKey(String raw) {
        if (raw == null) {
            return "";
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return "";
        }
        boolean plus63 = raw.replaceAll("\\s", "").startsWith("+63");
        boolean bareMobile63 = digits.length() == 12 && digits.startsWith("639");
        if (digits.startsWith("63") && (plus63 || bareMobile63)) {
            String rest = digits.substring(2);
            if (rest.startsWith("0")) {
                rest = rest.substring(1);
            }
            return "0" + rest;
        }
        if (digits.length() == 10 && digits.charAt(0) == '9') {
            return "0" + digits;
        }
        return digits;
    }

    /**
     * The form a number is stored and displayed in. An eleven-digit mobile becomes
     * {@code 0917 234 5678}; anything else keeps what was typed, minus stray characters, with runs
     * of whitespace collapsed to one space. Null stays null.
     */
    public static String format(String raw) {
        if (raw == null) {
            return null;
        }
        String key = matchKey(raw);
        if (MOBILE_KEY.matcher(key).matches()) {
            return key.substring(0, 4) + " " + key.substring(4, 7) + " " + key.substring(7);
        }
        return raw.replaceAll("[^0-9+()\\-\\s]", "").replaceAll("\\s+", " ").trim();
    }

    /** True when both numbers are the same phone. Two numbers with no digits are never the same. */
    public static boolean sameNumber(String a, String b) {
        String keyA = matchKey(a);
        return !keyA.isEmpty() && keyA.equals(matchKey(b));
    }
}
