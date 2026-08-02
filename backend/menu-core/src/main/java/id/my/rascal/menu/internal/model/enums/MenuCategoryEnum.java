package id.my.rascal.menu.internal.model.enums;

public enum MenuCategoryEnum {
    COFFEE("Kopi"),
    TEA("Teh"),
    FOOD("Makanan"),
    SNACK("Makanan Ringan");

    private final String displayName;

    MenuCategoryEnum(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
    public static MenuCategoryEnum from(String value) {
        return MenuCategoryEnum.valueOf(value.toUpperCase());
    }

}

