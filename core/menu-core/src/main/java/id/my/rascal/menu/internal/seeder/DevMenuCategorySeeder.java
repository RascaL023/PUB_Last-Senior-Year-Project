package id.my.rascal.menu.internal.seeder;

import java.time.LocalDateTime;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.seed.Seeder;
import id.my.rascal.common.seed.SeedType;
import id.my.rascal.menu.internal.entity.MenuCategory;
import id.my.rascal.menu.internal.repository.MenuCategoryRepository;

@Component
@Order(40)
public class DevMenuCategorySeeder implements Seeder {

    private final MenuCategoryRepository categoryRepository;

    public DevMenuCategorySeeder(MenuCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public SeedType seedType() {
        return SeedType.DEV;
    }

    @Override
    @Transactional
    public void seed() {
        LocalDateTime now = LocalDateTime.now();
        for (MenuSeedCatalog.CategorySeed seed : MenuSeedCatalog.CATEGORIES) {
            if (categoryRepository.existsByCategoryCode(seed.categoryCode())) continue;
            MenuCategory category = new MenuCategory();
            category.setCategoryCode(seed.categoryCode());
            category.setDisplayName(seed.displayName());
            category.setDisplayOrder(seed.displayOrder());
            category.setCreatedAt(now);
            categoryRepository.save(category);
        }
    }

}
