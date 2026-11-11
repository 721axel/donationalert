package com.donationalerts;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parsing and formatting of Donut-style money amounts (1k, 2.5m, 27.06b, 1t). */
public final class Money {
    private static final String[] SUFFIX = {"", "K", "M", "B", "T"};
    private static final double[] DIV = {1d, 1e3, 1e6, 1e9, 1e12};
    private static final Pattern ARG = Pattern.compile("^\\$?(\\d[\\d,]*(?:\\.\\d+)?)\\s*([kmbtKMBT]?)$");

    private Money() {}

    private static int suffixIndex(String s) {
        if (s == null || s.isEmpty()) return 0;
        return switch (Character.toUpperCase(s.charAt(0))) {
            case 'K' -> 1;
            case 'M' -> 2;
            case 'B' -> 3;
            case 'T' -> 4;
            default -> 0;
        };
    }

    /** Returns the amount, or -1 if it can't be parsed. */
    public static long parse(String number, String suffix) {
        try {
            double v = Double.parseDouble(number.replace(",", ""));
            return Math.round(v * DIV[suffixIndex(suffix)]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Parses command arguments like "100b", "$27.06B", "1,500,000". Returns -1 on failure. */
    public static long parseArg(String arg) {
        Matcher m = ARG.matcher(arg.trim());
        if (!m.matches()) return -1;
        return parse(m.group(1), m.group(2));
    }

    private static int unitIndex(long amount) {
        if (amount >= 1_000_000_000_000L) return 4;
        if (amount >= 1_000_000_000L) return 3;
        if (amount >= 1_000_000L) return 2;
        if (amount >= 1_000L) return 1;
        return 0;
    }

    /** "$27.06B", "$50B", "$1M". */
    public static String format(long amount) {
        int i = unitIndex(amount);
        String s = String.format(Locale.ROOT, "%.2f", amount / DIV[i]);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return "$" + s + SUFFIX[i];
    }

    /** Fixed two-decimal format in the same unit as {@code reference}; used for the count-up effect. */
    public static String formatIn(double value, long reference) {
        int i = unitIndex(reference);
        return String.format(Locale.ROOT, "$%.2f%s", value / DIV[i], SUFFIX[i]);
    }
}
