package id.my.rascal.menu.internal.seeder;

import java.util.List;

public final class MenuSeedCatalog {

    private MenuSeedCatalog() {}

    public static final List<CategorySeed> CATEGORIES = List.of(
        new CategorySeed("COFFEE", "Kopi", 1),
        new CategorySeed("TEA", "Teh", 2),
        new CategorySeed("BEVERAGE", "Minuman", 3),
        new CategorySeed("SNACK", "Camilan", 4),
        new CategorySeed("MAIN", "Makanan Utama", 5),
        new CategorySeed("DESSERT", "Penutup", 6)
    );

    public static final List<ModifierSeed> MODIFIERS = List.of(
        new ModifierSeed("SUGAR_LEVEL", 1, 1, List.of(
            new OptionSeed("Tanpa Gula", 0),
            new OptionSeed("Sedikit", 0),
            new OptionSeed("Normal", 0),
            new OptionSeed("Extra", 0)
        )),
        new ModifierSeed("ICE_LEVEL", 1, 1, List.of(
            new OptionSeed("Tanpa Es", 0),
            new OptionSeed("Sedikit Es", 0),
            new OptionSeed("Es Normal", 0),
            new OptionSeed("Extra Es", 0)
        )),
        new ModifierSeed("MILK_TYPE", 0, 1, List.of(
            new OptionSeed("Full Cream", 0),
            new OptionSeed("Skim", 0),
            new OptionSeed("Oat", 2000),
            new OptionSeed("Almond", 3000)
        )),
        new ModifierSeed("SIZE", 1, 1, List.of(
            new OptionSeed("Regular", 0),
            new OptionSeed("Large", 4000),
            new OptionSeed("Extra Large", 7000)
        )),
        new ModifierSeed("TOPPING", 0, 5, List.of(
            new OptionSeed("Pearl", 3000),
            new OptionSeed("Grass Jelly", 4000),
            new OptionSeed("Cheese Foam", 5000),
            new OptionSeed("Chocolate Drizzle", 3500)
        )),
        new ModifierSeed("EXTRA_SHOT", 0, 3, List.of(
            new OptionSeed("1 Shot", 5000),
            new OptionSeed("2 Shot", 9000)
        )),
        new ModifierSeed("SPICE_LEVEL", 1, 1, List.of(
            new OptionSeed("Tidak Pedas", 0),
            new OptionSeed("Sedang", 0),
            new OptionSeed("Pedas", 0),
            new OptionSeed("Sangat Pedas", 0)
        )),
        new ModifierSeed("SIDES", 0, 3, List.of(
            new OptionSeed("Fries", 15000),
            new OptionSeed("Hashbrown", 12000)
        ))
    );

    public static final List<MenuSeed> MENUS = List.of(
        new MenuSeed("Espresso", "Espresso murni single shot", 15000,
            List.of("COFFEE", "BEVERAGE"), List.of("SIZE", "EXTRA_SHOT")),
        new MenuSeed("Cafe Latte", "Espresso dengan susu steamed", 22000,
            List.of("COFFEE", "BEVERAGE"),
            List.of("SUGAR_LEVEL", "ICE_LEVEL", "MILK_TYPE", "SIZE", "EXTRA_SHOT")),
        new MenuSeed("Cappuccino", "Espresso, susu, dan foam", 24000,
            List.of("COFFEE", "BEVERAGE"),
            List.of("SUGAR_LEVEL", "ICE_LEVEL", "MILK_TYPE", "SIZE", "EXTRA_SHOT")),
        new MenuSeed("Cold Brew", "Kopi seduh dingin 12 jam", 26000,
            List.of("COFFEE", "BEVERAGE"),
            List.of("SUGAR_LEVEL", "ICE_LEVEL", "SIZE", "TOPPING")),
        new MenuSeed("Mocha", "Espresso cokelat dengan susu", 28000,
            List.of("COFFEE", "BEVERAGE"),
            List.of("SUGAR_LEVEL", "ICE_LEVEL", "MILK_TYPE", "SIZE", "EXTRA_SHOT", "TOPPING")),
        new MenuSeed("Matcha Latte", "Matcha dengan susu", 26000,
            List.of("TEA", "BEVERAGE"),
            List.of("SUGAR_LEVEL", "ICE_LEVEL", "MILK_TYPE", "SIZE")),
        new MenuSeed("Earl Grey", "Teh earl grey", 18000,
            List.of("TEA", "BEVERAGE"),
            List.of("SUGAR_LEVEL", "ICE_LEVEL", "SIZE")),
        new MenuSeed("Chocolate Milk", "Susu cokelat", 21000,
            List.of("BEVERAGE"),
            List.of("SUGAR_LEVEL", "ICE_LEVEL", "MILK_TYPE", "SIZE", "TOPPING")),
        new MenuSeed("Croissant", "Croissant butter panggang", 15000,
            List.of("SNACK", "DESSERT"), List.of("SIDES")),
        new MenuSeed("Nasi Goreng", "Nasi goreng rumahan", 28000,
            List.of("MAIN"), List.of("SPICE_LEVEL", "SIDES"))
    );


    public record OptionSeed(String name, int additionalPrice) {}

    public record CategorySeed(
        String categoryCode, 
        String displayName, 
        int displayOrder
    ) {}

    public record ModifierSeed(
        String name, 
        int minSelection, 
        int maxSelection, 
        List<OptionSeed> options
    ) {}

    public record MenuSeed(
        String name,
        String description,
        int basePrice,
        List<String> categoryCodes,
        List<String> modifierTypeNames
    ) {}

}
