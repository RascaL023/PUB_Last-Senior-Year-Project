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
        new CategorySeed("DESSERT", "Penutup", 6),
        new CategorySeed("JUICE", "Jus", 7)
    );

    public static final List<ModifierSeed> MODIFIERS = List.of(
        new ModifierSeed("Sugar Level", 1, 1, List.of(
            new OptionSeed("Tanpa Gula", 0),
            new OptionSeed("Sedikit", 0),
            new OptionSeed("Normal", 0),
            new OptionSeed("Extra", 0)
        )),
        new ModifierSeed("Ice Level", 1, 1, List.of(
            new OptionSeed("Tanpa Es", 0),
            new OptionSeed("Sedikit Es", 0),
            new OptionSeed("Es Normal", 0),
            new OptionSeed("Extra Es", 0)
        )),
        new ModifierSeed("Milk Type", 0, 1, List.of(
            new OptionSeed("Full Cream", 0),
            new OptionSeed("Skim", 0),
            new OptionSeed("Oat", 2000),
            new OptionSeed("Almond", 3000)
        )),
        new ModifierSeed("Size", 1, 1, List.of(
            new OptionSeed("Regular", 0),
            new OptionSeed("Large", 4000),
            new OptionSeed("Extra Large", 7000)
        )),
        new ModifierSeed("Topping", 0, 5, List.of(
            new OptionSeed("Pearl", 3000),
            new OptionSeed("Grass Jelly", 4000),
            new OptionSeed("Cheese Foam", 5000),
            new OptionSeed("Chocolate Drizzle", 3500)
        )),
        new ModifierSeed("Extra Shot", 0, 3, List.of(
            new OptionSeed("1 Shot", 5000),
            new OptionSeed("2 Shot", 9000)
        )),
        new ModifierSeed("Spice Level", 1, 1, List.of(
            new OptionSeed("Tidak Pedas", 0),
            new OptionSeed("Sedang", 0),
            new OptionSeed("Pedas", 0),
            new OptionSeed("Sangat Pedas", 0)
        )),
        new ModifierSeed("Sides", 0, 3, List.of(
            new OptionSeed("Fries", 15000),
            new OptionSeed("Hashbrown", 12000)
        ))
    );

    public static final List<MenuSeed> MENUS = List.of(
        // ---- Coffee ----
        new MenuSeed("Espresso", "Espresso murni single shot", 15000,
            List.of("COFFEE", "BEVERAGE"), List.of("Size", "Extra Shot")),
        new MenuSeed("Americano", "Espresso hitam dengan tambahan air panas", 18000,
            List.of("COFFEE", "BEVERAGE"), List.of("Size", "Extra Shot")),
        new MenuSeed("Cafe Latte", "Espresso dengan susu steamed", 22000,
            List.of("COFFEE", "BEVERAGE"),
            List.of("Sugar Level", "Ice Level", "Milk Type", "Size", "Extra Shot")),
        new MenuSeed("Cappuccino", "Espresso, susu, dan foam", 24000,
            List.of("COFFEE", "BEVERAGE"),
            List.of("Sugar Level", "Ice Level", "Milk Type", "Size", "Extra Shot")),
        new MenuSeed("Hazelnut Latte", "Cafe latte dengan sirup hazelnut", 30000,
            List.of("COFFEE", "BEVERAGE"),
            List.of("Sugar Level", "Ice Level", "Milk Type", "Size", "Extra Shot")),
        new MenuSeed("Caramel Macchiato", "Espresso dengan susu, sirup karamel, dan foam", 32000,
            List.of("COFFEE", "BEVERAGE"),
            List.of("Sugar Level", "Ice Level", "Milk Type", "Size", "Extra Shot", "Topping")),
        new MenuSeed("Cold Brew", "Kopi seduh dingin selama 12 jam", 26000,
            List.of("COFFEE", "BEVERAGE"),
            List.of("Sugar Level", "Ice Level", "Size", "Topping")),
        new MenuSeed("Mocha", "Espresso cokelat dengan susu", 28000,
            List.of("COFFEE", "BEVERAGE"),
            List.of("Sugar Level", "Ice Level", "Milk Type", "Size", "Extra Shot", "Topping")),

        // ---- Tea & beverage ----
        new MenuSeed("Matcha Latte", "Matcha dengan susu", 26000,
            List.of("TEA", "BEVERAGE"),
            List.of("Sugar Level", "Ice Level", "Milk Type", "Size")),
        new MenuSeed("Thai Tea", "Teh thailand dengan susu kental manis", 24000,
            List.of("TEA", "BEVERAGE"),
            List.of("Sugar Level", "Ice Level", "Milk Type", "Size", "Topping")),
        new MenuSeed("Earl Grey", "Teh earl grey hangat", 18000,
            List.of("TEA", "BEVERAGE"),
            List.of("Sugar Level", "Ice Level", "Size")),
        new MenuSeed("Lemon Tea", "Teh hitam dengan perasan lemon segar", 20000,
            List.of("TEA", "BEVERAGE"),
            List.of("Sugar Level", "Ice Level", "Size")),
        new MenuSeed("Chocolate Milk", "Susu cokelat hangat atau dingin", 21000,
            List.of("BEVERAGE"),
            List.of("Sugar Level", "Ice Level", "Milk Type", "Size", "Topping")),

        // ---- Juice ----
        new MenuSeed("Fresh Orange Juice", "Jus jeruk peras segar tanpa gula tambahan", 25000,
            List.of("JUICE", "BEVERAGE"), List.of("Size")),
        new MenuSeed("Watermelon Juice", "Jus semangka dingin yang menyegarkan", 23000,
            List.of("JUICE", "BEVERAGE"), List.of("Size")),

        // ---- Snack, main & dessert ----
        new MenuSeed("Croissant", "Croissant butter panggang", 15000,
            List.of("SNACK", "DESSERT"), List.of("Sides")),
        new MenuSeed("Nasi Goreng", "Nasi goreng rumahan dengan bumbu khas", 28000,
            List.of("MAIN"), List.of("Spice Level", "Sides")),
        new MenuSeed("Chicken Katsu", "Ayam katsu renyah dengan saus khas", 32000,
            List.of("MAIN"), List.of("Spice Level", "Sides")),
        new MenuSeed("Beef Burger", "Burger daging sapi dengan saus spesial", 35000,
            List.of("MAIN", "SNACK"), List.of("Spice Level", "Sides")),
        new MenuSeed("Cheesecake", "Cheesecake lembut dengan topping pilihan", 28000,
            List.of("DESSERT", "SNACK"), List.of("Topping"))
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
