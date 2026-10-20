package de.exit.perfcraft.service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResistorColorCode {

    public enum ResistorColor {
        BLACK(0, 1.0, null, "#1A1A1A"),
        BROWN(1, 10.0, 1.0, "#6D4C41"),
        RED(2, 100.0, 2.0, "#D32F2F"),
        ORANGE(3, 1_000.0, null, "#F57C00"),
        YELLOW(4, 10_000.0, null, "#FBC02D"),
        GREEN(5, 100_000.0, 0.5, "#388E3C"),
        BLUE(6, 1_000_000.0, 0.25, "#1976D2"),
        VIOLET(7, 10_000_000.0, 0.1, "#7B1FA2"),
        GRAY(8, 100_000_000.0, 0.05, "#616161"),
        WHITE(9, 1_000_000_000.0, null, "#EEEEEE"),
        GOLD(-1, 0.1, 5.0, "#C5A059"),
        SILVER(-2, 0.01, 10.0, "#9E9E9E");

        private final int digit;
        private final double multiplier;
        private final Double tolerance;
        private final String hexColor;

        ResistorColor(int digit, double multiplier, Double tolerance, String hexColor) {
            this.digit = digit;
            this.multiplier = multiplier;
            this.tolerance = tolerance;
            this.hexColor = hexColor;
        }

        public int getDigit() { return digit; }
        public double getMultiplier() { return multiplier; }
        public Double getTolerance() { return tolerance; }
        public String getHexColor() { return hexColor; }

        public static ResistorColor fromDigit(int d) {
            for (ResistorColor c : values()) {
                if (c.digit == d) return c;
            }
            return BLACK;
        }

        public static ResistorColor fromMultiplierExponent(int exp) {
            return switch (exp) {
                case -2 -> SILVER;
                case -1 -> GOLD;
                case 0 -> BLACK;
                case 1 -> BROWN;
                case 2 -> RED;
                case 3 -> ORANGE;
                case 4 -> YELLOW;
                case 5 -> GREEN;
                case 6 -> BLUE;
                case 7 -> VIOLET;
                case 8 -> GRAY;
                case 9 -> WHITE;
                default -> BLACK;
            };
        }

        public static ResistorColor fromTolerance(double percent) {
            if (Math.abs(percent - 1.0) < 0.01) return BROWN;
            if (Math.abs(percent - 2.0) < 0.01) return RED;
            if (Math.abs(percent - 0.5) < 0.01) return GREEN;
            if (Math.abs(percent - 0.25) < 0.01) return BLUE;
            if (Math.abs(percent - 0.1) < 0.01) return VIOLET;
            if (Math.abs(percent - 10.0) < 0.01) return SILVER;
            return GOLD; // Standard 5%
        }
    }

    public record Bands(ResistorColor band1, ResistorColor band2, ResistorColor multiplier, ResistorColor tolerance) {}

    private static final Pattern RESISTOR_PATTERN = Pattern.compile(
        "^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*([kKMmRΩ]?)\\s*$", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SHORT_PREFIX_PATTERN = Pattern.compile(
        "^\\s*([0-9]+)\\s*([kKMmR])\\s*([0-9]+)?\\s*$", Pattern.CASE_INSENSITIVE
    );

    public static Double parseOhms(String input) {
        if (input == null || input.isBlank()) return null;
        String clean = input.trim().replace("Ω", "").replace("ohm", "").trim();

        Matcher sm = SHORT_PREFIX_PATTERN.matcher(clean);
        if (sm.matches()) {
            double whole = Double.parseDouble(sm.group(1));
            String prefix = sm.group(2).toUpperCase(Locale.ROOT);
            double fraction = sm.group(3) != null ? Double.parseDouble("0." + sm.group(3)) : 0.0;
            double mult = switch (prefix) {
                case "K" -> 1_000.0;
                case "M" -> 1_000_000.0;
                default -> 1.0;
            };
            return (whole + fraction) * mult;
        }

        Matcher m = RESISTOR_PATTERN.matcher(clean);
        if (m.matches()) {
            double val = Double.parseDouble(m.group(1).replace(",", "."));
            String unit = m.group(2).toUpperCase(Locale.ROOT);
            double mult = switch (unit) {
                case "K" -> 1_000.0;
                case "M" -> 1_000_000.0;
                default -> 1.0;
            };
            return val * mult;
        }
        return null;
    }

    public static Bands calculate4Band(double ohms, double tolerancePercent) {
        if (ohms <= 0) return null;
        int exponent = (int) Math.floor(Math.log10(ohms)) - 1;
        double normalized = ohms / Math.pow(10, exponent);
        int val = (int) Math.round(normalized);
        int d1 = (val / 10) % 10;
        int d2 = val % 10;

        ResistorColor b1 = ResistorColor.fromDigit(d1);
        ResistorColor b2 = ResistorColor.fromDigit(d2);
        ResistorColor mult = ResistorColor.fromMultiplierExponent(exponent);
        ResistorColor tol = ResistorColor.fromTolerance(tolerancePercent);
        return new Bands(b1, b2, mult, tol);
    }

    public static Bands parseAndCalculate(String input) {
        Double ohms = parseOhms(input);
        if (ohms == null) return null;
        return calculate4Band(ohms, 5.0);
    }
}
