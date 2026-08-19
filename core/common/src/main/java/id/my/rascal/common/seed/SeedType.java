package id.my.rascal.common.seed;

/**
 * The flavour of seed data to run. Mirrors the legacy seed profiles
 * ({@code dev-seed} / {@code formal-seed}) so the {@code --seed} argument
 * can select seeders without needing a Spring profile.
 */
public enum SeedType {
    DEV,
    FORMAL;

    /**
     * Maps a user-supplied / profile value to a {@link SeedType}.
     * Accepts {@code dev}, {@code formal} and the legacy forms
     * {@code dev-seed} / {@code formal-seed}, case-insensitively.
     *
     * @return the matching type, or {@code null} when the value is not a seed type
     */
    public static SeedType from(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase()
                .replace("-seed", "")
                .replace("_", "");
        try {
            return SeedType.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}