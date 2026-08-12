package dev.mrlemoos.kingdom.calendar;

/** Regnal ordinals. Reigns beyond {@code MMMCMXCIX} are not a problem this realm will have. */
final class RomanNumerals {

    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private RomanNumerals() {
    }

    static String of(int number) {
        StringBuilder out = new StringBuilder();
        int remaining = number;
        for (int i = 0; i < VALUES.length; i++) {
            while (remaining >= VALUES[i]) {
                remaining -= VALUES[i];
                out.append(SYMBOLS[i]);
            }
        }
        return out.toString();
    }
}
