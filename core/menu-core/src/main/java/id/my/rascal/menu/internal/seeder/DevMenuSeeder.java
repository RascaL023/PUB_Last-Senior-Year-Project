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
import id.my.rascal.menu.internal.service.MenuSearchService;

@Component
@Order(50)
public class DevMenuSeeder implements Seeder {

    private final MenuRepository menuRepository;
    private final MenuCategoryRepository categoryRepository;
    private final ModifierTypeRepository modifierTypeRepository;
    private final MenuSearchService menuSearchService;

    public DevMenuSeeder(
        MenuRepository menuRepository,
        MenuCategoryRepository categoryRepository,
        ModifierTypeRepository modifierTypeRepository,
        MenuSearchService menuSearchService
    ) {
        this.menuRepository = menuRepository;
        this.categoryRepository = categoryRepository;
        this.modifierTypeRepository = modifierTypeRepository;
        this.menuSearchService = menuSearchService;
    }

    @Override
    public SeedType seedType() {
        return SeedType.DEV;
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

            Menu saved = menuRepository.save(menu);
            // Keep the Meilisearch read projection in sync with newly seeded data.
            menuSearchService.indexMenu(saved);
        }

        // Re-index every menu already in the DB. The loop above only inserts when a
        // name is missing (DB idempotency), so if the DB was seeded on a previous run
        // while the Meilisearch index is empty/reset, no document would ever be added.
        // Upserting by primary key is idempotent, so re-running seeds stays safe and
        // the index always mirrors PostgreSQL after a seed boot.
        menuRepository.findAll().forEach(menuSearchService::indexMenu);
    }

}
