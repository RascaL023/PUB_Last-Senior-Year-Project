package id.my.rascal.menu.internal.seeder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.seed.Seeder;
import id.my.rascal.common.seed.SeedType;
import id.my.rascal.menu.internal.entity.Menu;
import id.my.rascal.menu.internal.entity.MenuCategory;
import id.my.rascal.menu.internal.entity.ModifierType;
import id.my.rascal.menu.internal.repository.MenuCategoryRepository;
import id.my.rascal.menu.internal.repository.MenuRepository;
import id.my.rascal.menu.internal.repository.ModifierTypeRepository;

@Component
@Order(50)
public class FormalMenuSeeder implements Seeder {

    private final MenuRepository menuRepository;
    private final MenuCategoryRepository categoryRepository;
    private final ModifierTypeRepository modifierTypeRepository;

    public FormalMenuSeeder(
        MenuRepository menuRepository,
        MenuCategoryRepository categoryRepository,
        ModifierTypeRepository modifierTypeRepository
    ) {
        this.menuRepository = menuRepository;
        this.categoryRepository = categoryRepository;
        this.modifierTypeRepository = modifierTypeRepository;
    }

    @Override
    public SeedType seedType() {
        return SeedType.FORMAL;
    }

    @Override
    @Transactional
    public void seed() {
        LocalDateTime now = LocalDateTime.now();
        for (MenuSeedCatalog.MenuSeed seed : MenuSeedCatalog.MENUS) {
            if (menuRepository.existsByName(seed.name())) continue;
            Menu menu = new Menu();
            menu.setName(seed.name());
            menu.setDescription(seed.description());
            menu.setBasePrice(seed.basePrice());
            menu.setIsAvailable(true);
            menu.setImageUrls(new ArrayList<>());
            menu.setCreatedAt(now);
            menu.setUpdatedAt(now);

            List<MenuCategory> categories = new ArrayList<>();
            for (String code : seed.categoryCodes()) {
                categoryRepository.findByCategoryCode(code).ifPresent(categories::add);
            }
            menu.setCategories(categories);

            List<ModifierType> modifiers = new ArrayList<>();
            for (String name : seed.modifierTypeNames()) {
                modifierTypeRepository.findByName(name).ifPresent(modifiers::add);
            }
            menu.setModifierTypes(modifiers);

            menuRepository.save(menu);
        }
    }

}
