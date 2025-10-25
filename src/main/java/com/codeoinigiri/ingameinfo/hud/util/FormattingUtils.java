package com.codeoinigiri.ingameinfo.hud.util;

/**
 * Utility helpers for Minecraft formatting codes used in HUD text.
 * Ensures codes appear in canonical order: color codes first, then style codes.
 */
public final class FormattingUtils {
    private FormattingUtils() {}

    /**
     * Reorders consecutive '§' formatting codes in the string so that color codes
     * (0-9, a-f, A-F, r/R) precede style codes (kKlLmMnNoO).
     * Example: "§l§cText" -> "§c§lText".
     */
    public static String reorderFormattingCodes(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == '\u00A7' && i + 1 < text.length()) { // '§'
                StringBuilder codes = new StringBuilder();
                // collect consecutive formatting codes
                while (i < text.length() && text.charAt(i) == '\u00A7' && i + 1 < text.length()) {
                    codes.append(text.charAt(i));
                    codes.append(text.charAt(i + 1));
                    i += 2;
                }
                result.append(reorderCodes(codes.toString()));
            } else {
                result.append(text.charAt(i));
                i++;
            }
        }
        return result.toString();
    }

    /**
     * Internal routine that returns the given formatting codes in the order:
     * colors first, then styles.
     */
    public static String reorderCodes(String codes) {
        if (codes == null || codes.isEmpty()) return codes;
        StringBuilder colorCodes = new StringBuilder();
        StringBuilder styleCodes = new StringBuilder();
        for (int i = 0; i < codes.length(); i += 2) {
            if (i + 1 < codes.length() && codes.charAt(i) == '\u00A7') { // '§'
                char code = codes.charAt(i + 1);
                String codeStr = "\u00A7" + code;
                // colors: 0-9, a-f, A-F, r/R
                if ((code >= '0' && code <= '9') ||
                    (code >= 'a' && code <= 'f') ||
                    (code >= 'A' && code <= 'F') ||
                    code == 'r' || code == 'R') {
                    colorCodes.append(codeStr);
                } else if (code == 'k' || code == 'K' ||
                           code == 'l' || code == 'L' ||
                           code == 'm' || code == 'M' ||
                           code == 'n' || code == 'N' ||
                           code == 'o' || code == 'O') {
                    styleCodes.append(codeStr);
                }
            }
        }
        return colorCodes.toString() + styleCodes.toString();
    }
}
