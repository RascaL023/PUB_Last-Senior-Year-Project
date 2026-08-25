package id.my.rascal.common.util;

public class StringUtil {

    public static boolean safeIsBlank(String s) {
        return s == null || s.isBlank();
    }

    public static String normalizeSearch(String q) {
        if (safeIsBlank(q)) return "";
        return q.trim();
    }

    public static String normalizeAndCapitalizeFirst(String s) {
        if (safeIsBlank(s)) return "";
        s = s.trim();
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    public static String toSlug(String s) {
        if (safeIsBlank(s)) return "";

        return s.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    public static String normalizeSpaces(String s) {
        if (safeIsBlank(s))
            return "";

        return s.trim().replaceAll("\\s+", " ");
    }

    public static String capitalize(String s) {
        if (safeIsBlank(s)) return "";

        s = s.trim();

        return Character.toUpperCase(s.charAt(0))
                + s.substring(1);
    }

    public static String toUnderscoredEnum(String s) {
        if (safeIsBlank(s)) return "";

        return normalizeSpaces(s).replace(' ', '_').replace('-', '_');
    }
    
}
